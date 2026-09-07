package com.martinsterentjevs.cacheit.ui.account.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.martinsterentjevs.cacheit.R
import com.martinsterentjevs.cacheit.data.auth.AuthFlowException
import com.martinsterentjevs.cacheit.services.crypto.CryptoService
import com.martinsterentjevs.cacheit.services.security.SecurityService
import com.martinsterentjevs.cacheit.ui.common.PopupController
import com.martinsterentjevs.cacheit.ui.common.UiEvent
import com.martinsterentjevs.cacheit.ui.theme.CacheItSpacing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface PasswordChangeUiState {
    data object Idle : PasswordChangeUiState
    data object Saving : PasswordChangeUiState
}

@HiltViewModel
class PasswordChangeViewModel @Inject constructor(
    private val cryptoService: CryptoService,
    private val securityService: SecurityService,
    private val popupController: PopupController,
    // TODO: AuthRepository needs a changePassword(...) method added once the server
    // endpoint exists (not yet built - see pre-mvp-checklist's cleanup-issue scope).
    // private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<PasswordChangeUiState>(PasswordChangeUiState.Idle)
    val uiState: StateFlow<PasswordChangeUiState> = _uiState.asStateFlow()

    fun submit(currentPassword: String, newPassword: String, confirmPassword: String) {
        if (currentPassword.isBlank() || newPassword.isBlank()) {
            popupController.show(UiEvent.Snackbar(R.string.password_change_fill_all_fields_notice))
            return
        }
        if (newPassword != confirmPassword) {
            popupController.show(UiEvent.Snackbar(R.string.password_change_new_password_mismatch))
            return
        }
        if (_uiState.value is PasswordChangeUiState.Saving) return

        viewModelScope.launch {
            _uiState.value = PasswordChangeUiState.Saving
            try {
                // ⚠️ NOT YET REAL - the sequence below is the best-guess shape based on the
                // locked MEK/MUK architecture, but has open questions that need answering
                // before this is safe to wire up for real:
                //
                // 1. Does the salt stay the same across a password change, or does the server
                //    issue a fresh kdfSalt? (Argon2 salts don't inherently need to rotate with
                //    the password, but this project hasn't explicitly decided either way.)
                // 2. Is `securityService.getSalt()` still populated at this point in the app's
                //    lifecycle (it's set during login/registration) - or does it need
                //    re-fetching via AuthRepository.fetchSalt() before deriving anything here?
                // 3. The MEK itself is already held in plaintext locally via
                //    `securityService.getSecureMek()` (Keystore-protected at rest) - meaning
                //    this does NOT need to unwrap an old envelope at all, just re-wrap the
                //    already-held plaintext MEK under a new MUK. Confirm this assumption before
                //    relying on it.
                // 4. Old-password verification: should happen server-side (server compares the
                //    submitted old authHash against its stored one before accepting the new
                //    authHash + rewrapped MEK), not purely client-side - avoids a client that
                //    thinks it verified correctly when it didn't.
                //
                // val oldAuthHash = cryptoService.hashPassword(currentPassword)
                // val newAuthHash = cryptoService.hashPassword(newPassword)
                // val newMuk = cryptoService.hashMek(newPassword)
                // val mek = securityService.getSecureMek() ?: error("No MEK cached locally")
                // val newWrappedMek = cryptoService.wrapMek(mek, newMuk)
                // authRepository.changePassword(oldAuthHash, newAuthHash, newWrappedMek)

                popupController.show(UiEvent.Snackbar(R.string.password_change_unavailable))
            } catch (e: AuthFlowException) {
                popupController.show(UiEvent.Snackbar(R.string.snackbar_error,listOf(e.userMessage)))
            } finally {
                _uiState.value = PasswordChangeUiState.Idle
            }
        }
    }
}

@Composable
fun PasswordChange(viewModel: PasswordChangeViewModel = hiltViewModel()) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val enabled = uiState !is PasswordChangeUiState.Saving

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = currentPassword,
            onValueChange = { currentPassword = it },
            label = { Text(stringResource(R.string.password_change_current_password)) },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            label = { Text(stringResource(R.string.password_change_new_password)) },
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = CacheItSpacing.sm),
        )
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text(stringResource(R.string.password_change_confirm_password)) },
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = CacheItSpacing.sm),
        )
        Text(
            "Changing your password will sign out your other devices.",
            modifier = Modifier.padding(top = CacheItSpacing.xs),
        )
        Button(
            onClick = { viewModel.submit(currentPassword, newPassword, confirmPassword) },
            enabled = enabled,
            modifier = Modifier.padding(top = CacheItSpacing.sm),
        ) {
            Text(if (uiState is PasswordChangeUiState.Saving) stringResource(R.string.saving_in_progress) else stringResource(
                R.string.change_password
            ))
        }
    }
}
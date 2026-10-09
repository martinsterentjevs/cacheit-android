package com.martinsterentjevs.cacheit.ui.account.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.martinsterentjevs.cacheit.R
import com.martinsterentjevs.cacheit.data.account.AccountRepository
import com.martinsterentjevs.cacheit.ui.theme.CacheItSpacing
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AccountSummaryUiState{
    data object Loading: AccountSummaryUiState
    data class Error(
        val messageId:Int
    ) : AccountSummaryUiState
    data class Content(
        val accountHolder:String,
        val username:String,
        val email:String
    ): AccountSummaryUiState
}

@HiltViewModel
class AccountSummaryViewModel @Inject constructor(
    private val accountRepository: AccountRepository // your Android repo, not the server one
) : ViewModel() {
    private val _uiState = MutableStateFlow<AccountSummaryUiState>(AccountSummaryUiState.Loading)
    val uiState: StateFlow<AccountSummaryUiState> = _uiState.asStateFlow()
    private var isLoadInFlight = false

    init { load() }

    fun load() {
        if (isLoadInFlight) return
        isLoadInFlight = true
        viewModelScope.launch {
            try {
                val profile = accountRepository.getAccount()
                _uiState.value = AccountSummaryUiState.Content(
                    profile.accountHolder, profile.username ?: "", profile.email ?: ""
                )
            } catch (e: Exception) {
                _uiState.value = AccountSummaryUiState.Error(R.string.data_fetch_failed)
            } finally {
                isLoadInFlight = false
            }
        }
    }
}

@Composable
fun AccountSummary(
    viewModel: AccountSummaryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = CacheItSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(CacheItSpacing.xs)
    ) {
        when (val state = uiState) {
            is AccountSummaryUiState.Loading -> CircularProgressIndicator()
            is AccountSummaryUiState.Error -> Text(stringResource(R.string.account_loading_error))
            is AccountSummaryUiState.Content -> {
                SummaryRow(stringResource(R.string.registration_accountholder), state.accountHolder)
                SummaryRow(stringResource(R.string.registration_email), state.email)
                SummaryRow(stringResource(R.string.registration_username), state.username)
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value, style = MaterialTheme.typography.bodyLarge)
}
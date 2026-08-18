package com.martinsterentjevs.cacheit.ui.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.martinsterentjevs.cacheit.R
import com.martinsterentjevs.cacheit.ui.theme.CacheItSpacing
import com.martinsterentjevs.cacheit.ui.theme.TypeCaption
import com.martinsterentjevs.cacheit.ui.theme.TypeLabel
import com.martinsterentjevs.cacheit.ui.theme.TypeTitle

@Composable
fun RegistrationScreen(
    viewModel: RegistrationViewModel = hiltViewModel(),
    onRegistrationSuccess: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
) {
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                AuthEvent.Success -> onRegistrationSuccess()
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(CacheItSpacing.lg),
    ) {
        Text(stringResource(R.string.registration_title), style = TypeTitle, textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = CacheItSpacing.md).fillMaxWidth().align(Alignment.CenterHorizontally),
            color = MaterialTheme.colorScheme.onBackground)
        Text(stringResource(R.string.registration_subtitle), style = TypeLabel, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onBackground)

        Spacer(modifier = Modifier.height(CacheItSpacing.xl))

        val fieldsEnabled = uiState !is AuthUiState.Loading

        OutlinedTextField(name, { name = it }, enabled = fieldsEnabled,
            label = { Text(stringResource(R.string.registration_accountholder)) },
            modifier = Modifier.fillMaxWidth().padding(top = CacheItSpacing.lg))

        OutlinedTextField(email, { email = it }, enabled = fieldsEnabled,
            label = { Text(stringResource(R.string.registration_email)) },
            modifier = Modifier.fillMaxWidth().padding(top = CacheItSpacing.sm))

        OutlinedTextField(username, { username = it }, enabled = fieldsEnabled,
            label = { Text(stringResource(R.string.registration_username)) },
            modifier = Modifier.fillMaxWidth().padding(top = CacheItSpacing.sm))

        OutlinedTextField(password, { password = it }, enabled = fieldsEnabled,
            label = { Text(stringResource(R.string.registration_password)) },
            modifier = Modifier.fillMaxWidth().padding(top = CacheItSpacing.sm))

        OutlinedTextField(confirmPassword, { confirmPassword = it }, enabled = fieldsEnabled,
            label = { Text("Confirm password") },
            modifier = Modifier.fillMaxWidth().padding(top = CacheItSpacing.sm))

        Text(stringResource(R.string.registration_password_note), style = TypeCaption,
            color = MaterialTheme.colorScheme.onPrimary)

        Button(
            onClick = { viewModel.submit(name, username, email, password, confirmPassword) },
            enabled = fieldsEnabled,
            modifier = Modifier.fillMaxWidth().padding(top = CacheItSpacing.xl),
        ) {
            Text(stringResource(R.string.registration_request))
        }

        TextButton(onClick = onNavigateToLogin) {
            Text(stringResource(R.string.registration_account_exists))
        }
    }
}
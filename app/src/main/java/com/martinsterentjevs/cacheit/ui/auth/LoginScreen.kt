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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.martinsterentjevs.cacheit.R
import com.martinsterentjevs.cacheit.ui.common.components.PasswordField
import com.martinsterentjevs.cacheit.ui.theme.CacheItSpacing
import com.martinsterentjevs.cacheit.ui.theme.TypeBody
import com.martinsterentjevs.cacheit.ui.theme.TypeTitle

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit = {},
    onNavigateToRegister: () -> Unit = {},
) {
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                AuthEvent.Success -> onLoginSuccess()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(CacheItSpacing.lg),
    ) {
        Text(stringResource(R.string.login_title), style = TypeTitle,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = CacheItSpacing.md),
            color = MaterialTheme.colorScheme.onBackground)

        Text(stringResource(R.string.login_subtitle), style = TypeBody,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = CacheItSpacing.xl),
            color = MaterialTheme.colorScheme.onBackground)

        Spacer(modifier = Modifier.height(CacheItSpacing.xl * 5))

        OutlinedTextField(
            value = identifier,
            onValueChange = { identifier = it },
            label = { Text(stringResource(R.string.login_identifier)) },
            enabled = uiState !is AuthUiState.Loading,
            modifier = Modifier.fillMaxWidth().padding(top = CacheItSpacing.xl),
            singleLine = true,
        )


        PasswordField(
            value = password,
            onValueChange = { password = it },
            label = stringResource(R.string.login_password),
            enabled = uiState !is AuthUiState.Loading,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { viewModel.submit(identifier, password) },
            enabled = uiState !is AuthUiState.Loading,
            modifier = Modifier.fillMaxWidth().padding(top = CacheItSpacing.xl),
        ) {
            Text(stringResource(R.string.login_request), color = MaterialTheme.colorScheme.onBackground)
        }

        TextButton(onClick = onNavigateToRegister) {
            Text(stringResource(R.string.login_no_account), color = MaterialTheme.colorScheme.primary)
        }
    }
}
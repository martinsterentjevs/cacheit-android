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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.martinsterentjevs.cacheit.R
import com.martinsterentjevs.cacheit.ui.theme.CacheItSpacing
import com.martinsterentjevs.cacheit.ui.theme.CacheItTheme
import com.martinsterentjevs.cacheit.ui.theme.TypeBody
import com.martinsterentjevs.cacheit.ui.theme.TypeTitle

/**
 * Login screen — plain composable, not an Activity. Reached from Welcome or
 * from Registration ("Already have an account").
 *
 * Contents (not yet implemented):
 * - Email/username + password fields wired to the auth API
 * - Validation + inline error display
 * - "Forgot password" entry point (deferred — no server-side flow yet)
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit = {},
    onNavigateToRegister: () -> Unit = {},
) {
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(CacheItSpacing.lg),

    ) {
        Text((stringResource(R.string.login_title)), style = TypeTitle,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = CacheItSpacing.md),
            color = MaterialTheme.colorScheme.onBackground)

        Text((stringResource(R.string.login_subtitle)), style = TypeBody,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = CacheItSpacing.xl)
                        ,color = MaterialTheme.colorScheme.onBackground)

        Spacer(modifier = Modifier.height(CacheItSpacing.xl*5))

        OutlinedTextField(
            value = identifier,
            onValueChange = { identifier = it },
            label = { Text((stringResource(R.string.login_identifier))) },

            modifier = Modifier
                .fillMaxWidth()
                .padding(top = CacheItSpacing.xl),
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text((stringResource(R.string.login_password))) },

            modifier = Modifier
                .fillMaxWidth()
                .padding(top = CacheItSpacing.sm),
        )

        // TODO: wire to auth API, call onLoginSuccess() on success
        Button(
            onClick = onLoginSuccess,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = CacheItSpacing.xl),
        ) {
            Text((stringResource(R.string.login_request)),
                    color = MaterialTheme.colorScheme.onBackground)
        }

        TextButton(onClick = onNavigateToRegister) {
            Text((stringResource(R.string.login_no_account)),
                color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    CacheItTheme {
        LoginScreen()
    }
}
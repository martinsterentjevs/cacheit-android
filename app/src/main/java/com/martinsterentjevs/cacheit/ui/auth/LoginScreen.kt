package com.martinsterentjevs.cacheit.ui.auth

import android.R
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.martinsterentjevs.cacheit.ui.theme.CacheItSpacing
import com.martinsterentjevs.cacheit.ui.theme.CacheItTheme
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
        Text("Log in", style = TypeTitle)

        OutlinedTextField(
            value = identifier,
            onValueChange = { identifier = it },
            label = { Text("Email or username") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = CacheItSpacing.xl),
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
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
            Text("Log in")
        }

        TextButton(onClick = onNavigateToRegister) {
            Text("Need an account? Register")
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
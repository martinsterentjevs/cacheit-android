package com.martinsterentjevs.cacheit.ui.auth

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
 * Registration screen — plain composable. On success, the client generates and
 * wraps the MEK before anything is sent to the server (crypto core, not yet
 * implemented — this screen only owns the form and the success/failure
 * callback contract).
 *
 * Contents (not yet implemented):
 * - Email + password + confirm-password fields
 * - Client-side MEK generation + wrap, called before the register API request
 * - Validation + inline error display, including a "registration closed" case
 *   for single-user-mode servers that already have an account
 */
@Composable
fun RegistrationScreen(
    onRegistrationSuccess: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
) {
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(CacheItSpacing.lg),
    ) {
        Text("Create account", style = TypeTitle)

        OutlinedTextField(
            value = identifier,
            onValueChange = { identifier = it },
            label = { Text("Email") },
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

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm password") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = CacheItSpacing.sm),
        )

        // TODO: generate + wrap MEK client-side, call register API, then onRegistrationSuccess()
        Button(
            onClick = onRegistrationSuccess,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = CacheItSpacing.xl),
        ) {
            Text("Create account")
        }

        TextButton(onClick = onNavigateToLogin) {
            Text("Already have an account? Log in")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RegistrationScreenPreview() {
    CacheItTheme {
        RegistrationScreen()
    }
}
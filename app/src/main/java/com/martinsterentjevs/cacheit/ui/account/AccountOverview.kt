package com.martinsterentjevs.cacheit.ui.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.martinsterentjevs.cacheit.ui.theme.CacheItSpacing
import com.martinsterentjevs.cacheit.ui.theme.CacheItTheme
import com.martinsterentjevs.cacheit.ui.theme.TypeHeading

/**
 * Account overview — reached from Notes List. Not gated behind its own auth
 * step; the existing session already covers it.
 *
 * Password change and device sessions live here as sections, not as separate
 * nav destinations — neither needs its own back-stack entry.
 *
 * Contents (not yet implemented):
 * - Account email/username display
 * - Password change form (triggers MEK rewrap + invalidates other device sessions)
 * - Active device sessions list, each individually revocable
 */
@OptIn(ExperimentalMaterial3Api::class) //Temporary workaround
@Composable
fun AccountOverviewScreen(
    onBack: () -> Unit = {},
    onLoggedOut: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(innerPadding)
                .padding(CacheItSpacing.lg),
        ) {
            // TODO: account summary (email/username)

            Text("Password change", style = TypeHeading)
            // TODO: password change form

            Text(
                "Device sessions",
                style = TypeHeading,
                modifier = Modifier.padding(top = CacheItSpacing.xl),
            )
            // TODO: device sessions list, each row individually revocable

            Button(
                onClick = onLoggedOut,
                modifier = Modifier.padding(top = CacheItSpacing.xl),
            ) {
                Text("Log out")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AccountOverviewScreenPreview() {
    CacheItTheme {
        AccountOverviewScreen()
    }
}
package com.martinsterentjevs.cacheit.ui.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.martinsterentjevs.cacheit.R
import com.martinsterentjevs.cacheit.ui.account.components.AccountSummary
import com.martinsterentjevs.cacheit.ui.account.components.ClearOut
import com.martinsterentjevs.cacheit.ui.account.components.PasswordChange
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
    viewModel: AccountOverviewViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onPasswordChange: () -> Unit = {},
    onDeviceSessions: () -> Unit = {},
    onLoggedOut: () -> Unit = {},
) {
    LaunchedEffect(Unit) { viewModel.events.collect {
        if (it == AccountOverviewEvent.LoggedOut) onLoggedOut()
    } }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.account_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            AccountSummary()
            Text(stringResource(R.string.account_password_change), style = TypeHeading)

            PasswordChange()

            Text(
                stringResource(R.string.account_device_sessions),
                style = TypeHeading,
                modifier = Modifier.padding(top = CacheItSpacing.xl),
            )
            // TODO: device sessions list, each row individually revocable

            Button(
                onClick = { viewModel.onLogoutTapped() },
                modifier = Modifier.padding(top = CacheItSpacing.xl),
            ) {
                Text(stringResource(R.string.account_log_out))
            }
            Text(stringResource(R.string.clearout_title))
            ClearOut()
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
package com.martinsterentjevs.cacheit.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.martinsterentjevs.cacheit.R
import androidx.compose.ui.R.string
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.martinsterentjevs.cacheit.ui.theme.CacheItSpacing
import com.martinsterentjevs.cacheit.ui.theme.CacheItTheme
import com.martinsterentjevs.cacheit.ui.theme.Neutral200
import com.martinsterentjevs.cacheit.ui.theme.TypeTitle

/**
 * First screen shown on a fresh install. No mode-selection step here — single vs
 * multi-user mode is a server deployment decision made at server first-boot, not
 * something the client ever presents.
 */
@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit = {},
    onLogin: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = CacheItSpacing.md, vertical = CacheItSpacing.xl),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            stringResource(R.string.welcome_greeting),
            style = TypeTitle,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = CacheItSpacing.md),
        )

        Button(
            onClick = onGetStarted,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = CacheItSpacing.xl, bottom = CacheItSpacing.md),
        ) {
            Text(stringResource(R.string.welcome_get_started))
        }

        OutlinedButton(
            onClick = onLogin,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = CacheItSpacing.sm, bottom = CacheItSpacing.xl),
        ) {
            Text(stringResource(R.string.welcome_account_exists))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomeScreenPreview() {
    CacheItTheme {
        WelcomeScreen()
    }
}
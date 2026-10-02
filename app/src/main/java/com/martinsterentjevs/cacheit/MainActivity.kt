package com.martinsterentjevs.cacheit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.martinsterentjevs.cacheit.data.preferences.ThemePreference
import com.martinsterentjevs.cacheit.data.preferences.ThemePreferenceRepository
import com.martinsterentjevs.cacheit.ui.theme.CacheItTheme
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var themeRepo: ThemePreferenceRepository
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themePreference by themeRepo.themePreference
                .collectAsStateWithLifecycle(
                    initialValue = ThemePreference.SYSTEM
                )

            val darkTheme = when (themePreference) {
                ThemePreference.SYSTEM -> isSystemInDarkTheme()
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
            }
            CacheItTheme(
                darkTheme = darkTheme
            ) {
                CacheItApp()
            }
        }
    }
}
package com.martinsterentjevs.cacheit.ui.account.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.martinsterentjevs.cacheit.R
import com.martinsterentjevs.cacheit.data.preferences.ThemePreference
import com.martinsterentjevs.cacheit.data.preferences.ThemePreferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.launch


@HiltViewModel
class ThemeToggleViewModel @Inject constructor(
    private val themeRepo: ThemePreferenceRepository,
) : ViewModel() {

    val themePreference = themeRepo.themePreference

    fun setUseSystemTheme(enabled: Boolean) {
        viewModelScope.launch {
            themeRepo.setThemePreference(
                if (enabled) {
                    ThemePreference.SYSTEM
                } else {
                    ThemePreference.DARK
                }
            )
        }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            themeRepo.setThemePreference(
                if (enabled) {
                    ThemePreference.DARK
                } else {
                    ThemePreference.LIGHT
                }
            )
        }
    }
}
@Composable
fun ThemeToggle(
    viewModel: ThemeToggleViewModel = hiltViewModel(),
) {
    val preference by viewModel.themePreference.collectAsStateWithLifecycle(
        initialValue = ThemePreference.SYSTEM,
        lifecycle = LocalLifecycleOwner.current.lifecycle
    )
    val useSystemTheme = preference == ThemePreference.SYSTEM
    val darkTheme = preference == ThemePreference.DARK

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.account_use_system_theme))

            Checkbox(
                checked = useSystemTheme,
                onCheckedChange = viewModel::setUseSystemTheme
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.account_toggle_theme))

            Switch(
                checked = darkTheme,
                onCheckedChange = viewModel::setDarkTheme,
                enabled = !useSystemTheme
            )
        }
    }
}
package com.martinsterentjevs.cacheit.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK
}

private val Context.themeDataStore by preferencesDataStore(
    name = "theme_preferences"
)

@Singleton
class ThemePreferenceRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private companion object {
        val THEME_PREFERENCE = stringPreferencesKey("theme_preference")
    }

    val themePreference: Flow<ThemePreference> =
        context.themeDataStore.data.map { preferences ->
            preferences[THEME_PREFERENCE]
                ?.let { value ->
                    runCatching { ThemePreference.valueOf(value) }.getOrNull()
                }
                ?: ThemePreference.SYSTEM
        }

    suspend fun setThemePreference(preference: ThemePreference) {
        context.themeDataStore.edit { preferences ->
            preferences[THEME_PREFERENCE] = preference.name
        }
    }
}
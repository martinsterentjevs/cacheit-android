package com.martinsterentjevs.cacheit.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.autosaveDataStore by preferencesDataStore(
    name = "autosave_preferences"
)

@Singleton
class AutosavePreference @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY = longPreferencesKey("autosave_inactivity_ms")
        const val DEFAULT_MS = 30_000L
    }

    private val dataStore = context.autosaveDataStore

    suspend fun getDurationMs(): Long = dataStore.data.map { it[KEY] ?: DEFAULT_MS }.first()

    suspend fun setDurationMs(ms: Long) {
        dataStore.edit { it[KEY] = ms }
    }
}

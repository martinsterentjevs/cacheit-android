package com.martinsterentjevs.cacheit.services.security

import android.content.Context
import android.content.SharedPreferences
import java.util.Base64
import androidx.core.content.edit

/**
 * Plain (non-Keystore) local key-value storage for values that don't need
 * confidentiality on their own — account salt, already-Keystore-wrapped
 * envelopes (the wrapping is what protects them, not this store), format
 * version markers, etc. Adding a new persisted value means adding a
 * [LocalCacheKey] entry, not touching this class.
 *
 * A cache, not a source of truth — anything stored here must be
 * re-fetchable from its real origin (server, or re-derivation) if this
 * store is empty, e.g. after a fresh install or app data clear.
 *
 * Backed by SharedPreferences for MVP simplicity; the interface is the
 * seam if this ever needs to move to DataStore later without touching
 * call sites.
 */
internal interface LocalStore {
    fun putBytes(key: LocalCacheKey, value: ByteArray)
    fun getBytes(key: LocalCacheKey): ByteArray?
    fun putString(key: LocalCacheKey, value: String)
    fun getString(key: LocalCacheKey): String?
    fun remove(key: LocalCacheKey)
}

internal class SharedPreferencesLocalStore(context: Context) : LocalStore {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("cacheit_local_store", Context.MODE_PRIVATE)

    override fun putBytes(key: LocalCacheKey, value: ByteArray) {
        prefs.edit { putString(key.key, Base64.getEncoder().encodeToString(value)) }
    }

    override fun getBytes(key: LocalCacheKey): ByteArray? =
        prefs.getString(key.key, null)?.let { Base64.getDecoder().decode(it) }

    override fun putString(key: LocalCacheKey, value: String) {
        prefs.edit { putString(key.key, value) }
    }

    override fun getString(key: LocalCacheKey): String? = prefs.getString(key.key, null)

    override fun remove(key: LocalCacheKey) {
        prefs.edit { remove(key.key) }
    }
}
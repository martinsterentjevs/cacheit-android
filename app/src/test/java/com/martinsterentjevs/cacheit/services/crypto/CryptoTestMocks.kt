package com.martinsterentjevs.cacheit.services.crypto

import com.martinsterentjevs.cacheit.services.security.CacheAlias
import com.martinsterentjevs.cacheit.services.security.KeyStoreService
import com.martinsterentjevs.cacheit.services.security.LocalCacheKey
import com.martinsterentjevs.cacheit.services.security.LocalStore

/**
 * Identity wrap/unwrap — sufficient here since these tests exercise AESWrapper
 * and ArgonWrapper's own logic, not KeyStoreService's. Real Keystore behavior
 * (hardware-backed, invalidation, etc.) has its own instrumented test suite,
 * separate from this plain-JVM one.
 */
internal class FakeKeyStoreService : KeyStoreService {
    override fun wrap(alias: CacheAlias, plaintext: ByteArray): ByteArray = plaintext
    override fun unwrap(alias: CacheAlias, envelope: ByteArray): ByteArray = envelope
    override fun invalidate(alias: CacheAlias) {}
}

internal class FakeLocalStore : LocalStore {
    private val stringValues = mutableMapOf<LocalCacheKey, String>()
    private val byteValues = mutableMapOf<LocalCacheKey, ByteArray>()

    override fun putBytes(key: LocalCacheKey, value: ByteArray) {
        byteValues[key] = value
    }

    override fun getBytes(key: LocalCacheKey): ByteArray? = byteValues[key]

    override fun putString(key: LocalCacheKey, value: String) {
        stringValues[key] = value
    }

    override fun getString(key: LocalCacheKey): String? = stringValues[key]

    override fun remove(key: LocalCacheKey) {
        byteValues.remove(key)
        stringValues.remove(key)
    }
}
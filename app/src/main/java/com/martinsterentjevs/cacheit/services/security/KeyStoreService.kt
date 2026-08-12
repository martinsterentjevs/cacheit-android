package com.martinsterentjevs.cacheit.services.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Thrown when a Keystore-backed key can no longer be used (biometric
 * re-enrollment, lock screen removed, device state change, etc.). Callers
 * must treat this as "cache is dead" and fall back to re-deriving the
 * value from its real source of truth — never surface this as a crash.
 */
internal class CacheInvalidatedException(alias: CacheAlias, cause: Throwable) :
    Exception("Keystore entry for $alias was invalidated and has been deleted", cause)

/**
 * Generic wrap/unwrap over AndroidKeyStore-backed AES-256-GCM keys. This
 * interface knows nothing about MEKs, notes, or any specific secret —
 * callers supply a [CacheAlias] and get an opaque encrypted envelope back.
 * Adding a new kind of locally-cached secret means adding a [CacheAlias]
 * entry, not touching this class.
 *
 * Not a substitute for the MUK-wrapped MEK that persists server-side — this
 * is strictly a local, device-bound performance cache. If wrap/unwrap ever
 * fails for any reason, callers must fall back to re-deriving from the real
 * source of truth, never treat a cache miss as an error state.
 *
 * Every call reaches real Android Keystore hardware, so this class is not
 * unit-testable on a plain JVM — exercise it via instrumented tests only.
 */
internal interface KeyStoreService {
    /** Encrypts [plaintext] under the Keystore key for [alias], generating that key first if it doesn't exist. */
    fun wrap(alias: CacheAlias, plaintext: ByteArray): ByteArray

    /** Decrypts an envelope previously produced by [wrap] for the same [alias]. */
    fun unwrap(alias: CacheAlias, envelope: ByteArray): ByteArray

    /** Deletes the Keystore entry for [alias]. Call on logout and on password change. */
    fun invalidate(alias: CacheAlias)
}

internal class AndroidKeyStoreService : KeyStoreService {

    private companion object {
        const val PROVIDER = "AndroidKeyStore"
        const val TRANSFORM = "AES/GCM/NoPadding"
        const val NONCE_SIZE = 12
        const val TAG_SIZE = 128
    }

    private val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }

    private fun getOrCreateKey(alias: CacheAlias): SecretKey {
        (keyStore.getKey(alias.alias, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            alias.alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    override fun wrap(alias: CacheAlias, plaintext: ByteArray): ByteArray {
        val key = getOrCreateKey(alias)
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val nonce = cipher.iv // Keystore generates this internally on encrypt init — not our SecureRandom call
        val ciphertext = cipher.doFinal(plaintext)
        // No version byte here (unlike the cross-client note envelope) — this format
        // never leaves the device, so there's no second implementation to stay in sync with.
        return nonce + ciphertext
    }

    override fun unwrap(alias: CacheAlias, envelope: ByteArray): ByteArray {
        val key = try {
            keyStore.getKey(alias.alias, null) as? SecretKey
                ?: throw IllegalStateException("No Keystore entry for $alias")
        } catch (e: KeyPermanentlyInvalidatedException) {
            invalidate(alias)
            throw CacheInvalidatedException(alias, e)
        }

        val nonce = envelope.copyOfRange(0, NONCE_SIZE)
        val ciphertext = envelope.copyOfRange(NONCE_SIZE, envelope.size)
        val cipher = Cipher.getInstance(TRANSFORM)

        return try {
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_SIZE, nonce))
            cipher.doFinal(ciphertext)
        } catch (e: KeyPermanentlyInvalidatedException) {
            invalidate(alias)
            throw CacheInvalidatedException(alias, e)
        }
    }

    override fun invalidate(alias: CacheAlias) {
        if (keyStore.containsAlias(alias.alias)) {
            keyStore.deleteEntry(alias.alias)
        }
    }
}
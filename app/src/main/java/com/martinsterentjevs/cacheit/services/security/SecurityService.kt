package com.martinsterentjevs.cacheit.services.security

import android.content.Context
import com.martinsterentjevs.cacheit.services.security.SharedPreferencesLocalStore

/**
 * Single public entry point for local security-adjacent storage. Everything
 * underneath — [KeyStoreService], [LocalStore], the alias/key enums — is
 * internal; callers never choose between Keystore and plain storage
 * themselves, they call the method that already encodes that choice.
 *
 * Naming convention for expansion: a plain value gets `getX`/`setX`
 * (backed by [LocalStore] alone); a value needing on-device confidentiality
 * gets `getSecureX`/`setSecureX` (backed by [LocalStore] + [KeyStoreService]
 * together — see [getSecureMek] for the composition pattern to copy).
 *
 * `getSecureX` calls only ever reach real Keystore hardware through
 * [AndroidKeyStoreService], so any test exercising them needs the
 * instrumented suite, not a plain JVM unit test. `getX`/`setX` calls only
 * touch [LocalStore], which is fine to fake in a plain JVM test.
 */
class SecurityService internal constructor(
    context: Context,
    private val keyStoreService: KeyStoreService,
    private val localStore: LocalStore
) {
    constructor(context: Context):this(
        context = context,
        keyStoreService = AndroidKeyStoreService(),
        localStore = SharedPreferencesLocalStore(context)
    )


    // ---- Plain (local-only) values ----

    /** Account salt — not secret, just needs to be consistent across app launches. Null on cache miss (fetch from the server's salt-lookup endpoint). */
    fun getSalt(): ByteArray? = localStore.getBytes(LocalCacheKey.ACCOUNT_SALT)

    fun setSalt(salt: ByteArray) = localStore.putBytes(LocalCacheKey.ACCOUNT_SALT, salt)

    fun clearSalt() = localStore.remove(LocalCacheKey.ACCOUNT_SALT)

    // ---- Secure (Keystore-backed) values ----

    /**
     * Cached, already-unwrapped MEK for this session. Null on cache miss or
     * on invalidation — callers must treat null the same way regardless of
     * cause: fall back to re-deriving the MEK from the MUK-wrapped copy via
     * password re-entry, never treat it as an error state.
     */
    fun getSecureMek(): ByteArray? {
        val envelope = localStore.getBytes(LocalCacheKey.MEK_SESSION_ENVELOPE) ?: return null
        return try {
            keyStoreService.unwrap(CacheAlias.MEK_SESSION_CACHE, envelope)
        } catch (e: CacheInvalidatedException) {
            localStore.remove(LocalCacheKey.MEK_SESSION_ENVELOPE)
            null
        }
    }

    fun setSecureMek(mek: ByteArray) {
        val envelope = keyStoreService.wrap(CacheAlias.MEK_SESSION_CACHE, mek)
        localStore.putBytes(LocalCacheKey.MEK_SESSION_ENVELOPE, envelope)
    }

    /** Call on logout and on password change — the cached MEK is stale the moment either happens. */
    fun clearSecureMek() {
        localStore.remove(LocalCacheKey.MEK_SESSION_ENVELOPE)
        keyStoreService.invalidate(CacheAlias.MEK_SESSION_CACHE)
    }
}
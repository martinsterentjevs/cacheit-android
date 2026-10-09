package com.martinsterentjevs.cacheit.services.security

/**
 * Namespace of Android Keystore key aliases. Adding a new locally-cached
 * secret means adding one entry here — KeyStoreService itself never needs
 * to change.
 */
internal enum class CacheAlias(val alias: String) {
    MEK_SESSION_CACHE("cacheit_mek_session_cache"),
}
package com.martinsterentjevs.cacheit.services.security

/**
 * Namespace of LocalStore keys. Adding a new locally-persisted, non-secret
 * value means adding one entry here — LocalStore itself never needs to
 * change.
 */
internal enum class LocalCacheKey(val key: String) {
    ACCOUNT_SALT("cacheit_account_salt"),
    MEK_SESSION_ENVELOPE("cacheit_mek_session_envelope"),
}
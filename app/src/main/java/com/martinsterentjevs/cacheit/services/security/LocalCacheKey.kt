package com.martinsterentjevs.cacheit.services.security

/**
 * Namespace of LocalStore keys. Adding a new locally-persisted, non-secret
 * value means adding one entry here - LocalStore itself never needs to
 * change.
 */
internal enum class LocalCacheKey(val key: String) {
    ACCOUNT_SALT("cacheit_account_salt"),
    MEK_SESSION_ENVELOPE("cacheit_mek_session_envelope"),
    ACCOUNT_ID("cacheit_account_id"),
    ACCESS_TOKEN("cacheit_access_token"),
    REFRESH_TOKEN("cacheit_refresh_token"),
    DEVICE_ID("cacheit_device_id"),
}
package com.martinsterentjevs.cacheit.network.auth


import kotlinx.serialization.Serializable

/**
 * All byte-array-shaped fields here are Base64-encoded strings on the wire.
 * These shapes must match the server's actual JSON contract exactly - verify
 * field names against the real server DTOs before wiring this up live;
 * these are drafted from the flow we designed, not copied from server source.
 *
 * Registration carries name/username/email as distinct fields, matching what
 * the server stores separately. Login (and the salt lookup that precedes it)
 * consolidates to a single [identifier] - whatever the user actually typed,
 * matched server-side against either the username or email column.
 */

@Serializable
data class SaltLookupRequest(val identifier: String)

@Serializable
data class SaltLookupResponse(val salt: String) // Base64 - real or deterministic fake, indistinguishable

@Serializable
data class RegisterRequest(
    val name: String,
    val username: String,
    val email: String,
    val salt: String,       // Base64 - client-generated at registration
    val authHash: String,   // Base64 - Argon2id output, IDENTIFIER_HASH_CONTEXT
    val wrappedMek: String, // Base64 envelope - MEK wrapped by MUK, server stores opaquely
)

@Serializable
data class LoginRequest(
    val identifier: String, // username or email, whichever the user typed
    val authHash: String,   // Base64 - computed client-side after the salt lookup, not the raw password
)

@Serializable
data class AccountSessionResponse(
    val accountId: String,
    val accessToken: String,
    val refreshToken: String,
    val wrappedMek: String,  // Base64 envelope - client unwraps locally with its own derived MUK
)
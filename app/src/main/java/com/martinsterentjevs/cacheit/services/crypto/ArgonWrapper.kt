package com.martinsterentjevs.cacheit.services.crypto

import com.martinsterentjevs.cacheit.services.security.SecurityService
import jakarta.inject.Inject
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters

internal class ArgonWrapper @Inject constructor(val securityService: SecurityService) {

    private companion object {
        val MEK_HASH_CONTEXT = "CacheIt_MEK_HASH".toByteArray(Charsets.UTF_8)
        val IDENTIFIER_HASH_CONTEXT = "CacheIt_Password_HASH".toByteArray(Charsets.UTF_8)
        const val MEMORY_KIB = 65536 // 64 MiB
        const val ITERATIONS = 3
        const val PARALLELISM = 1
        const val SALT_LENGTH = 16
        const val OUTPUT_LENGTH = 32
    }

    private fun getCachedSalt(): ByteArray {
        val salt = securityService.getSalt()
            ?: throw IllegalStateException(
                "No account salt cached - fetch it from the salt-lookup endpoint and call setSalt() first",
            )
        require(salt.size >= SALT_LENGTH) { "Salt must be at least $SALT_LENGTH bytes" }
        return salt
    }

    /** Shared Argon2id derivation. [context] is what makes hashPassword and hashMekKey diverge for the same input. */
    private fun derive(input: String, context: ByteArray): ByteArray {
        val salt = getCachedSalt()
        val inputBytes = input.toByteArray(Charsets.UTF_8)

        val params = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withMemoryAsKB(MEMORY_KIB)
            .withIterations(ITERATIONS)
            .withSalt(salt)
            .withParallelism(PARALLELISM)
            .withAdditional(context)
            .build()

        val argon = Argon2BytesGenerator()
        argon.init(params)
        return ByteArray(OUTPUT_LENGTH).also { output -> argon.generateBytes(inputBytes, output) }
    }

    /** Server-facing auth hash — this is the only value the server ever sees. */
    fun hashPassword(input: String): ByteArray = derive(input, IDENTIFIER_HASH_CONTEXT)

    /** Local MUK derivation — used to wrap/unwrap the MEK, never sent to the server. */
    fun hashMekKey(input: String): ByteArray = derive(input, MEK_HASH_CONTEXT)
}
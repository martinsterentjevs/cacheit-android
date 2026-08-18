package com.martinsterentjevs.cacheit.services.crypto

import com.martinsterentjevs.cacheit.services.security.SecurityService
import java.security.SecureRandom
internal class MaterialGenerator(private val securityService: SecurityService) {

    private companion object {
        const val MEK_SIZE = 32
        const val SALT_SIZE = 16
    }

    /** Fresh MEK for a new account. Caller wraps it under the MUK before it ever leaves this class. */
    fun generateMek(): ByteArray = ByteArray(MEK_SIZE).also { SecureRandom().nextBytes(it) }

    /** Fresh account salt for registration. Caches it immediately — every derive() call after this reads it back from here. */
    fun generateSalt(): ByteArray =
        ByteArray(SALT_SIZE).also {
            SecureRandom().nextBytes(it)
            securityService.setSalt(it)
        }
}
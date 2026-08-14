package com.martinsterentjevs.cacheit.services.crypto

import com.martinsterentjevs.cacheit.services.security.SecurityService
import java.security.SecureRandom

class MaterialGenerator (private val securityService: SecurityService) {
    fun generateMek(): ByteArray {
        val mekSize = 32
        val mekArray = ByteArray(mekSize)
        SecureRandom().nextBytes(mekArray)
        securityService.setSalt(mekArray)
        return mekArray
    }
}
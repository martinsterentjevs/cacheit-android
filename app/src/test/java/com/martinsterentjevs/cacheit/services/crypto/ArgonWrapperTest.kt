package com.martinsterentjevs.cacheit.services.crypto

import android.content.Context
import com.martinsterentjevs.cacheit.services.security.SecurityService
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class ArgonWrapperTest {

    private lateinit var securityService: SecurityService
    private lateinit var argonWrapper: ArgonWrapper

    @Before
    fun setUp() {
        securityService = SecurityService(
            context = mock(Context::class.java),
            keyStoreService = FakeKeyStoreService(),
            localStore = FakeLocalStore(),
        )
        securityService.setSalt(ByteArray(16) { it.toByte() })
        argonWrapper = ArgonWrapper(securityService)
    }

    @Test
    fun `hashPassword and hashMekKey diverge for the same input`() {
        val password = "correct horse battery staple"
        val authHash = argonWrapper.hashPassword(password)
        val mukHash = argonWrapper.hashMekKey(password)
        assertFalse(authHash.contentEquals(mukHash))
    }

    @Test
    fun `hashPassword is deterministic for the same input and salt`() {
        val password = "correct horse battery staple"
        val first = argonWrapper.hashPassword(password)
        val second = argonWrapper.hashPassword(password)
        assertArrayEquals(first, second)
    }

    @Test
    fun `different passwords produce different auth hashes`() {
        val hashA = argonWrapper.hashPassword("password one")
        val hashB = argonWrapper.hashPassword("password two")
        assertFalse(hashA.contentEquals(hashB))
    }

    @Test
    fun `missing salt throws instead of silently failing`() {
        val noSaltService = SecurityService(
            context = mock(Context::class.java),
            keyStoreService = FakeKeyStoreService(),
            localStore = FakeLocalStore(),
        )
        val wrapper = ArgonWrapper(noSaltService)
        assertThrows(IllegalStateException::class.java) {
            wrapper.hashPassword("whatever")
        }
    }

    @Test
    fun `salt shorter than minimum length is rejected`() {
        securityService.setSalt(ByteArray(8)) // shorter than the 16-byte minimum
        assertThrows(IllegalArgumentException::class.java) {
            argonWrapper.hashPassword("whatever")
        }
    }
}
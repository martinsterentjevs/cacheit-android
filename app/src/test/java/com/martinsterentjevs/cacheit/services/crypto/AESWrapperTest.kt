package com.martinsterentjevs.cacheit.services.crypto

import android.content.Context
import com.martinsterentjevs.cacheit.services.security.SecurityService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import java.util.Base64
import java.util.UUID

class AESWrapperTest {

    private lateinit var securityService: SecurityService
    private lateinit var aesWrapper: AESWrapper
    private val noteId = UUID.randomUUID()

    @Before
    fun setUp() {
        securityService = SecurityService(
            context = mock(Context::class.java),
            keyStoreService = FakeKeyStoreService(),
            localStore = FakeLocalStore(),
        )
        securityService.setSecureMek(ByteArray(32) { it.toByte() }) // fixed 256-bit test MEK
        aesWrapper = AESWrapper(securityService)
    }

    @Test
    fun `round trip preserves typical note text`() {
        val plaintext = "Grocery list: eggs, milk, bread"
        val envelope = aesWrapper.encrypt(plaintext, noteId, NoteField.TITLE)
        assertEquals(plaintext, aesWrapper.decrypt(envelope, noteId, NoteField.TITLE))
    }

    @Test
    fun `round trip preserves empty plaintext`() {
        val envelope = aesWrapper.encrypt("", noteId, NoteField.BODY)
        assertEquals("", aesWrapper.decrypt(envelope, noteId, NoteField.BODY))
    }

    @Test
    fun `round trip preserves multi-kilobyte plaintext`() {
        val plaintext = "x".repeat(10_000)
        val envelope = aesWrapper.encrypt(plaintext, noteId, NoteField.BODY)
        assertEquals(plaintext, aesWrapper.decrypt(envelope, noteId, NoteField.BODY))
    }

    @Test
    fun `nonces are unique across many encryptions of the same plaintext`() {
        val nonces = (1..5_000).map {
            envelopeNonceBytes(aesWrapper.encrypt("same text", noteId, NoteField.TITLE)).toList()
        }
        assertEquals(nonces.size, nonces.toSet().size)
    }

    @Test
    fun `tampering with ciphertext breaks decryption`() {
        val envelope = aesWrapper.encrypt("sensitive content", noteId, NoteField.BODY)
        val tampered = flipLastByte(envelope)
        assertThrows(Exception::class.java) {
            aesWrapper.decrypt(tampered, noteId, NoteField.BODY)
        }
    }

    @Test
    fun `wrong noteId fails to decrypt due to AAD mismatch`() {
        val envelope = aesWrapper.encrypt("secret", noteId, NoteField.TITLE)
        val otherNoteId = UUID.randomUUID()
        assertThrows(Exception::class.java) {
            aesWrapper.decrypt(envelope, otherNoteId, NoteField.TITLE)
        }
    }

    @Test
    fun `cross-field swap on the same note fails to decrypt`() {
        val titleEnvelope = aesWrapper.encrypt("My Title", noteId, NoteField.TITLE)
        assertThrows(Exception::class.java) {
            aesWrapper.decrypt(titleEnvelope, noteId, NoteField.BODY)
        }
    }

    @Test
    fun `corrupted version byte is rejected`() {
        val envelope = aesWrapper.encrypt("text", noteId, NoteField.TITLE)
        val bytes = Base64.getDecoder().decode(envelope)
        bytes[0] = 99
        val corrupted = Base64.getEncoder().encodeToString(bytes)
        assertThrows(IllegalArgumentException::class.java) {
            aesWrapper.decrypt(corrupted, noteId, NoteField.TITLE)
        }
    }

    @Test
    fun `missing MEK throws instead of silently failing`() {
        val emptySecurityService = SecurityService(
            context = mock(Context::class.java),
            keyStoreService = FakeKeyStoreService(),
            localStore = FakeLocalStore(),
        )
        val wrapper = AESWrapper(emptySecurityService)
        assertThrows(IllegalStateException::class.java) {
            wrapper.encrypt("text", noteId, NoteField.TITLE)
        }
    }

    private fun envelopeNonceBytes(envelope: String): ByteArray {
        val bytes = Base64.getDecoder().decode(envelope)
        return bytes.copyOfRange(1, 13)
    }

    private fun flipLastByte(envelope: String): String {
        val bytes = Base64.getDecoder().decode(envelope)
        bytes[bytes.size - 1] = (bytes[bytes.size - 1].toInt() xor 0xFF).toByte()
        return Base64.getEncoder().encodeToString(bytes)
    }
}
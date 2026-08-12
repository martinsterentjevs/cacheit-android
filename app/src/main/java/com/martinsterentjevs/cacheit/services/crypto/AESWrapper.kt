package com.martinsterentjevs.cacheit.services.crypto

import com.martinsterentjevs.cacheit.services.security.SecurityService
import jakarta.inject.Inject
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

internal class AESWrapper @Inject constructor(private val securityService: SecurityService) {

    private companion object {
        const val TRANSFORM = "AES/GCM/NoPadding"
        const val NONCE_SIZE = 12 // 96 bits (standard for GCM)
        const val TAG_SIZE = 128 // bits (16 bytes)
        const val AAD_DELIMITER = ":"
        const val VERSION: Byte = 1
    }

    private fun getMekKey(): SecretKeySpec {
        val mek = securityService.getSecureMek()
            ?: throw IllegalStateException(
                "No MEK available - session is locked, re-derive from the MUK-wrapped copy first",
            )
        return SecretKeySpec(mek, "AES")
    }

    /**
     * Encrypts a plaintext string using AES-256-GCM with AAD.
     * @param plaintext The string to encrypt
     * @param noteId The UUID of the note the field belongs to
     * @param field The field being encrypted
     * @return Base64-encoded envelope: version byte + nonce + ciphertext||tag
     */
    fun encrypt(plaintext: String, noteId: UUID, field: NoteField): String {
        val byteInput = plaintext.toByteArray(Charsets.UTF_8)
        val nonce = getNonce()
        val spec = GCMParameterSpec(TAG_SIZE, nonce)

        val aes = Cipher.getInstance(TRANSFORM)
        val newAad = (noteId.toString() + AAD_DELIMITER + field.aadIdentifier).toByteArray(Charsets.UTF_8)
        aes.init(Cipher.ENCRYPT_MODE, getMekKey(), spec)
        aes.updateAAD(newAad)

        val result = aes.doFinal(byteInput)
        val envelope = byteArrayOf(VERSION) + nonce + result
        return Base64.getEncoder().encodeToString(envelope)
    }

    /**
     * Decrypts an envelope produced by [encrypt].
     * @param envelope Base64-encoded envelope, as stored in the TEXT column
     * @param noteId The UUID of the note the field belongs to - must match what was used to encrypt
     * @param field The field being decrypted - must match what was used to encrypt
     * @return The original plaintext string
     */
    fun decrypt(envelope: String, noteId: UUID, field: NoteField): String {
        val envelopeBytes = Base64.getDecoder().decode(envelope)
        val version = envelopeBytes[0]
        require(version == VERSION) { "Unsupported envelope version: $version" }

        val nonce = readNonce(envelopeBytes)
        val ciphertext = envelopeBytes.copyOfRange(1 + NONCE_SIZE, envelopeBytes.size)
        val spec = GCMParameterSpec(TAG_SIZE, nonce)

        val aes = Cipher.getInstance(TRANSFORM)
        aes.init(Cipher.DECRYPT_MODE, getMekKey(), spec)

        val newAad = (noteId.toString() + AAD_DELIMITER + field.aadIdentifier).toByteArray(Charsets.UTF_8)
        aes.updateAAD(newAad)

        val result = aes.doFinal(ciphertext)
        return String(result, Charsets.UTF_8)
    }

    private fun getNonce(): ByteArray {
        val nonce = ByteArray(NONCE_SIZE)
        SecureRandom().nextBytes(nonce)
        return nonce
    }

    private fun readNonce(byteInput: ByteArray): ByteArray {
        return byteInput.copyOfRange(1, 1 + NONCE_SIZE)
    }
}
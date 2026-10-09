package com.martinsterentjevs.cacheit.services.crypto

import com.martinsterentjevs.cacheit.services.security.SecurityService
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

internal class AESWrapper(private val securityService: SecurityService) {

    private companion object {
        const val TRANSFORM = "AES/GCM/NoPadding"
        const val NONCE_SIZE = 12 // 96 bits (standard for GCM)
        const val TAG_SIZE = 128 // bits (16 bytes)
        const val AAD_DELIMITER = ":"
        const val VERSION: Byte = 1

        // Fixed AAD context for MEK-wrap operations — distinct from any note's AAD,
        // since wrapping isn't tied to a noteId/field at all. Cross-client contract,
        // same as the NoteField identifiers — Desktop must use the identical constant.
        val MEK_WRAP_AAD = "cacheit.mek-wrap".toByteArray(Charsets.UTF_8)
    }

    private fun getMekKey(): SecretKeySpec {
        val mek = securityService.getSecureMek()
            ?: throw IllegalStateException(
                "No MEK available — session is locked, re-derive from the MUK-wrapped copy first",
            )
        return SecretKeySpec(mek, "AES")
    }

    /**
     * Encrypts a plaintext string using AES-256-GCM with AAD bound to the note field.
     * @return Base64-encoded envelope: version byte + nonce + ciphertext||tag
     */
    fun encrypt(plaintext: String, noteId: UUID, field: NoteField): String {
        val aad = noteFieldAad(noteId, field)
        return encryptRaw(plaintext.toByteArray(Charsets.UTF_8), getMekKey(), aad)
    }

    /** Decrypts an envelope produced by [encrypt]. noteId/field must match what was used to encrypt. */
    fun decrypt(envelope: String, noteId: UUID, field: NoteField): String {
        val aad = noteFieldAad(noteId, field)
        return String(decryptRaw(envelope, getMekKey(), aad), Charsets.UTF_8)
    }

    /**
     * Wraps a raw MEK under the caller-supplied MUK. Not tied to any note — used at
     * registration (wrap a freshly generated MEK) and whenever the MUK-wrapped copy
     * needs to be re-derived (e.g. password change).
     */
    fun wrapMek(mek: ByteArray, muk: ByteArray): String =
        encryptRaw(mek, SecretKeySpec(muk, "AES"), MEK_WRAP_AAD)

    /** Unwraps a MEK envelope under the caller-supplied MUK — used at login, after fetching the wrapped copy. */
    fun unwrapMek(envelope: String, muk: ByteArray): ByteArray =
        decryptRaw(envelope, SecretKeySpec(muk, "AES"), MEK_WRAP_AAD)

    private fun encryptRaw(plaintext: ByteArray, key: SecretKeySpec, aad: ByteArray): String {
        val nonce = getNonce()
        val spec = GCMParameterSpec(TAG_SIZE, nonce)

        val aes = Cipher.getInstance(TRANSFORM)
        aes.init(Cipher.ENCRYPT_MODE, key, spec)
        aes.updateAAD(aad)

        val result = aes.doFinal(plaintext)
        val envelope = byteArrayOf(VERSION) + nonce + result
        return Base64.getEncoder().encodeToString(envelope)
    }

    private fun decryptRaw(envelope: String, key: SecretKeySpec, aad: ByteArray): ByteArray {
        val envelopeBytes = Base64.getDecoder().decode(envelope)
        val version = envelopeBytes[0]
        require(version == VERSION) { "Unsupported envelope version: $version" }

        val nonce = readNonce(envelopeBytes)
        val ciphertext = envelopeBytes.copyOfRange(1 + NONCE_SIZE, envelopeBytes.size)
        val spec = GCMParameterSpec(TAG_SIZE, nonce)

        val aes = Cipher.getInstance(TRANSFORM)
        aes.init(Cipher.DECRYPT_MODE, key, spec)
        aes.updateAAD(aad)

        return aes.doFinal(ciphertext)
    }

    private fun noteFieldAad(noteId: UUID, field: NoteField): ByteArray =
        (noteId.toString() + AAD_DELIMITER + field.aadIdentifier).toByteArray(Charsets.UTF_8)

    private fun getNonce(): ByteArray {
        val nonce = ByteArray(NONCE_SIZE)
        SecureRandom().nextBytes(nonce)
        return nonce
    }

    private fun readNonce(byteInput: ByteArray): ByteArray {
        return byteInput.copyOfRange(1, 1 + NONCE_SIZE)
    }
}
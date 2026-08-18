package com.martinsterentjevs.cacheit.services.crypto

import com.martinsterentjevs.cacheit.services.security.SecurityService
import java.util.UUID

enum class NoteField(val aadIdentifier: String) {
    TITLE("note.title"),
    BODY("note.body"),
    ENC_DRAWING("note.encDrawing"),
}

interface CryptoService {
    fun encryptField(plaintext: String, noteId: UUID, field: NoteField): String
    fun decryptField(envelope: String, noteId: UUID, field: NoteField): String
    fun wrapMek(mek: ByteArray, muk: ByteArray): String
    fun unwrapMek(envelope: String, muk: ByteArray): ByteArray
    fun hashPassword(input: String): ByteArray
    fun hashMek(input: String): ByteArray
    fun generateMek(): ByteArray
    fun generateSalt(): ByteArray
}

internal class CryptoServiceImpl(securityService: SecurityService) : CryptoService {

    private val aesWrapper = AESWrapper(securityService)
    private val argonWrapper = ArgonWrapper(securityService)
    private val generator = MaterialGenerator(securityService)

    override fun encryptField(plaintext: String, noteId: UUID, field: NoteField): String =
        aesWrapper.encrypt(plaintext, noteId, field)

    override fun decryptField(envelope: String, noteId: UUID, field: NoteField): String =
        aesWrapper.decrypt(envelope, noteId, field)

    override fun wrapMek(mek: ByteArray, muk: ByteArray): String = aesWrapper.wrapMek(mek, muk)

    override fun unwrapMek(envelope: String, muk: ByteArray): ByteArray = aesWrapper.unwrapMek(envelope, muk)

    override fun hashPassword(input: String): ByteArray = argonWrapper.hashPassword(input)

    override fun hashMek(input: String): ByteArray = argonWrapper.hashMekKey(input)

    override fun generateMek(): ByteArray = generator.generateMek()

    override fun generateSalt(): ByteArray = generator.generateSalt()
}
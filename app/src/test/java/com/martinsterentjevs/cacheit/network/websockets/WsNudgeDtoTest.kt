package com.martinsterentjevs.cacheit.network.websockets

import com.martinsterentjevs.cacheit.network.cacheItJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Instant

class WsNudgeDtoTest {

    @Test
    fun `decodes a NOTE_UPDATED payload with all fields present`() {
        val json = """
            {"type":"NOTE_UPDATED","noteId":"4e9cf9ac-9175-4ba6-9483-a8ed4f64610d","lastModifiedAt":"2026-09-08T20:30:55.491967Z"}
        """.trimIndent()

        val nudge = cacheItJson.decodeFromString<WsNudgeDto>(json)

        assertEquals(WsNudgeType.NOTE_UPDATED, nudge.type)
        assertEquals("4e9cf9ac-9175-4ba6-9483-a8ed4f64610d", nudge.noteId)
        assertEquals(Instant.parse("2026-09-08T20:30:55.491967Z"), nudge.lastModifiedAt)
        assertNull(nudge.lockedByDeviceId)
    }

    @Test
    fun `decodes a NOTE_DELETED payload`() {
        val json = """{"type":"NOTE_DELETED","noteId":"abc-123","lastModifiedAt":"2026-09-08T20:30:55Z"}"""

        val nudge = cacheItJson.decodeFromString<WsNudgeDto>(json)

        assertEquals(WsNudgeType.NOTE_DELETED, nudge.type)
        assertEquals("abc-123", nudge.noteId)
    }

    @Test
    fun `decodes a NOTE_LOCK_ACQUIRED payload with lockedByDeviceId and no timestamp`() {
        val json = """{"type":"NOTE_LOCK_ACQUIRED","noteId":"abc-123","lockedByDeviceId":"device-456"}"""

        val nudge = cacheItJson.decodeFromString<WsNudgeDto>(json)

        assertEquals(WsNudgeType.NOTE_LOCK_ACQUIRED, nudge.type)
        assertEquals("device-456", nudge.lockedByDeviceId)
        assertNull(nudge.lastModifiedAt)
    }

    @Test
    fun `decodes a NOTE_LOCK_RELEASED payload with only type and noteId`() {
        val json = """{"type":"NOTE_LOCK_RELEASED","noteId":"abc-123"}"""

        val nudge = cacheItJson.decodeFromString<WsNudgeDto>(json)

        assertEquals(WsNudgeType.NOTE_LOCK_RELEASED, nudge.type)
        assertNull(nudge.lastModifiedAt)
        assertNull(nudge.lockedByDeviceId)
    }

    @Test
    fun `ignores unknown fields rather than failing`() {
        // ignoreUnknownKeys = true on cacheItJson - guards against the server adding a field
        // this DTO doesn't know about yet and every client silently breaking as a result.
        val json = """
            {"type":"NOTE_RESTORED","noteId":"abc-123","lastModifiedAt":"2026-09-08T20:30:55Z","someNewServerField":42}
        """.trimIndent()

        val nudge = cacheItJson.decodeFromString<WsNudgeDto>(json)

        assertEquals(WsNudgeType.NOTE_RESTORED, nudge.type)
    }
}
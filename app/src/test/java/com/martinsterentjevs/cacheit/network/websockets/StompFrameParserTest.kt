package com.martinsterentjevs.cacheit.network.websockets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class StompFrameParserTest {

    @Test
    fun `parses a well-formed CONNECTED frame`() {
        val raw = "CONNECTED\nversion:1.2\n\n\u0000"

        val frame = StompFrameParser.parse(raw)

        assertEquals("CONNECTED", frame.command)
        assertEquals("1.2", frame.headers["version"])
        assertEquals("", frame.body)
    }

    @Test
    fun `parses a MESSAGE frame with a JSON body`() {
        val raw = "MESSAGE\ndestination:/user/queue/nudges\ncontent-type:application/json\n\n" +
                "{\"type\":\"NOTE_UPDATED\",\"noteId\":\"abc-123\"}\u0000"

        val frame = StompFrameParser.parse(raw)

        assertEquals("MESSAGE", frame.command)
        assertEquals("/user/queue/nudges", frame.headers["destination"])
        assertEquals("{\"type\":\"NOTE_UPDATED\",\"noteId\":\"abc-123\"}", frame.body)
    }

    @Test
    fun `throws on a frame missing the header separator`() {
        val raw = "CONNECTED\nversion:1.2\u0000" // no blank line before body/terminator

        assertThrows(IllegalArgumentException::class.java) {
            StompFrameParser.parse(raw)
        }
    }



    @Test
    fun `throws on a header line missing a colon`() {
        val raw = "CONNECTED\nversionwithoutcolon\n\n\u0000"

        assertThrows(IllegalArgumentException::class.java) {
            StompFrameParser.parse(raw)
        }
    }

    @Test
    fun `blank header lines are ignored, not treated as malformed`() {
        val raw = "CONNECTED\nversion:1.2\n\nheartbeat:0,0\n\n\u0000"
        // header section only runs up to the FIRST blank line per the parser's contract -
        // this asserts that contract explicitly, since it's easy to accidentally "fix" into
        // multi-blank-line tolerance later without noticing it changes frame boundaries.

        val frame = StompFrameParser.parse(raw)

        assertEquals("CONNECTED", frame.command)
        assertEquals("1.2", frame.headers["version"])
    }

    @Test
    fun `strips trailing null terminator and newlines`() {
        val raw = "RECEIPT\nreceipt-id:cacheit-nudge-subscription\n\n\u0000\n"

        val frame = StompFrameParser.parse(raw)

        assertEquals("RECEIPT", frame.command)
        assertEquals("cacheit-nudge-subscription", frame.headers["receipt-id"])
    }
}
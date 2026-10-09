package com.martinsterentjevs.cacheit.network.websockets

object StompFrameParser {

    fun parse(raw: String): StompFrame {
        val normalized = raw.trimEnd('\u0000')

        val headerEnd = normalized.indexOf("\n\n")

        if (headerEnd == -1) {
            throw IllegalArgumentException("Invalid STOMP frame: missing header separator")
        }

        val headerSection = normalized.substring(0, headerEnd)
        val body = normalized.substring(headerEnd + 2)

        val lines = headerSection.lines()

        if (lines.isEmpty() || lines[0].isBlank()) {
            throw IllegalArgumentException("Invalid STOMP frame: missing command")
        }

        val command = lines[0]

        val headers = lines
            .drop(1)
            .filter { it.isNotBlank() }
            .associate { line ->
                val separator = line.indexOf(':')

                if (separator == -1) {
                    throw IllegalArgumentException(
                        "Invalid STOMP header: $line"
                    )
                }

                val key = line.substring(0, separator)
                val value = line.substring(separator + 1)

                key to value
            }

        return StompFrame(
            command = command,
            headers = headers,
            body = body
        )
    }
}
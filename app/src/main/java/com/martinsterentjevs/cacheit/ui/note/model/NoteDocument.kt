package com.martinsterentjevs.cacheit.ui.note.model

import kotlinx.serialization.Serializable

/**
 * Versioned logical representation of a note.
 *
 * Text is represented as visual lines because the note has a fixed logical width.
 * `line` is the zero-based logical row. A line's vertical position is therefore:
 * line * NoteSpace.TEXT_LINE_HEIGHT.
 *
 * Drawings use the same note-space coordinates as text. Their points are NOT
 * expressed in a separate 4000x4000 drawing space.
 */
@Serializable
data class NoteDocument(
    val version: Int = 1,
    val elements: Map<Int,NoteElement> = emptyMap(),
)

@Serializable
data class NoteElement(
    val content:String
)
package com.martinsterentjevs.cacheit.ui.note.components.drawing

import com.martinsterentjevs.cacheit.ui.note.model.NoteSpace
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

object DrawingSerializer {
    private const val CURRENT_SCHEMA_VERSION = 1

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun serialize(document: DrawingDocument): String =
        json.encodeToString(document)

    fun deserialize(value: String): DrawingDocument {
        try {
            val document =
                json.decodeFromString<DrawingDocument>(value)

            validate(document)

            return document
        } catch (e: DrawingSerializationException) {
            throw e
        } catch (e: SerializationException) {
            throw DrawingSerializationException(
                "Malformed drawing JSON",
                e,
            )
        } catch (e: IllegalArgumentException) {
            throw DrawingSerializationException(
                "Invalid drawing document",
                e,
            )
        }
    }

    private fun validate(document: DrawingDocument) {
        require(
            document.schemaVersion ==
                    CURRENT_SCHEMA_VERSION,
        ) {
            "Unsupported drawing schema version: " +
                    "${document.schemaVersion}"
        }

        document.strokes.forEachIndexed { strokeIndex, stroke ->
            require(stroke.width > 0) {
                "Invalid width in stroke $strokeIndex"
            }

            if (stroke.tool == DrawingTool.Eraser) {
                require(stroke.color == null) {
                    "Eraser stroke $strokeIndex must not have a color"
                }
            } else {
                require(stroke.color != null) {
                    "Stroke $strokeIndex requires a color"
                }
            }

            stroke.points.forEachIndexed { pointIndex, point ->
                require(
                    point.x in
                            0..NoteSpace.BASE_WIDTH.toInt(),
                ) {
                    "Invalid x at stroke $strokeIndex, " +
                            "point $pointIndex"
                }

                /*
                 * Note height is document-dependent, so the serializer
                 * cannot impose an upper Y bound without the NoteDocument.
                 *
                 * DrawingLayer validates/constrains Y against the current
                 * NoteLogicalSize when converting physical input.
                 */
                require(point.y >= 0) {
                    "Invalid y at stroke $strokeIndex, " +
                            "point $pointIndex"
                }

                require(point.pressure in 0f..1f) {
                    "Invalid pressure at stroke $strokeIndex, " +
                            "point $pointIndex"
                }
            }
        }
    }
}

class DrawingSerializationException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
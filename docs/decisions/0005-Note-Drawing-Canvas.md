# 0005 — Portable Note Drawing Representation

**Status:** Accepted
**Date:** Created - 26/08/2026 Finalize - 03/09/2026
**Related:**
- ADR 0004 — NoteEdit Redesign Criteria (checked against 0004's final Accepted text
  directly - mode table, layering table, and back-navigation rules are all consistent with what
  follows; earlier drafting of this ADR happened before 0004 had surfaced in full)
- ADR-0005 companion document outlining issues and timeline of implementation progress,
  this document contains the version live as of Issue #7, if any element seems stale,
  check the companion for additional info

## Context

Notes may contain both text and drawing content. The NoteEdit redesign treats text and drawing
as two rendering layers occupying the same note surface, with their visual ordering and touch
interaction changing according to the active screen mode. `View`, `TextEdit`, and `DrawingEdit`
therefore need to be able to render the same underlying drawing data differently without
changing the stored note representation. ADR 0004 separates rendering order from interaction
ownership and establishes drawing as a persistent part of the complete `FaceNote`.

The drawing system is intended to be usable beyond the Android/Jetpack Compose client. In
particular, the same persisted drawing representation should eventually be renderable by any future client
(be it iOS or desktop). Therefore, the stored drawing must not depend on Android canvas classes,
Compose-specific objects, device pixel dimensions, or other renderer-specific state.

The drawing format also needs to preserve enough input information to support pressure-sensitive
tools while remaining simple enough for the initial implementation.

The drawing editor will initially provide a small set of tools: a basic pen with a selector color wheel, eraser and undo/redo.
It is presented as a floating tool island on the right side of the note canvas.
The toolbar and editor are client concerns; the persisted representation describes the resulting drawing
rather than the Android UI used to create it.

## Decision

### 1. Drawings use a renderer-independent JSON document

A note's drawing content is represented as a serialized JSON drawing document, describing
drawing operations/strokes rather than a rasterized image or renderer-specific canvas state.

The conceptual structure is:

```text
DrawingDocument
├── schemaVersion
└── strokes[]

Stroke
├── tool
├── width
├── color        (nullable - null for tools with no color concept, e.g. eraser)
└── points[]

Point
├── x
├── y
└── pressure
```

The JSON format is deliberately independent of Compose, Android `Path`, Avalonia drawing
primitives, device resolution, or physical screen dimensions. A renderer is responsible for
converting the logical drawing representation into its own native rendering primitives.

### 2. The logical drawing canvas is a content driven logical space with initial 9:21 aspect ratio

The logical canvas went through two square iterations before landing here - a fixed
1000×1000 space, then a fixed 4000×4000 space intended to sit above 4K rendering targets so
downscaling would never lose fidelity. Both were rejected: a note is read top-to-bottom like a
page, not viewed as a fixed square, and a square canvas forces either wasted space or a
zoom/pan model fighting the note's actual shape instead of following it. The full iteration
history is in the companion document.

The canvas is fixed-width, variable-height, matching `NoteSpace`:

- `BASE_WIDTH = 360`, `BASE_HEIGHT = 840` - a 9:21 aspect ratio at minimum content, one
  logical unit per dp at base scale
- Height grows with content via `NoteSpace.heightForLines()`: `max(BASE_HEIGHT, lineCount *
  TEXT_LINE_HEIGHT)` - a note with more text than fits the base height gets a taller logical
  canvas rather than a scaled-down one
- Width never changes. A fixed horizontal extent is what keeps a stroke spatially anchored to
  the text line it annotates as the note grows or shrinks above and below it

Drawing coordinates live in this same logical space - a stroke's `(x, y)` is a position on the
note, not on an independent square drawing surface. Text and drawing share one coordinate
system specifically so a drawn annotation stays attached to what it marks up.

### 3. The viewport is renderer state, not drawing data

The logical canvas has an adaptive maximum size, based on the note line count;
the physical viewport is determined by the client. On Android, the viewport is the available
NoteCanvas area, starting at the logical origin `(0,0)` when opened, movable via a two-finger gesture.
Viewport position is **not** persisted in the drawing JSON.

### 4. Coordinates are logical rather than physical

A point such as `{"x": 250, "y": 400}` means the same logical location regardless of whether
the drawing is rendered on a phone, tablet, desktop window, or Avalonia canvas. The renderer
maps the logical space to its available viewport. No device-specific
conversion values are stored in the drawing.

### 5. Pressure is stored as normalized input data

Points may contain normalized pressure in the range `0.0..1.0`, defaulting to `1.0` when the
input source doesn't provide meaningful pressure. Pressure is preserved even when a particular
tool doesn't use it - Pen is pressure-aware; Eraser carries the field for format consistency
but currently ignores it. A renderer
that doesn't support pressure can render using its default tool behavior. The parser does not
discard pressure merely because the current renderer doesn't use it, preserving the
information for other or future clients.

### 6. Pressure sensitivity is a tool capability

Pressure is not inherently applied to every tool. Tools have a capability/semantic indicating
whether pressure affects their rendering, while the stored point format stays consistent
(`x`, `y`, `pressure`) regardless of which tool produced it. Exact pressure-to-rendering curves
are intentionally not part of this storage-format decision - different clients may implement
equivalent tool behavior using their own rendering APIs.

### 7. Width and color are stroke properties, constant for the stroke's lifetime

Width and color belong to the stroke, not individual points - one width and one color per
stroke. Changing the selected color or width ends the current stroke and starts a new one with
the new properties, rather than mutating the stroke in progress:

```text
Stroke A: color = black          (not: one stroke containing multiple colors)
Stroke B: color = red
```

This keeps the representation simple and predictable. Variable-width rendering can still be
supported by pressure-aware tools without storing a width value for every point. The same
principle applies to any other fixed stroke property introduced later.

### 8. Erasing is a first-class tool, not a modifier

The eraser produces its own stroke (`tool = eraser`, `color = null`, its own `width` and
`points`) rather than acting as a modifier applied to existing strokes. An eraser stroke does
not immediately rewrite, split, or delete underlying stroke geometry - renderers interpret it
as a request to remove rendered content beneath its path.

### 9. Erasing is implemented through rendering/masking, not destructive geometry modification

```text
normal strokes → render → eraser strokes → apply transparency/mask → final drawing
```

The exact masking/compositing mechanism is renderer-specific (Android/Compose and Avalonia may
each use their own supported approach); the persisted representation stays the same either way.
The JSON must not contain a precomputed alpha bitmap or renderer-specific mask - this avoids
the cost and complexity of destructively finding, splitting, or deleting every stroke
intersected by an eraser, and preserves the eraser as part of drawing history (`Pencil A,
Pencil B, Eraser C`) rather than permanently mutating A and B.

### 10. Drawing documents are versioned at the document level, via `schemaVersion`

```json
{ "schemaVersion": 1, "strokes": [] }
```

Named `schemaVersion` rather than `version` specifically to avoid collision with this
project's existing, unrelated use of "version" for note edit history (`NoteVersion`,
`NoteVersionDto`, `hasHistory`) - a drawing's `version` field living inside a `NoteVersion`
snapshot would be a real source of confusion in code and conversation ("this version's drawing
has version 1") that costs nothing to avoid now, before anything is implemented.

This field identifies the drawing serialization format, not the application version.
Individual strokes do not carry their own format version. Versioning exists to let future
clients evolve the representation while retaining the ability to identify which format a
stored drawing uses.

### 11. Empty drawings may be represented as absent or empty content

At the note level, `drawing = null` is valid. A drawing document with no strokes
(`{"schemaVersion": 1, "strokes": []}`) is also valid. Both are considered empty drawing
content, and the implementation may normalize between them where convenient - but must not
interpret malformed JSON as either of these (see below).

### 12. Malformed drawing JSON does not invalidate the entire note, and must not be silently overwritten on next save

If drawing JSON cannot be parsed: the drawing is ignored for rendering, the user is notified
that drawing content couldn't be read, the rest of the note remains usable, and the malformed
persisted value must not automatically be replaced with an empty drawing. In particular, this
sequence must not happen:

```text
malformed JSON → parse failure → [] → user saves note → original drawing data destroyed
```

**This requires an explicit corollary on in-memory representation, not just intent.** Stating
the failure mode isn't sufficient to prevent it - if a parse failure falls back to an empty or
default `DrawingDocument` purely to have something to render, the next save will faithfully
re-serialize *that* empty document over the original, reproducing exactly the sequence above.
The in-memory drawing state must therefore distinguish "successfully parsed" from
"unparseable, raw value preserved verbatim" as genuinely different states, not both collapsing
to some `DrawingDocument` instance:

```kotlin
sealed interface DrawingState {
    data class Parsed(val document: DrawingDocument) : DrawingState
    data class Unparseable(val rawValue: String) : DrawingState
}
```

On save, `Unparseable` re-emits `rawValue` unchanged. A malformed drawing is a data-read
problem, not equivalent to an empty drawing, and the application retains the original
persisted value until an explicit recovery or replacement decision is made.

### 13. The drawing representation is independent of the drawing UI

The JSON format does not describe the Android drawing toolbar (Pen, Eraser, color selection via
a color wheel, undo/redo, presented via a floating tool island on the right side of the
NoteCanvas). These controls configure how subsequent strokes are produced, but their UI
structure is not persisted - only the resulting stroke semantics are.

Pencil and marker were part of the toolbar in early drafts but rendered strokes identically to
Pen - no differentiated width, opacity, or blend behavior existed to justify a separate tool.
They were removed rather than kept as unused decoration. `DrawingTool` covers only `Pen` and
`Eraser` for now; a future tool needs an actual differentiated rendering path before it's added
back, not just a new enum case.

### 14. Drawing and text remain independent content layers

The drawing representation contains no text content. A note continues to hold independent
`title`, `body`, and `drawing` fields on `FaceNote`. `NoteCanvas` composes the text and drawing
layers visually; the active NoteEdit mode determines which layer is interactive and which is
visually dominant, per ADR 0004's layering table. The drawing format itself has no awareness
of whether it's currently being viewed, edited, dimmed underneath text, or rendered on top.

## Example representation

```json
{
  "schemaVersion": 1,
  "strokes": [
    {
      "tool": "pen",
      "width": 4,
      "color": { "red": 0, "green": 0, "blue": 0, "alpha": 255 },
      "points": [
        { "x": 120, "y": 240, "pressure": 0.72 },
        { "x": 125, "y": 244, "pressure": 0.81 },
        { "x": 131, "y": 249, "pressure": 1.0 }
      ]
    }
  ]
}
```

An eraser follows the same structure, with `color: null`:

```json
{
  "schemaVersion": 1,
  "strokes": [
    {
      "tool": "eraser",
      "width": 32,
      "color": null,
      "points": [
        { "x": 400, "y": 300, "pressure": 1.0 },
        { "x": 430, "y": 305, "pressure": 1.0 }
      ]
    }
  ]
}
```

Color is a structured `{red, green, blue, alpha}` object rather than a hex string, matching
`DrawingColor`'s validated `0..255` per-channel representation. Beyond that, exact serialized
field names and Kotlin representation remain an implementation detail unless separately
elevated into a formal schema.

## Consequences

### Easier

- The same drawing data can be rendered by Compose, Avalonia, or another future client
- Physical screen resolution does not affect persisted drawing coordinates
- The drawing can be scaled to different viewports without changing its stored geometry
- Pressure information is preserved even for clients that don't currently use it
- Stroke properties stay simple - width and color are constant per stroke
- Erasing doesn't require immediately rewriting or splitting existing geometry
- Future undo/redo can potentially treat eraser operations as ordinary drawing operations
- `schemaVersion` gives future clients a way to distinguish incompatible drawing formats
  without colliding with this project's unrelated note-version-history terminology
- The NoteEdit mode system can treat the drawing as a renderer-independent content layer
- No changes needed to the encryption layer - the drawing document is plaintext-before-
  encryption exactly like title/body always were, so `NoteField.ENC_DRAWING` and the existing
  AAD scheme are untouched by this decision

### Harder

- Every renderer must correctly map the content-driven logical canvas (fixed width, height
  computed from note content) into its physical viewport, and recompute logical height as
  content changes rather than assuming a fixed bound
- Renderer implementations must agree on tool semantics closely enough for drawings to look
  consistent across clients
- Eraser rendering requires compositing/masking support, processed in the correct stroke order
  rather than simply drawing all strokes independently
- Pressure-aware tools may produce visually different results between clients if pressure
  curves aren't standardized later
- Malformed drawing data requires a real user-facing recovery path (see point 12), not just a
  documented intent to avoid data loss

### Deferred

Intentionally not fixed by this ADR: exact pressure-to-width/opacity curves, exact marker
opacity model, exact pencil texture, exact eraser compositing implementation, undo/redo
behavior, pinch-to-zoom behavior, drawing selection/movement, stroke transformation/grouping,
synchronization/merge behavior for concurrent drawing edits, exact JSON schema beyond the
conceptual structure, exact color encoding format, exact width units, future drawing objects
beyond strokes.

**Also deferred, flagged explicitly rather than left implicit:** `GET /notes` already returns
full `NoteDto` per note including `encDrawing`, but `NoteCard` only ever needs a boolean
(`hasDrawing`) for its corner icon - never the actual strokes. A drawing with several thousand
points (pressure-per-point adds up) means every list fetch carries full stroke data for notes
the list never renders. This isn't new - the field always existed - but this ADR is what makes
the growth potential concrete. Not solved here: acceptable for now at low note counts (same
reasoning as the server's `hasHistory` N+1 - correct now, worth revisiting with real usage
data), with a possible future direction of a small cached raster thumbnail for list rendering
alongside the vector source, without giving up JSON as the editing source of truth.

These can be decided when the drawing implementation requires them rather than prematurely
constraining the format.

## Alternatives considered

**Raster image storage.** Rejected - tied to a particular resolution, loses the
renderer-independent stroke information future clients need, and makes pressure, tool
semantics, and future editing significantly less useful.

**Android/Compose-specific path serialization.** Rejected - couples the note format to one
client implementation and makes the planned Avalonia renderer substantially harder.

**Device-pixel coordinates.** Rejected - the same drawing would have different geometry when
rendered on devices with different resolutions or canvas sizes. The content-driven logical
coordinate system in Decision 2 avoids this entirely - a fixed base width plus a height derived
from note content, independent of any device's physical resolution.

**Destructive erasing.** Rejected for the initial implementation - requires locating and
modifying potentially many existing strokes and can require splitting stroke geometry around
erased regions, adding computational and implementation complexity without being necessary
yet. First-class eraser strokes with rendering-time masking give a simpler initial
representation while preserving the original stroke data.

**Persisted alpha masks.** Rejected - increases storage requirements, complicates
synchronization, and undermines the renderer-independent stroke representation. The mask is
derived rendering state and should be generated by the renderer from eraser strokes, not
stored.

**Removing pressure when a tool doesn't use it.** Rejected - pressure stays part of the point
representation even when a particular tool ignores it, preserving input information for future
clients and keeping the serialized point structure independent of the active tool.

**Persisting viewport position.** Rejected - viewport position is client interaction state,
not drawing content. Persisting it would make a drawing's stored representation depend on how
one client happened to be viewing it at save time.

## Implementation boundary

```text
Serialized FaceNote.drawing
            │
            ▼
    DrawingState (Parsed | Unparseable)
            │
      ┌─────┴─────┐
      ▼           ▼
   Renderer    Input/tooling
      │           │
      │           └── creates/updates strokes
      │
      └── renders the content-driven logical canvas (`NoteSpace`)
```

`NoteCanvas` consumes the drawing subsystem rather than implementing JSON parsing, stroke
serialization, or tool semantics itself. `NoteEditViewModel` remains responsible for note-level
state and persistence; the drawing subsystem is responsible for interpreting and producing
drawing content. This keeps the boundary between note editing state, drawing data, and client
rendering explicit.
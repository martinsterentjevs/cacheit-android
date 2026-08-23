# 0002 - Note Visibility Mapping
Status: Proposed
Date: 2026-08-19

## Context
As a result of Issue #4, there is no unencrypted representation of a note usable
by the UI. Server-side and local storage both hold only encrypted fields, per
the zero-trust architecture (client-side AES-256-GCM, server/at-rest storage
never holds plaintext). `CryptoService` handles field-level encrypt/decrypt.
This ADR defines the in-memory mapping layer that gives the UI something
readable, without weakening that guarantee anywhere data is persisted.

## Decision
`NoteDto` (and its version equivalent) remain the encrypted wire/storage
format everywhere data leaves memory - network calls and local storage both
stay ciphertext-only. A new in-memory-only type, `FaceNote` (and
`FaceNoteVersion`), represents decrypted content for UI consumption. Mapping
between the two happens at the repository boundary via `NoteCryptoMapper`,
never inline in ViewModels or screens.

**Read path** (`getNotes`, `getVersion`, etc.) - `NoteDto` in, `FaceNote`/
`FaceNoteVersion` out. Decryption failure on an individual note is isolated
per-note, not propagated as an exception that fails the whole call: failed
notes are excluded from the returned list and surfaced to the user as an
informational popup ("Failed to decrypt N notes"), rather than blocking or
corrupting the rest of the list.

**Write path** (`addNote`, `updateNote`) - `FaceNote` in, encrypted to
`NoteDto` immediately before the network call. The server's response is
re-decrypted rather than merged onto the pre-write `FaceNote`: this doubles as
a correctness check on the encrypt/decrypt round-trip itself, since crypto
bugs here fail silently and are otherwise only caught downstream on a
different device.

If re-decryption of the response fails, the write is treated as unverified,
not failed - the server call already returned success, so the note likely
did persist correctly and the failure is local-only. No compensating
delete/rollback call is made. On `updateNote`, the display reverts to the
pre-edit `FaceNote`; on `addNote`, the note is not added to the visible list
(no prior state exists to revert to). Either way, an info popup states the
change couldn't be confirmed rather than that it failed, and the next sync
pull (existing WebSocket-nudge → REST-pull delta mechanism) reconciles
whatever the server actually holds - no bespoke retry/rollback logic is
built for this path.

The verbose log on this failure records `noteId`, exception type, and stack
trace only - decrypted plaintext is never written to logs, even for
debugging, per the zero-trust model.
**noteId-keyed operations** (`deleteNote`, `acquireDrawingLock`,
`releaseDrawingLock`, `getVersionHistory`, `restoreVersion`) operate on
`noteId`/`versionId` directly and don't touch `FaceNote` at all - no content
to represent.

## Consequences
UI code only ever sees `FaceNote`; encryption details stay fully contained in
the repository layer. Decrypt failures degrade gracefully (partial list +
notice) instead of an all-or-nothing failure. Adds one conversion pass on
every read and write, and a second in-memory type to keep in sync with
`NoteDto` if the note schema changes.

## Alternatives considered
Passing `NoteDto` directly to the UI and decrypting inline per-screen -
rejected: duplicates the field/AAD convention across every screen instead of
one mapper, and makes it easy to miss the per-note failure isolation this ADR
requires.
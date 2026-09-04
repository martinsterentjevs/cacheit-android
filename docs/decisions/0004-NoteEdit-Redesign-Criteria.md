# 0004 — NoteEdit Redesign Criteria
Created: 25/08/2026
Status: Accepted

## Context

During Issue #6, `NoteEditScreen` and its ViewModel were a temporary outline - just enough to
work: two plain fields for data entry, a bare "add drawing" button with no real interaction
model behind it, and lock acquire/release wired only partially (missing on back-navigation
mid-draw).

The redesign was scoped in the [pre-MVP checklist](../Scope_Of_PreMVP_Checklist.md), originally
defined as:

> canvas with text lines approach. **Text and drawing are exclusive per note, not co-existing
> in one canvas** (per the original wireframe's pencil/cursor mode toggle) - this is a real
> constraint on the redesign, not just a visual choice. The current scaffold shows a body text
> field and a drawing-entry button simultaneously on the same screen; the redesign replaces
> that with an exclusive mode toggle, not adds to it.

That reading treats a note as either text-only or drawing-only for its whole lifetime. A basic
use case breaks this immediately: using a drawing to mark up or annotate something already
written in text. Exclusivity makes that impossible by construction, not just inconvenient -
so the criteria needed re-evaluation before implementation started, not after.

## Decision

Notes hold text and drawing content together, always. The four screen modes below control
which content is *interactive* at a given moment - they are not four kinds of notes, and a
note never has an exclusive "type."

### Screen modes

| Screen mode     | Purpose                | Content interaction                              | Primary action |
|-----------------|------------------------|--------------------------------------------------|----------------|
| **Create**      | Create a new note      | Text initially editable; drawing can be selected | Save/create    |
| **View**        | Read an existing note  | Nothing editable                                 | Enter edit     |
| **TextEdit**    | Modify text content    | Text interactive; drawing non-interactive        | Save           |
| **DrawingEdit** | Modify drawing content | Drawing interactive; text non-interactive        | Save           |

### Interaction exclusion per mode

| Mode        | Edit Text             | Edit Drawing              | Notes                                                    |
|-------------|-----------------------|---------------------------|----------------------------------------------------------|
| Create      | Goes to TextEdit mode | Unavailable until toggled | Starts on TextEdit; drawing available by explicit toggle |
| View        | Unavailable           | Unavailable               | Viewing only                                             |
| TextEdit    | Add/Edit text         | Unavailable               | Text elements only are interactive                       |
| DrawingEdit | Unavailable           | Fully featured            | Drawn elements only are interactive                      |

### Layering and rendering per mode

"Top layer" is z-order; "interactive" is which layer receives touch. These are independent -
a layer can be visible without being the one that responds to input.

| Mode            | Top layer | Drawing opacity | Text interactive      | Drawing interactive               | Transition                                                     |
|-----------------|-----------|-----------------|-----------------------|-----------------------------------|----------------------------------------------------------------|
| **Create**      | Drawing   | 100%            | Yes (default focus)   | No (nothing drawn yet, typically) | n/a - initial state                                            |
| **View**        | Drawing   | 100%            | No                    | No                                | n/a - tap routes to mode choice, not to either layer's content |
| **TextEdit**    | Text      | 50%             | Yes                   | No (touch passes through)         | 150ms cross-fade in/out, matching existing dialog entry timing |
| **DrawingEdit** | Drawing   | 100%            | No (inert underneath) | Yes (captures all touch)          | n/a - drawing already on top, no swap needed                   |

TextEdit is the only mode where the top layer and the dimmed layer differ from the other
three. Drawing dims to 50% specifically so drawn content stays legible as context while text
input takes visual priority - the alpha step is cosmetic continuity, not a legibility
requirement, since text always renders fully opaque and on top regardless of drawing density.

**Follow-up question, not blocking acceptance:** whether Create should adopt TextEdit's
layering rule once a drawing exists on an in-progress new note (drawing added, then user
returns to typing the title before the first save). Functionally this is the same situation as
TextEdit on an existing note, so the answer is likely yes, but that's an inference rather than
a decided rule - confirm before relying on it during implementation.

### Back navigation is a mode exit, not a screen exit

Back navigation from an edit mode means "finish editing," not "leave the note." `View` is the
stable resting state; `TextEdit` and `DrawingEdit` are transient states that always exit
through a save attempt, never a bare discard. This directly replaces the standalone checklist
item "Drawing lock release on back-navigation mid-draw" - it is no longer a bolt-on fix, it is
a consequence of this transition rule.

| Mode            | Back triggers                                                       | On save success (`Verified`) | On save unconfirmed (`Unverified`)                   | On save failure (network/server error)       |
|-----------------|---------------------------------------------------------------------|------------------------------|------------------------------------------------------|----------------------------------------------|
| **Create**      | If blank: dismiss directly. If content exists: save (via `addNote`) | Dismiss, or → `View`         | Dismiss, or → `View`, show "couldn't confirm saved"  | Dismiss, or stay in `Create`, error surfaced |
| **TextEdit**    | Save (via `updateNote`)                                             | → `View`                     | → `View`, show "couldn't confirm saved"              | Stay in `TextEdit`, error already surfaced   |
| **DrawingEdit** | Save (via `updateNote`), then release lock **if held**              | Release lock → `View`        | Release lock → `View`, show "couldn't confirm saved" | Lock **not** released, stay in `DrawingEdit` |
| **View**        | Leave screen                                                        | n/a                          | n/a                                                  | n/a                                          |

**Lock release is conditional, not unconditional.** A lock only exists to release if one was
ever acquired. Per the Create-mode rule, `acquireDrawingLock`/`releaseDrawingLock` are never
called while `preEditNote == null` - a note with no server-side existence yet has no other
device that could contend for it, so entering `DrawingEdit` pre-save is a purely local
rendering state with no network calls behind it:

```kotlin
private fun releaseIfHeld() {
    if (current.preEditNote != null && current.isDrawingLocked) releaseDrawingLock()
}
```

**Failure (not `Unverified`) is what blocks the transition to `View`.** `Unverified` (per ADR
0002's write-path decision) means the server almost certainly has the write - only the local
redecrypt-confirmation failed - so releasing the lock and moving on is correct. A genuine
`NoteFlowException` means the write likely did *not* happen; releasing the lock and silently
returning to `View` in that case would discard unsaved strokes and abandon the lock for
nothing. Staying in the mode with the existing error popup lets the user retry instead.

**One save function, not two.** `updateNote`/`addNote` always take the whole `FaceNote`
(title + body + drawing together), matching the server's whole-note `PUT`/`POST` - there is no
partial-update endpoint for "just the drawing" or "just the title." What differs between
`TextEdit` and `DrawingEdit` exits is only whether a lock-release happens alongside the same
save call, not the save itself.

**Create dismisses if untouched, saves if not.** An unmodified, still-blank `Create` screen is
dismissed directly on back - there is no note worth creating from nothing. Any real content
(title, body, or drawing) takes the normal save-then-`View` path instead:

```kotlin
private fun isBlank(note: FaceNote) =
    note.title.isBlank() && note.body.isNullOrBlank() && note.drawing.isNullOrBlank()

fun exitCreate() {
    val current = uiState.value as? Ready ?: return
    if (isBlank(current.note)) {
        onBack() // leave directly - nothing to persist, no note ever existed
    } else {
        exitTextEdit() // real content - save via addNote, then View
    }
}

fun exitTextEdit() = viewModelScope.launch {
    when (saveInternal()) {
        is NoteWriteResult.Verified -> mode = Mode.View
        NoteWriteResult.Unverified -> { showUnconfirmedPopup(); mode = Mode.View }
        // failure: stay in TextEdit, error already surfaced by saveInternal()
    }
}

fun exitDrawingEdit() = viewModelScope.launch {
    when (saveInternal()) {
        is NoteWriteResult.Verified -> { releaseIfHeld(); mode = Mode.View }
        NoteWriteResult.Unverified -> { releaseIfHeld(); showUnconfirmedPopup(); mode = Mode.View }
        // failure: stay in DrawingEdit, lock (if any) still held, nothing silently lost
    }
}
```

`NoteEditScreen` doesn't currently intercept the system back button - this requires
`androidx.activity.compose.BackHandler`, dispatching on the current mode:

```kotlin
BackHandler(enabled = uiState.mode != Mode.View) {
    when (uiState.mode) {
        Mode.TextEdit -> viewModel.exitTextEdit()
        Mode.DrawingEdit -> viewModel.exitDrawingEdit()
        Mode.Create -> viewModel.exitCreate()
        Mode.View -> Unit // enabled=false covers this, unreachable
    }
}
```

When `mode == View`, `BackHandler` is disabled and the system default (pop the screen,
triggering `onBack()`) applies unchanged.

No other exit path in this app uses a discard/confirm dialog - AFK autosave and `Unverified`
writes both already resolve without blocking the user on a prompt. `Create` deliberately
follows the same pattern rather than introducing a second, inconsistent answer to "how do I
leave a screen."

## Consequences

**Easier:** annotation-style use (marking up existing text with a drawing) is possible at all,
which the exclusive model ruled out by construction. The client-side model now matches the
server schema, which already always allowed `encTitle`/`encBody`/`encDrawing` independently -
exclusivity would have been an artificial client-only restriction fighting the data model
rather than reflecting it. Lock handling becomes a direct consequence of the mode-exit rule
instead of a set of special cases to remember and re-discover as bugs.

**Harder:** per-mode hit-testing is real work, not a visual tweak - `TextEdit` and
`DrawingEdit` each need to make the *other* layer touch-transparent, not just visually
de-emphasized, or gestures aimed at one layer will incorrectly hit the other. The mode state
machine now owns more responsibility (save-branching by `NoteWriteResult` variant, conditional
lock release, blank-check on Create exit) than the original two-field scaffold ever needed to.

**Given up:** the simplicity of "a note is one of two kinds." Every future feature touching
note content now has to consider both text and drawing as potentially present together, rather
than treating them as mutually exclusive branches.

## Alternatives considered

**Leaving the criteria as originally defined (exclusive text-or-drawing per note).** Rejected
on more than preference - it fails the annotation use case outright (a drawing can't mark up
text it can't coexist with), it fights the server schema instead of matching it (both fields
have always been independently nullable, never enforced as either/or at the data layer), and
it forces an artificial commitment at note-creation time that has no clean way to change later
without a migration path this project has no reason to build. It also doesn't match how any
comparable note-taking app actually behaves, which is a signal the constraint was solving a
problem nobody has, not a real one.

**Dropping the redesign entirely.** Not a real option, not just an inferior one. The original
scaffold has a drawing-entry button with no drawing interaction model behind it, lock
acquire/release that's provably incomplete (missing on back-navigation), and zero answer for
how text and drawing would ever visually coexist on one screen. Skipping this work doesn't
avoid it - it just means discovering the same design questions piecemeal, as bugs, later,
under worse conditions (real user data on the line instead of a design doc).

**Per-mode discard-confirmation dialogs on back navigation** (raised during design, not in the
original scope). Rejected: it introduces a second, inconsistent pattern for "how do I leave a
screen" alongside the app's existing autosave-oriented approach elsewhere (AFK autosave and
`Unverified`-write handling both already resolve silently without blocking the user). Low-stakes
content like an abandoned note title doesn't warrant a confirmation prompt when the rest of the
app already treats similar situations as safe to save-and-move-on automatically.

**Separate `saveText()`/`saveDrawing()` partial-update functions.** Rejected: there is no
partial-update endpoint on the server to call - `updateNote`/`addNote` are whole-note
operations by design. Building separate client-side save paths for a distinction the API
doesn't support would add complexity with nothing to attach it to.

**Unconditional lock release on every exit, regardless of save outcome.** Rejected: releasing
the lock after a failed save (not `Unverified` - an actual network/server error) would discard
both the user's unsaved strokes and their exclusive hold on the note for no benefit, and open
the note to another device's edits while the local one silently lost work.
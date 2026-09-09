# Pre-MVP checklist
Created: 22/08/2026 Last updated: 24/08/2026
Status: Proposed

---
## Overview

The purpose of this document is to clearly outline the scope of Issue #7
The work definition is too large to add each item in the issue for overflow reasons therefore
this file serves as the grouping origin. The work items are split into 4 categories: small
issues (e.g. Password fields swapped out form regular text), medium, large, testing.

Deliberately kept as one issue rather than split into several smaller ones, to keep the
cross-links between related items (e.g. session navigation <-> TokenAuthenticator <-> cold
start) visible in one place rather than scattered across separate issues.

---
### Small items
>Items that don't need large changes, targeted at one or two re-useable elements

- [x] Password text fields (hidden text, visibility toggle, single line)
- [x] `NoteCard`'s `hasHistory` indicator: ~~currently shown twice (corner cut + footer icon) -
  pick one after actually looking at both on-device, remove the other~~ Remains showing both - view [ADR 0003](decisions/0003-Note-History-Marking.md)
- [x] Drawing lock release on back-navigation mid-draw: currently only released via the
  in-screen toggle button, not on backing out of the screen while still locked
- [x] Pull-to-refresh on `NotesListScreen` - currently only loads once on screen entry via
  `LaunchedEffect`; no swipe gesture re-triggers `load()`
- [x] Empty-state CTA button on `NotesListScreen` - the empty-state copy ("No notes added
  yet.") already exists, the paired CTA button does not (flagged in the screen's own header
  comment already, just not previously tracked here)
- [x] `isFromCache` indicator - `NotesResult.isFromCache` exists in state but nothing in
  `NotesListScreen` renders a "showing saved notes, couldn't refresh" treatment for it
- [x] Delete-note UI entry point - `NoteRepository.deleteNote()` exists, no screen calls it
  yet (not on `NoteCard`, not on `NoteEditScreen`) - needed for the first testing path below
- [ ] Run `ktlintFormat` across everything touched by this issue before closing it out
- [?] Add project icon ad app icon
- [x] Separate sync and mutation in-flight guards - load()/refresh() share a sync guard, while note deletion uses its own mutation guard.
---
### Medium items
>Items that require some manual work.

- [x] Localization strings - replace any user-facing strings that are manually defined
- [x] `NoteEditUiState.NotFound` copy: currently reads as permanent ("This note couldn't be
  found") but the real cause may just be "hasn't synced down to this device yet." Needs real
  logic (check pending-sync state), not just a copy change.
- [x] Cold-start session restoration: app currently doesn't check for a valid persisted
  session on launch and route straight to the note list - forces a fresh login even when a
  valid session already exists locally. Distinct from `TokenAuthenticator`'s refresh-on-401
  job below - this is "do we even try the saved session," not "what happens when it expires."
- [ ] AFK-triggered autosave: decided as MVP scope (configurable 30s-10min idle threshold,
  autosave-and-exit-to-view) but not yet built - `NoteEditViewModel` currently only saves on
  explicit button tap
- [ ] `AccountOverviewViewModel`'s in-scope pieces: theme toggle, in-app clear-out (tap = local
  wipe). Both were decided in-scope from the start of the note-issue split, neither is built yet.

---
### Large items
> Items that may take chunks of commits for larger missing feature elements

- [x] NoteEditScreen redesign - **NEW [25/08/2026]:** canvas with text lines. 
  Viewmodel UI has 4 modes `Create`, `View`, `TextEdit` and `DrawingEdit` modes.
  Each edit mode allows limited interaction with elements. View [ADR 0004](decisions/0004-NoteEdit-Redesign-Criteria.md)
- [x] NoteCard crystallization - final visual design definition for the card (layout, spacing,
  actual treatment of the `hasHistory`/`hasDrawing` indicators once decided above), not just
  the structural skeleton currently in place
- [ ] Self-host capability stubs - inactive UI groundwork (e.g. a settings field for a custom
  server base URL, HTTP-vs-HTTPS toggle) enabling faster self-host setup later as a secondary,
  non-cloud path. Current app state only supports the cloud-hosted path. Stub only - full
  self-host networking/config work is not this issue's scope, just the UI hooks for it.
- [x] Session navigation: nav-graph gating for unauthenticated/expired-session state. Currently,
  nothing enforces where an unauthenticated user lands - no redirect point exists.
- [x] Session lifecycle: `TokenAuthenticator` (refresh-on-401, single-flight guard, AT
  injection into pending requests). **Confirmed in scope for this issue** - the account
  currently has no real token lifecycle work beyond attaching tokens to authed requests
  (Issue #6), and cold start doesn't use saved tokens at all (see cold-start item above).

---
### Testing
> Definitions of tests still to be made. Feasibility notes are honest, not aspirational -
> single-device paths are straightforward instrumented UI tests; two-device paths need a
> different approach than "run two emulators," noted below.

- [ ] Automatic testing of UX flows.

  **Single device paths** - standard Compose UI instrumented tests, no special tooling needed:

    - [ ] **Path A - full note lifecycle**
        1. Register a fresh test account
        2. Assert Notes list shows `Empty` state
        3. Tap create FAB -> assert Editor opens in create mode
        4. Enter title + body, tap Save -> assert navigation back to list, assert note appears
        5. Tap the note -> assert Editor opens in edit mode with the correct content loaded
        6. Edit title/body, tap Save -> assert the change persisted
        7. Delete the note (**depends on the delete-UI-entry-point item above existing first**)
        8. Delete the account `Note - This requires server path for account deletion to be complete - check corresponding server repo progress.`

    - [ ] **Path B - version restore, text only**
        1. Register account, create a note (title "A"), save -> 1 version exists
        2. Edit title to "B", save -> 2 versions exist, `hasHistory` true
        3. Open version history, select the first version, restore
        4. Assert displayed title reverts to "A"
      <!-- DEPENDS ON: NoteVersionViewModel and its restoreVersion return-type decision,
           both in progress now - this path is a target definition, not runnable yet. -->

    - [ ] **Path C - version restore, drawing to text-only**
        1. Register account, create a text-only note, save
        2. Switch to drawing mode, draw something, save
        3. Open version history, restore the pre-drawing (text-only) version
        4. Assert the note displays as text-only again, drawing content gone
      <!-- DEPENDS ON: Path B's dependencies, PLUS the NoteEditScreen redesign (exclusive
           text/drawing mode toggle) from the Large items above. Blocked on more in-flight work
           than Path B - don't expect this one runnable until both land. -->

  **Two-device paths** - do NOT attempt with two real emulator instances; that's a Firebase
  Test Lab-grade setup, disproportionate for this project. Instead, drive "device 2" via
  direct API calls (same approach as the server's `NoteControllerTestBase.loginSecondDevice`),
  and only UI-assert on device 1's reaction to the resulting sync:

    - [ ] **Path D** - Device 1 (real UI test): create a note, save. Device 2 (direct API call):
      login, `GET /notes`. Assert (via API response, not UI) the note is visible to device 2.
    - [ ] **Path E** - same shape, for edit propagation and drawing-lock conflict (device 2's
      API call attempts to acquire the lock device 1 already holds; assert 409).
      <!-- SCOPE CHECK: if these are meant to test pull-to-refresh/manual-sync (already built),
           in scope now. If "syncing edits" implies WebSocket-nudge-triggered sync, that's
           WebSocket-issue territory and shouldn't block this issue closing. -->

- [ ] Local path testing (can load from local cache, the sync from coming online works)
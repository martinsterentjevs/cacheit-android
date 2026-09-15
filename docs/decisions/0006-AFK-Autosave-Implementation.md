# 0006 — AFK-triggered autosave: Android implementation

**Status:** Accepted
**Date:** 2026-09-15

## Context

The cross-client autosave contract (`cacheit-spec/technical/architecture/decisions/NNNN-afk-autosave.md`)
defines two racing triggers — a user-configurable inactivity timer and a
platform sleep guard — that invoke the existing manual save path. This ADR
records the Android-specific implementation of that contract: how the timer
is scheduled, how the screen-off guard is wired to the edit screen's
lifecycle, and where the duration setting is persisted.

## Decision

**Inactivity timer**

Implemented in `NoteEditViewModel` as a cancellable coroutine job:

- On each edit interaction, cancel any pending timer job and relaunch a new
  one: `delay(inactivityDurationMs)` then invoke the existing save function.
- On manual save, cancel and relaunch the same way — save of any kind resets
  the clock.
- On `ViewModel.onCleared()` (screen disposal / navigating away), cancel the
  job outright — no dangling save should fire against a note that's no
  longer open.

**Screen-off guard**

Registered and unregistered scoped to the edit screen's own lifecycle, not
app-wide:

- Register a `BroadcastReceiver` for `Intent.ACTION_SCREEN_OFF` in the edit
  screen's `onStart` (via `LifecycleEventObserver` or
  `DisposableEffect`/`LifecycleResumeEffect` in the composable — not
  `Application.onCreate`).
- On receipt, cancel the pending inactivity timer job and invoke save
  immediately through the same path.
- Unregister the receiver in the edit screen's `onStop`, so a screen-off
  event while a different note (or no note) is open does not trigger a
  stale save.

Scoping to the edit screen rather than app-wide avoids two failure modes:
firing a save for a note that isn't the one currently open, and leaking a
registered receiver past the screen's lifetime.

**Inactivity duration setting**

- Persisted per-user, following the same storage mechanism as
  `ThemePreference` (DataStore-backed preference).
- Default value: 30s, per the cross-client contract.
- Exposed as a setting on the existing settings screen; no new screen
  required.
- `NoteEditViewModel` reads the current duration from the preference at
  timer-scheduling time (not cached at ViewModel construction), so a
  setting change takes effect on the next scheduled cycle without requiring
  the user to reopen the note.

**Save path**

Both triggers call the same function already used for manual save in
`NoteEditViewModel` — no new persistence or draft-write logic introduced.

## Consequences

**Easier:**
- Edit loss from screen timeout or walking away is eliminated on Android
  without touching the save/persistence layer.
- Reuses the existing `ThemePreference`-style DataStore pattern, so no new
  persistence mechanism for the setting.

**Harder / trade-offs:**
- `NoteEditViewModel` now owns two concurrent lifecycle-sensitive concerns
  (timer job, receiver registration relayed from the composable) instead of
  one — adds surface area to an already-large ViewModel (748 lines,
  previously flagged as a post-MVP extraction candidate).
- Reading the duration setting per-cycle rather than caching means an extra
  DataStore read on every timer reschedule; acceptable given edit
  interactions aren't high-frequency, but worth knowing if profiling ever
  flags it.
- Receiver lifecycle correctness (registered only while the edit screen is
  actually in the foreground) is easy to get subtly wrong — verify with an
  instrumented test that backgrounding a different screen does not trigger
  autosave on a previously-open note.

## Alternatives considered

- **App-wide `ACTION_SCREEN_OFF` registration** (e.g. in an Application-level
  observer) — rejected: would require tracking "is a note currently open and
  which one" as separate state instead of relying on the edit screen's own
  lifecycle, and risks firing saves for notes that aren't the active one.
- **Caching the duration setting at ViewModel construction** — rejected:
  would require an explicit refresh mechanism when the user changes the
  setting mid-session; reading per-cycle is simpler and cheap enough.
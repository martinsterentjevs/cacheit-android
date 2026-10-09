# 0001 - Shared base ViewModel for auth flow submission

**Status:** Accepted
**Date:** 2026-08-16

## Context

Login and Registration both follow the same submit shape: validate input →
run a suspend block (salt fetch → derive → repository call → cache MEK) →
on success emit a one-time navigation event, on failure surface a popup and
reset to idle. Registration's block additionally generates and wraps a MEK
before the repository call, but the surrounding state machine - loading flag,
one-time success event via `Channel`, and error routing to `PopupController` - is identical.

Two options were on the table: duplicate this scaffolding in
`LoginViewModel` and `RegistrationViewModel` independently, or extract it
into a shared base class that both extend.

## Decision

`BaseAuthViewModel` holds the shared `AuthUiState` (`Idle`/`Loading`),
the `Channel<AuthEvent>`-backed one-time success event, and a
`launchAuthFlow(block: suspend () -> Unit)` helper that wraps loading state
and error-to-popup handling around whatever block a subclass provides.
`LoginViewModel` and `RegistrationViewModel` extend it and only supply their
own `submit(...)` with the flow-specific suspend block.

## Consequences

**Easier:**
- Adding a third auth-adjacent screen (e.g. a future "change password"
  flow) reuses the same scaffolding with zero duplication.
- Loading-state and error-popup behaviour stay consistent by construction -
  a bug fix in `launchAuthFlow` fixes it everywhere at once.
- Each concrete ViewModel's `submit()` reads as pure business logic, with no
  state-machine boilerplate mixed in.

**Harder:**
- A future auth screen with a genuinely different submit shape (e.g. one
  needing multi-step progress states instead of a single Loading flag) would
  need to either extend `AuthUiState` carefully or break out of the base
  class - the abstraction has to flex before it snaps.
- One more file to read (`BaseAuthViewModel.kt`) before a new contributor
  fully understands `LoginViewModel`, versus everything being inline in one
  file.

## Alternatives considered

- **Full duplication** - rejected. The shared shape is identical enough
  (only the suspend block differs) that duplicating it would mean any future
  fix to error handling or the event channel needs to be applied twice, with
  the two copies free to drift.
- **Composition (a `AuthFlowRunner` helper class injected into each VM)
  instead of inheritance** - considered, since composition is generally
  preferred over inheritance. Rejected for this specific case because the
  shared state (`uiState`, `events`) needs to be exposed directly as the
  ViewModel's own public API, not wrapped through a delegate - inheritance
  is the more direct fit than composition here without extra indirection.
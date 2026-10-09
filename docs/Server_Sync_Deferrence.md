# Server Sync Deference
Created:12/08/2026 Last updated:12/08/2026
Status: Depricated (relevant work item is complete)

## Owns 
- Elements of the auth system that have changed or evolved under work on this repo
- Scope of the element integration

## Does not own 
- Auth system specifics
- Note system specifics
- Design elements

## Overview
This document exists for the sole purpose of noting any changes in auth system based on the discoveries and decisions reached while working on Issue #3: Auth system setup

## Discoveries — auth system elements affected

Surfaced while implementing the Android crypto core (MEK/MUK wrapper, Argon2id
KDF contexts) — auth-system changes the server side doesn't yet reflect, since
none of these were anticipated at Issue #3 close.

### 1. Login cannot be a single request

The client needs the account's salt *before* it can compute the value it
authenticates with — so login is two round-trips, not one:

1. Client sends identifier only, unauthenticated, to a salt-lookup endpoint
2. Server returns that account's salt (real accounts) or a deterministic fake
   (non-existent accounts) — see §2
3. Client derives MUK + auth hash locally, submits the auth hash as the actual
   login credential

`AccountSessionDto` returning salt post-auth is the wrong shape — too late in
the flow to be useful. Needs a dedicated pre-auth endpoint instead.

### 2. Salt-lookup endpoint needs a user-enumeration mitigation

A naive "salt found → return it, not found → 404" response leaks account
existence. Mitigation: server generates a deterministic fake salt for unknown
identifiers via HMAC-SHA256(identifier), keyed by a new server-only secret
(same handling tier as `JWT_SECRET`) — not Argon2id, which is unkeyed (so
independently reproducible by an attacker, defeating the point) and
deliberately slow (creating a timing side-channel between real/fake paths).

## Required server-side changes (cacheit-server)

- New endpoint: salt lookup by identifier, unauthenticated, generic response
  shape regardless of real/fake path
- `AccountRepository`: lookup method returning nullable (fall through to
  fake-salt path on null)
- New server-only secret for the fake-salt HMAC key (config, not committed)
- Decide + implement rate limiting on the salt-lookup endpoint (open below)
- Confirm response timing parity between real-account (DB read) and
  fake-account (HMAC) paths

## ADRs required — cross-repo, not per-repo

Per the documentation convention — non-obvious, expensive to reverse, likely
to be questioned later. All three constrain Android and Desktop both, so they
belong in `cacheit-spec/technical/architecture/decisions/`, not folded into
any one repo's own `docs/decisions/`:

- Crypto envelope format — AES-256-GCM, version byte + nonce + ciphertext
  layout, AAD structure, rejected alternatives and why
- Two-step login / salt-lookup design — why unauthenticated by design, why
  fake salts over generic errors, why HMAC over Argon2id for the fake path
- `NoteField` AAD identifier list as canonical spec constant — single source
  of truth, so Android and Desktop can't drift independently

## Open questions — resolved

- **Rate limiting on the salt-lookup endpoint:** in scope for this pass,
  time-boxed — implement if time allows within Issue #3 follow-up work,
  document as a deferred known gap if not. Not a blocker for closing the
  crypto/auth chunk either way.
- **Canonical `NoteField` identifier list location:** moves to a new
  `technical/cross-client-reference.md` in `cacheit-spec` (see below) rather
  than living only inside an ADR — ADRs record the decision and its
  rationale; this doc is the thing Desktop actually implements against.

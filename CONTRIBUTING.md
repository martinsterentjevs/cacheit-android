# Contributing to CacheIt Android

Thanks for your interest in CacheIt. This document covers the conventions used in this
repository - branching, commits, and the PR process.

## Branching

`main` and `dev` are the two permanent branches:

- **`main`** - production-ready code only. Protected, requires a PR and review to merge.
- **`dev`** - integration branch. All work merges here first. Protected, requires CI to pass.

All work happens on short-lived branches created from `dev`:

```
feature/<issue-id>-<slug>     e.g. feature/3-note-crud-screens
bugfix/<issue-id>-<slug>      e.g. bugfix/12-fix-sync-refresh-race
chore/<slug>                  e.g. chore/bump-compose-bom
docs/<slug>                   e.g. docs/update-setup-guide
```

`hotfix/` branches are the exception - they branch from `main` and merge to both `main` and `dev`,
used only for urgent production fixes.

Releases are cut on `release/v<MAJOR>_<MINOR>` branches from `dev`, tagged on `main` once merged
(e.g. `release/v1_2` → tag `v1.2.0`).

## Commit messages

Commits follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <short summary>

[optional body]

[optional footer]
```

**Types:**

| Type       | Use for                              |
|------------|--------------------------------------|
| `feat`     | New feature                          |
| `fix`      | Bug fix                              |
| `refactor` | Restructure with no behaviour change |
| `docs`     | Documentation only                   |
| `chore`    | Build, deps, config                  |
| `test`     | Adding or updating tests             |
| `ci`       | CI/CD pipeline changes               |
| `perf`     | Performance improvement              |
| `style`    | Formatting only                      |

**Scope** - use the affected feature area in lowercase-kebab, e.g. `auth`, `notes`, `sync`,
`drawing`, `crypto`:

```
feat(notes): add version history list screen
fix(sync): resolve duplicate manifest entries on reconnect
chore(crypto): bump androidx.security-crypto
```

**Rules:**
- Imperative mood ("add", not "added")
- 72 characters max on the summary line
- One logical change per commit
- Squash fixup commits before opening a PR
- Breaking changes get a `BREAKING CHANGE:` footer, never buried in the body

## Pull requests

Every PR should include:

- **Summary** - what changed and why, one paragraph
- **Type of change** - feat / fix / refactor / chore / breaking
- **Testing** - what was tested, how
- **Related issues** - `Closes #N` if applicable

**Review requirements:**

| Target branch | Review                              | CI                      |
|---------------|-------------------------------------|-------------------------|
| `main`        | 1 approval required                 | Full suite must pass    |
| `dev`         | Self-merge allowed, review welcomed | Build + tests must pass |
| `release/*`   | 1 approval required                 | Full suite must pass    |

## Code style

- **Kotlin/Compose:** enforced via `.editorconfig` and ktlint. Run `./gradlew ktlintFormat` before
  opening a PR; CI runs `ktlintCheck`.
- Compose functions follow standard naming (`PascalCase`, no `get`/`set` prefixes) and should stay
  stateless where practical - hoist state to the caller rather than holding it inside a
  `@Composable` unless the state is purely UI-local (scroll position, animation state, etc.).

## Testing

```bash
# Unit tests (JVM, no device/emulator needed)
./gradlew testDebugUnitTest

# Instrumented / UI tests (requires a connected device or emulator)
./gradlew connectedDebugAndroidTest
```

Add or update unit tests for any new ViewModel, repository, or crypto-layer logic. Add or update
Compose UI tests for any new screen or user-visible interaction. No PR that weakens coverage in the
crypto or sync layers will be merged without an explicit justification in the PR description -
same standard as `crypto/`/`sync/` on the server repo.

## Changelog

Add an entry under `[Unreleased]` in [`CHANGELOG.md`](CHANGELOG.md) for any user-facing change, as
part of the same PR.

## Documentation

If your change affects the crypto contract, the sync protocol, screen navigation structure, or
setup steps, update the relevant file in `docs/` in the same PR - not as a follow-up. Any
crystallized cross-client decision (data formats, protocol shapes) belongs in the spec repo, not
just here - see the Two-tier ADR model in the Git Structure Reference.

## A note on the security model

This is where the zero-trust design becomes concrete on the client side: MUK is derived from the
user's password and MEK is unwrapped here, in memory, before any note content is ever encrypted or
decrypted locally. If your change touches key derivation, key storage (`EncryptedSharedPreferences`
or its successor), the refresh-token store, or anything under the crypto contract, flag it
explicitly in the PR description even if it looks like a minor change - these are the areas where a
subtle bug has outsized consequences, more so here than almost anywhere else in the project, since
this is the one place raw key material exists outside the server's zero-knowledge boundary.****
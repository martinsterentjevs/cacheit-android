# CacheIt Android

Android client for a zero-trust, end-to-end encrypted note-taking application.

## Overview

This repo contains the Jetpack Compose Android client for the CacheIt project.
Encryption and decryption are handled on-device via the Crypto Engine before
any data leaves the device. Platform secrets are managed via Android Keystore
through the CredMan layer. Sync is handled via REST pulls triggered by typed
WebSocket nudge signals from the server.

For a full project overview: [CacheIt](https://martinsterentjevs.github.io/cacheit-spec/)

## Stack

- Kotlin
- Jetpack Compose
- Android Keystore

## Project Structure

- `src/` — application source
- `tests/` — test suite
- `docs/` — architecture decisions

## Quick Start

```bash
git clone https://github.com/martinsterentjevs/cacheit-android.git
cd cacheit-android
# open in Android Studio or IntelliJ IDEA with Android plugin
# requires Android SDK
```

## Links

- [Contributing](CONTRIBUTING.md)
- [Server repo](https://github.com/martinsterentjevs/cacheit-server)
- [Full project documentation](https://github.com/martinsterentjevs/cacheit-spec)

## License

MIT — see [LICENSE](LICENSE)
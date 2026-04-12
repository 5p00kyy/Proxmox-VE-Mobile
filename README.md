# Proxmox VE Mobile

`Proxmox VE Mobile` is an Android client for browsing and operating a Proxmox VE environment from a phone or tablet.

The current codebase is a single-module Kotlin/Jetpack Compose app with direct Retrofit calls to the Proxmox API. It includes login, a dashboard, and management screens for nodes, VMs, containers, storage, network, users, tasks, backups, cluster status, and settings. Some of those surfaces are functional, some are read-only, and several contain unfinished actions or placeholder UI. This README describes the repository as it exists today rather than the intended end state.

## Localization

The app ships string resources for these locales:

- Default (`values`): English
- `values-de`: German
- `values-es`: Spanish

Localization is resource-based through Android `strings.xml` files. New locale additions should preserve string names, placeholders, format specifiers, and escaping so translations remain behaviorally identical to the default resources.

## Current Status

What appears to work from code inspection:

- Manual login against a user-supplied Proxmox host and port.
- Optional encrypted credential storage with auto-fill on next launch.
- Dashboard node listing and basic status display.
- VM list loading plus start, stop, and delete actions.
- Container list loading plus start, stop, and delete actions.
- Storage, network, users, tasks, backups, and cluster screens that fetch and display data.

What is clearly incomplete or only partially implemented:

- Several screens expose settings or action buttons with `TODO` handlers.
- Some settings are local UI state only and are not persisted or wired into runtime behavior.
- Advanced actions such as backup restore/download, user edit/delete UI flows, and some container resource operations are unfinished.
- Networking currently trusts all TLS certificates and hostnames, which is acceptable for local experimentation but not production-grade security.
- There are no committed unit tests or instrumentation tests.

## Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- AndroidX Navigation Compose
- Retrofit with Gson
- OkHttp
- Kotlin Coroutines
- `EncryptedSharedPreferences` for saved credentials

## Architecture

The project is currently lightweight rather than strongly layered:

- `presentation/`: Compose screens, navigation, and `MainViewModel`
- `data/api/`: Retrofit API definitions plus authentication/client setup
- `data/model/`: API models
- `data/security/`: encrypted credential storage

The app does not currently use a robust repository/domain layer. Most screens call `MainViewModel`, which in turn constructs API services directly.

## Repository Layout

```text
.
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/proxmoxmobile/
│       │   ├── data/
│       │   └── presentation/
│       └── res/
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
├── build.gradle.kts
├── gradle.properties
└── settings.gradle.kts
```

## Setup

Prerequisites:

- JDK 17
- Android Studio or a local Android SDK installation

Local setup:

```bash
./gradlew tasks
```

If you want to build locally, create your own `local.properties` or configure the Android SDK through Android Studio. That file is intentionally not tracked.

## Build And Verification

Typical commands in a real Android environment:

```bash
./gradlew assembleDebug
./gradlew test
./gradlew lint
```

This repository was cleaned up on a machine without a configured Android SDK, so Android builds, lint, emulator runs, and instrumentation tests could not be verified here.

## Security Notes

- Credentials can be stored in encrypted shared preferences.
- The current networking layer disables certificate and hostname verification to simplify connections. That is a major hardening gap and should be fixed before treating the app as production-ready.
- `android:usesCleartextTraffic="true"` is enabled in the manifest, so plain HTTP is allowed.

## Known Gaps

- No automated tests are present.
- No CI configuration is present.
- Several dependencies and parts of the UI suggest abandoned or unfinished feature work.
- The settings screen overstates what is actually configurable.

## Recommended Next Steps

1. Verify the app on a real Android SDK/emulator and record which screens/actions actually succeed against a Proxmox instance.
2. Harden networking by removing trust-all TLS behavior and documenting certificate requirements.
3. Add at least a small unit test suite around authentication, model parsing, and view-model behavior.
4. Decide which unfinished screens are in scope, then either complete them or reduce the navigation surface.
5. Introduce a clearer data layer if the app is going to grow beyond direct view-model API calls.

For a concrete staged roadmap, see [`docs/revival-plan.md`](docs/revival-plan.md).

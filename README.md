# Proxmox VE Mobile

`Proxmox VE Mobile` is an Android client for browsing and operating a Proxmox VE environment from a phone or tablet.

The current codebase is a single-module Kotlin/Jetpack Compose app that is moving from screen-owned Retrofit calls toward repository-backed feature slices. It includes login, a dashboard, and management screens for nodes, VMs, containers, storage, network, users, tasks, backups, cluster status, and settings. Some of those surfaces are functional, some are read-only, and several contain unfinished actions or placeholder UI. This README describes the repository as it exists today rather than the intended end state.

## Localization

The app ships string resources for these locales:

- Default (`values`): English
- `values-de`: German
- `values-es`: Spanish

Localization is resource-based through Android `strings.xml` files. New locale additions should preserve string names, placeholders, format specifiers, and escaping so translations remain behaviorally identical to the default resources.

## Current Status

What appears to work from code inspection:

- Manual login against a user-supplied Proxmox host and port.
- Password or API token login against a user-supplied Proxmox host and port.
- Optional encrypted credential storage with auto-fill on next launch.
- Optional SHA-256 certificate fingerprint pinning for self-signed HTTPS servers.
- Dashboard node listing, basic status display, task activity summary, polling, and manual refresh through a repository-backed feature ViewModel.
- Node detail status loading through a repository-backed feature ViewModel, reachable from dashboard node cards with node identity preserved.
- VM list/detail loading plus start, graceful shutdown, force stop, reboot, delete, returned task notices, direct task-detail links, VMID-filtered task history, and read-only config/snapshot visibility through repository-backed feature ViewModels.
- Container list loading plus start, graceful shutdown, force stop, reboot, delete, returned task notices, direct task-detail links, and VMID-filtered task history through a repository-backed feature ViewModel.
- Container detail status and snapshot visibility through a repository-backed feature ViewModel, reachable from the LXC list with node identity preserved.
- Task history loading through a repository-backed feature ViewModel, with node selection, status/type/VMID filters, detail/status, task log viewing, and running-task stop requests.
- Storage listing and read-only storage content browsing through a repository-backed feature ViewModel with loading, error, refresh, and empty states.
- Network interface listing through a repository-backed feature ViewModel with loading, error, retry, refresh, and cached-node selection.
- User listing through a repository-backed feature ViewModel with loading, error, retry, refresh, and empty states.
- Backup listing through a repository-backed feature ViewModel with node selection, storage filtering, partial-storage warnings, retry, refresh, and empty states.
- Cluster status through a repository-backed read-only screen with quorum, vote, and node membership state.

What is clearly incomplete or only partially implemented:

- Some screens remain placeholders or read-only while planned workflows are built deliberately.
- Some settings are local UI state only and are not persisted or wired into runtime behavior.
- Advanced actions such as node reboot/shutdown/shell, backup create/restore/download/delete, user edit/delete UI flows, VM console/configuration editing, and container detail resource/console operations are unfinished.
- Unfinished backup, user, settings, and LXC-detail controls are disabled or marked as planned rather than presented as working.
- The explicit insecure TLS fallback is debug-only; SHA-256 certificate fingerprint pinning is available for self-signed servers and should be preferred.
- Unit test coverage exists for session/auth, dashboard, node, VM, LXC, task, network, storage, user, backup, security, and localization seams, but broader unit, instrumentation, and screenshot test coverage is still missing.

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
- `data/security/`: encrypted credential storage plus credential-store contracts
- `data/session/`: current authentication session ownership and session service contract
- `presentation/auth/`: authentication UI state/session controller
- `data/dashboard/`, `data/node/`, `data/vm/`, `data/lxc/`, `data/task/`, `data/network/`, `data/storage/`, `data/user/`, `data/backup/`, `data/cluster/`: first repository-backed vertical slices

The app is moving toward repository-backed feature slices. Dashboard, node detail, VM list/detail, LXC list/detail, tasks, network, storage, users, backups, and cluster status now use dedicated repositories and feature ViewModels. VM detail now includes read-only configuration and snapshot data through the repository path. Authentication state/API-service access now sits behind `AuthSessionController`, saved login data sits behind `CredentialStore`, and `MainViewModel` still owns cached-node and global dialog concerns. Node detail now preserves node identity when opening VM, LXC, storage, network, and task views.

## Repository Layout

```text
.
|-- app/
|   |-- build.gradle.kts
|   `-- src/main/
|       |-- AndroidManifest.xml
|       |-- java/com/proxmoxmobile/
|       |   |-- data/
|       |   `-- presentation/
|       `-- res/
|-- gradle/
|   |-- libs.versions.toml
|   `-- wrapper/
|-- build.gradle.kts
|-- gradle.properties
`-- settings.gradle.kts
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

The 2026-06-06 audit verified `test`, `lint`, and `assembleDebug` locally with JDK 17 and an Android SDK configured through environment variables. Unit coverage currently starts with the session/auth, node, VM, LXC, task, and localization seams; broader behavioral coverage is still needed.

## Project Workflow

- Roadmap and recovery plan: [`docs/development-cycle-2026.md`](docs/development-cycle-2026.md)
- Current audit and feature status: [`docs/project-audit-2026-06-06.md`](docs/project-audit-2026-06-06.md)
- Contributor guide: [`CONTRIBUTING.md`](CONTRIBUTING.md)
- Security policy: [`SECURITY.md`](SECURITY.md)
- Changelog: [`CHANGELOG.md`](CHANGELOG.md)

Pull requests are expected to run `./gradlew test`, `./gradlew lint`, and `./gradlew assembleDebug`. The GitHub Actions workflow in `.github/workflows/android.yml` enforces the same baseline for pull requests and pushes to `main`.

## Security Notes

- Password credentials and API token pieces can be stored in encrypted shared preferences.
- HTTPS uses platform TLS validation by default.
- Self-signed HTTPS servers can use an Android-trusted imported CA or a pinned SHA-256 certificate fingerprint.
- SSL verification can be disabled only in debug builds for trusted lab servers. Release builds require platform TLS validation or a configured SHA-256 certificate fingerprint.
- Release builds no longer request app-wide cleartext traffic. Debug builds still allow HTTP for local testing.
- See [`SECURITY.md`](SECURITY.md) for the recommended `openssl` command and trust-on-first-use policy notes.

## Known Gaps

- Automated test coverage is narrow and currently focused on session/auth, dashboard, node, VM, LXC, task, network, storage, user, backup, security, and localization seams.
- CI is present for build, lint, and unit tests.
- Several dependencies and parts of the UI suggest abandoned or unfinished feature work.
- The settings screen overstates what is actually configurable.

## Recommended Next Steps

1. Verify the app on a real Android SDK/emulator and record which screens/actions actually succeed against a Proxmox instance.
2. Validate the self-signed certificate guidance against real Proxmox appliances and reverse proxies.
3. Expand unit tests around authentication, model parsing, and view-model behavior.
4. Decide which unfinished screens are in scope, then either complete them or reduce the navigation surface.
5. Continue moving detail screens and remaining app-level actions into repositories and feature ViewModels.

For a concrete staged roadmap, see [`docs/revival-plan.md`](docs/revival-plan.md). For the broader product, refactor, QA, workflow, and community development cycle, see [`docs/development-cycle-2026.md`](docs/development-cycle-2026.md).

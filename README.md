# Proxmox VE Mobile

`Proxmox VE Mobile` is an Android client for browsing and operating a Proxmox VE environment from a phone or tablet.

The current beta line is focused on common mobile operator workflows: login, secure connection handling, dashboard visibility, node navigation, VM/LXC lifecycle actions, task follow-up, and read-only infrastructure inspection. It is a single-module Kotlin/Jetpack Compose app that is moving from screen-owned Retrofit calls toward repository-backed feature slices. This README describes the repository as it exists today rather than the intended end state.

## Beta Release

The planned first official beta is `v0.1.0-beta.1`.

Distribution plan:

- GitHub Releases will be the first beta download path.
- The beta APK will be attached to the release once automated gates and real Proxmox smoke QA pass.
- Play Store and F-Droid packaging are deferred until after the first GitHub beta.

Supported environment targets for the first beta:

- Android API 24 or newer.
- Proxmox VE 8.x is the primary smoke-test target.
- Standalone nodes and small clusters are in scope.
- Proxmox Backup Server management is not in scope for the first beta.

### Installing A Beta APK

When `v0.1.0-beta.1` is published:

1. Open the GitHub Release for the beta tag.
2. Download the attached `proxmox-ve-mobile-v0.1.0-beta.1-release.apk`.
3. Confirm the release notes identify the APK as a signed beta build.
4. On Android, allow installation from the browser or file manager you used to download the APK.
5. Open the APK and follow the Android install prompt.

Only install APKs attached to this repository's GitHub Releases. Debug APKs and unsigned workflow artifacts are for project validation only and should not be treated as public beta downloads.

## Screenshots

Screenshots and short screen recordings should be added before tagging `v0.1.0-beta.1`. At minimum, capture:

- Login with the TLS/fingerprint controls visible, using non-sensitive sample host text.
- Dashboard with generic or disposable lab resources only.
- VM list/detail and LXC list/detail with disposable guest names.
- Task detail/logs with UPIDs, usernames, node names, IPs, and sensitive log values redacted or captured from a throwaway lab.
- One read-only admin slice such as storage, network, users, backups, or cluster, using sanitized sample data.

Do not publish screenshots that reveal private hostnames, public or private IP addresses, usernames, backup notes, task logs with environment details, API tokens, tickets, cookies, fingerprints tied to a private server, or desktop/user-specific paths. If sanitized real screenshots are not available, use a disposable Proxmox lab or omit screenshots until public-safe media can be captured.

Safe examples include placeholder host text, generic node names such as `pve-demo`, disposable guest names such as `demo-vm-101`, redacted UPIDs, and storage or cluster views from a throwaway lab.

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

## Known Limitations

These limitations are expected for `v0.1.0-beta.1` and should be reflected in release notes:

- Guest console access is not implemented.
- VM and LXC configuration editing is not implemented.
- Snapshot create, delete, and rollback are not implemented.
- Backup create, restore, download, and delete are not implemented.
- User create, edit, and delete flows are not implemented.
- Node reboot, shutdown, shell, and advanced service actions are not implemented.
- Storage, network, user, backup, and cluster areas are primarily read-only.
- TFA-specific password login handling is not complete; API token login is the preferred beta workaround where appropriate.
- Real Proxmox smoke QA is required before the beta is tagged.

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
./gradlew assembleRelease
./gradlew test
./gradlew lint
```

For beta release candidates, run the consolidated gate:

```bash
./scripts/beta-gate.sh v0.1.0-beta.1
```

The 2026-06-06 audit verified `test`, `lint`, and `assembleDebug` locally with JDK 17 and an Android SDK configured through environment variables. Unit coverage currently starts with the session/auth, node, VM, LXC, task, and localization seams; broader behavioral coverage is still needed.

## Project Workflow

- Roadmap and recovery plan: [`docs/development-cycle-2026.md`](docs/development-cycle-2026.md)
- Beta release plan: [`docs/beta-release-plan.md`](docs/beta-release-plan.md)
- Current audit and feature status: [`docs/project-audit-2026-06-06.md`](docs/project-audit-2026-06-06.md)
- Contributor guide: [`CONTRIBUTING.md`](CONTRIBUTING.md)
- Security policy: [`SECURITY.md`](SECURITY.md)
- Changelog: [`CHANGELOG.md`](CHANGELOG.md)

Pull requests are expected to run `./gradlew test`, `./gradlew lint`, `./gradlew assembleDebug`, and `./gradlew assembleRelease`. The GitHub Actions workflow in `.github/workflows/android.yml` enforces the same baseline for pull requests and pushes to `main`.

## Security Notes

- Password credentials and API token pieces can be stored in encrypted shared preferences.
- HTTPS uses platform TLS validation by default.
- Self-signed HTTPS servers can use an Android-trusted imported CA or a pinned SHA-256 certificate fingerprint.
- SSL verification can be disabled only in debug builds for trusted lab servers. Release builds require platform TLS validation or a configured SHA-256 certificate fingerprint.
- Release builds no longer request app-wide cleartext traffic. Debug builds still allow HTTP for local testing.
- See [`SECURITY.md`](SECURITY.md) for the recommended `openssl` command and trust-on-first-use policy notes.

## Privacy Notes

- The app does not include telemetry or analytics.
- Saved host details, usernames, passwords, API token IDs, and API token secrets are stored locally through encrypted shared preferences when credential saving is enabled.
- The app does not intentionally send credentials anywhere except the configured Proxmox VE endpoint.
- Bug reports should remove passwords, API tokens, tickets, cookies, CSRF tokens, private keys, public IP details that should not be shared, and sensitive hostnames.

## Known Gaps

- Automated test coverage is narrow and currently focused on session/auth, dashboard, node, VM, LXC, task, network, storage, user, backup, security, and localization seams.
- CI is present for build, lint, and unit tests.
- Several dependencies and parts of the UI suggest abandoned or unfinished feature work.
- Some settings remain planned or disabled until their runtime behavior is implemented.

## Recommended Next Steps

1. Verify the app on a real Android SDK/emulator and record which screens/actions actually succeed against a Proxmox instance.
2. Validate the self-signed certificate guidance against real Proxmox appliances and reverse proxies.
3. Execute the `v0.1.0-beta.1` smoke matrix and fix only beta blockers before tagging.
4. Promote the changelog from `Unreleased` to `v0.1.0-beta.1` once QA evidence is collected.
5. Continue moving deferred parity work into focused post-beta release trains.

For a concrete staged roadmap, see [`docs/revival-plan.md`](docs/revival-plan.md). For the broader product, refactor, QA, workflow, and community development cycle, see [`docs/development-cycle-2026.md`](docs/development-cycle-2026.md). For the first official beta checklist, see [`docs/beta-release-plan.md`](docs/beta-release-plan.md).

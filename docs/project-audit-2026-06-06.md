# Proxmox VE Mobile Project Audit

Date: 2026-06-06

This audit covers the local Android project after the Spanish localization addition. It combines code inspection, Gradle QA, localization checks, and a comparison against the current official Proxmox VE web UI feature surface.

## Direction

The project was aiming at a native Android companion for the Proxmox VE web UI: login, node overview, VM/LXC operations, storage, network, users, tasks, backups, cluster status, and settings. That direction is still valid, but the codebase is currently wider than it is deep. Several screens look like real product surface while only loading partial data or exposing placeholder actions.

The right next move is not a full rewrite. The right move is to turn one vertical slice into a trustworthy pattern, then repeat it.

Recommended first serious scope:

1. Login and session handling.
2. Cluster/node dashboard.
3. VM list with safe lifecycle actions and task feedback.
4. LXC list with safe lifecycle actions and task feedback.
5. Task history/log visibility.
6. Minimal settings that only expose real behavior.

Everything else should be hidden, clearly marked read-only, or finished later.

## QA Results

Commands run:

```bash
JAVA_HOME=<java-17-home> ANDROID_HOME=<android-sdk> ANDROID_SDK_ROOT=<android-sdk> GRADLE_USER_HOME=<gradle-cache> ./gradlew test
JAVA_HOME=<java-17-home> ANDROID_HOME=<android-sdk> ANDROID_SDK_ROOT=<android-sdk> GRADLE_USER_HOME=<gradle-cache> ./gradlew assembleDebug
JAVA_HOME=<java-17-home> ANDROID_HOME=<android-sdk> ANDROID_SDK_ROOT=<android-sdk> GRADLE_USER_HOME=<gradle-cache> ./gradlew lint
```

Results:

- `test`: passed and now includes focused dashboard, VM, LXC, task, network, storage, user, backup, security, and localization consistency unit tests. Coverage is still narrow.
- `assembleDebug`: passed and produced a debug APK.
- `lint`: initially failed on suspicious indentation in `DashboardScreen`; fixed during this audit. It still reports warnings around insecure networking, stale dependencies, plurals, unused resources, and launcher icon metadata.
- Localization keys: English, German, and Spanish string resources have matching key sets and matching placeholder formats.

Environment notes:

- The default shell Java points to OpenJDK 8 without `javac`.
- Gradle works when run with `JAVA_HOME=/usr/lib/jvm/java-17-openjdk`.
- There is no `local.properties`; Android SDK paths were supplied through environment variables.

## Fixes Applied

- Fixed the lint-blocking suspicious indentation in `DashboardScreen`.
- Rendered the existing global confirmation dialog state in `ProxmoxNavHost`, so VM/LXC delete confirmation flows can actually appear.
- Added localized generic confirmation strings for English, German, and Spanish.
- Wired VM/LXC card action progress into the existing `actionInProgress` state so start/shutdown/force-stop/reboot/delete controls do not stay live during an in-flight action and only the active operation shows progress.

## Current Feature Matrix

| Area | Current state | Notes |
| --- | --- | --- |
| Login | Partially functional | Password and API token login are implemented. Saved credentials auto-fill, but do not auto-authenticate. |
| TLS/security | Improved, not complete | Platform TLS validation is default, `verifySsl` is honored, self-signed servers can use Android trusted/imported CAs or a pinned SHA-256 certificate fingerprint, release builds reject trust-all TLS, and release builds no longer request app-wide cleartext. |
| Dashboard | Improved read-only slice | Loads nodes through `DashboardRepository` + `DashboardViewModel`, caches valid nodes, refreshes every 30 seconds, supports manual refresh, shows task activity summary with task-summary failures isolated from node loading, and links node cards to node detail. Some quick actions still default to the first cached node. |
| Node detail | Improved read-only slice | Loads `/nodes/{node}/status` through `NodeRepository` + `NodeDetailViewModel`, displays version/load/resource status, and links to node-scoped VM, LXC, storage, network, and task views. Node shell/reboot/shutdown/update workflows are not implemented yet. |
| VM list | Improved vertical slice | Loads QEMU guests through `VmRepository` + `VmListViewModel`; start/shutdown/force-stop/reboot/delete are wired, shutdown/force-stop/reboot/delete use contextual confirmations, actions surface returned task IDs, link to task detail/logs, expose VMID-filtered task history, and link to node-aware detail routes. Console, config editing, snapshot mutation, migration, and clone are not implemented yet. |
| VM detail | Improved read-only slice | Loads QEMU status, configuration, and snapshots through `VmRepository` + `VmDetailViewModel`, is reachable from VM cards with node identity preserved, and falls back across cached nodes for old VMID-only routes. Console, config editing, and snapshot create/rollback/delete are disabled until implemented. |
| LXC list | Improved vertical slice | Loads containers through `LxcRepository` + `LxcListViewModel`; start/shutdown/force-stop/reboot/delete are wired, shutdown/force-stop/reboot/delete use contextual confirmations, actions surface returned task IDs, link to task detail/logs, and expose VMID-filtered task history. The old unused screen-level metrics collector has been removed. Detail screen still has unfinished controls. |
| LXC detail | Improved read-only slice | Loads container status and snapshots through `LxcRepository` + `LxcDetailViewModel`, is reachable from LXC cards with node identity preserved, and falls back across cached nodes for old VMID-only routes. Resource edit, start/stop, console, and snapshot create/rollback/delete controls are disabled until implemented. |
| Storage | Improved read-only slice | Lists node storages and browses selected storage content through `StorageRepository` + `StorageViewModel`, with loading, error, retry, refresh, empty, and content states. Upload/download/delete, permissions, edit, and backup restore paths are not implemented yet. |
| Network | Improved read-only slice | Lists interfaces through `NetworkRepository` + `NetworkViewModel`, supports cached-node selection, node-detail preselection, refresh, real retry, and loading/error/empty states. No network configuration edits yet. |
| Users | Improved read-only slice | Lists access users through `UserRepository` + `UserManagementViewModel`, with loading, error, retry, refresh, empty, and content states. Create/edit/delete controls are disabled until implemented. |
| Tasks | Improved vertical slice | Loads tasks through `TaskRepository` + feature ViewModels, supports node selection, status/type/VMID filters, task detail/status, task logs, running-task stop requests using UPIDs, direct links from VM/LXC action notices, dashboard task activity, and VM/LXC card entry points into filtered task history. |
| Backups | Improved read-only slice | Loads backups through `BackupRepository` + `BackupListViewModel`, supports cached-node selection, backup storage filtering, real retry/refresh, partial-storage warnings, and loading/error/empty/content states. Create/download/restore/delete controls are disabled until implemented. |
| Cluster | Improved read-only slice | Loads `/cluster/status` through `ClusterRepository` + `ClusterViewModel`, displays quorum/votes/node membership, and handles loading/error/retry states. Cluster logs, HA, replication, and mutation workflows are not implemented yet. |
| Server list | Placeholder | Screen says coming soon. |
| Settings | Honest minimal settings | Clear credentials and logout work. Dark mode, notifications, biometric, auto-login, refresh interval, and bug report controls are disabled until runtime behavior is implemented. |
| Localization | Structurally good | Three locales have matching keys/placeholders, but many runtime error messages remain hardcoded in Kotlin. |
| Tests/CI | Partially improved | CI workflow exists for build/lint/test. Session/auth, auth UI-state controller, dashboard/node/VM/LXC/task/network/storage/user/backup/cluster/security repository tests, network ViewModel tests, and localization consistency tests are committed. Instrumentation and screenshot tests are still missing. |

## High-Risk Findings

1. TLS and cleartext policy still need production hardening.
   - `ProxmoxApiFactory` now uses platform TLS validation by default.
   - Self-signed Proxmox servers can now be pinned by SHA-256 certificate fingerprint.
   - Insecure certificate acceptance only occurs in debug builds when `verifySsl` is explicitly disabled.
   - Release builds no longer request app-wide cleartext traffic, while debug builds still allow local HTTP testing.
   - Imported CA guidance is documented in `SECURITY.md`; full trust-on-first-use remains a future feature because it needs explicit confirmation and certificate-change warnings.

2. API client and session ownership are only partially centralized.
   - `SessionManager` now owns login state and `ProxmoxApiFactory` owns Retrofit/OkHttp construction.
   - Authentication UI state/API-service access now delegates through `AuthSessionController`; saved credentials now use a `CredentialStore` seam.
   - Dashboard, node detail, VM list/detail, LXC list/detail, task, network, storage, user, backup, and cluster screens now use repositories and feature ViewModels.
   - Unused direct VM/LXC/user/backup action helpers have been removed from `MainViewModel`.
   - Auth UI form state, cached nodes, UI dialog state, and some resource actions still need clearer ownership boundaries.

3. UI state is more honest, but several planned workflows still need implementation.
   - Settings now disable toggles that are not persisted or applied.
   - Backups and users now disable create/edit/delete style controls until implemented.
   - LXC detail now disables resource and console controls until implemented.

4. The data models are too strict for Proxmox API variability.
   - Many fields are non-null even though screen code defensively treats responses as nullable or optional.
   - Gson plus Kotlin non-null fields can produce runtime null surprises when API payloads omit fields.

5. The app depends on cached first-node behavior.
   - Dashboard node cards now carry node identity into node detail.
   - Network now exposes cached-node selection, but its initial route still defaults to the first cached node.
   - Backups now expose cached-node selection, but its initial route still defaults to the first cached node.
   - Old VMID-only guest detail routes still search nodes opportunistically for backward compatibility.

6. There is no safety net for refactoring.
   - Compilation, lint, focused dashboard/VM/LXC/task/network/storage/user/backup/security repository tests, and localization consistency tests pass.
   - CI enforces build, lint, and unit tests, but instrumentation tests, screenshot tests, and dependency drift checks are still missing.

## Feature Parity Target

Official Proxmox VE documentation describes the web UI as a cluster-wide management interface with a resource tree, task/log panel, built-in HTML5 console, role-based permissions, multiple auth sources and TFA, VM/container management, storage, pools, firewall, backup/restore, HA, replication, ACME, notifications, metric servers, and support/subscription panels.

For this mobile app, full parity should be interpreted as role-aware operational coverage, not a one-to-one clone of every desktop panel.

Mobile parity tiers:

1. Core operations:
   - Login/session, cluster/node health, VM/LXC status, start/shutdown/stop/reboot, task progress, task logs, safe confirmations.

2. Operator workflows:
   - Console access, backup/create/restore, snapshot create/rollback/delete, migration, clone, config/resource edits, storage upload/download/delete/protect, node reboot/shutdown, update status.

3. Admin workflows:
   - Users/groups/tokens/permissions, roles, authentication realms, TFA, storage config, network config, firewall, notifications.

4. Advanced cluster workflows:
   - HA, replication, Ceph, SDN, ACME, pools, metric servers, support/subscription, bulk actions.

## Refactor Plan

### Phase 0: Make The App Honest

- Hide or clearly mark placeholder actions.
- Remove settings that do not affect behavior.
- Add a feature matrix in README or docs and keep it current.
- Keep build and lint runnable with documented environment variables.

### Phase 1: Create A Real Core Slice

Build a clean vertical slice around VM list and lifecycle actions:

- `SessionManager`: owns server config, auth token, CSRF token, login/logout, saved credentials.
- `ProxmoxApiFactory`: one place for OkHttp/Retrofit, TLS policy, headers, and logging.
- `VmRepository`: wraps QEMU endpoints and returns typed results.
- `VmListViewModel`: owns VM list state, refresh, action state, errors, and task IDs.
- `VmListScreen`: UI only, no direct Retrofit calls.

Once that pattern works, apply it to LXC.

### Phase 2: Harden Networking

- Default to platform TLS validation.
- Add an explicit insecure compatibility mode only if needed for homelab self-signed certificates.
- Make certificate behavior visible and intentional in settings/login.
- Remove body-level HTTP logging from production builds.
- Revisit cleartext traffic and backup rules.

### Phase 3: API Model Cleanup

- Replace broad raw maps and overly strict data classes with DTOs that match real Proxmox responses.
- Keep DTOs separate from UI models.
- Add parser tests for representative node, VM, LXC, storage, task, user, and backup payloads.
- Prefer API token support for long-lived mobile sessions.

### Phase 4: UX/Product Shape

- Add a resource tree or equivalent mobile navigation model: cluster, nodes, guests, storage.
- Carry node identity in routes instead of guessing from cached nodes.
- Add task progress/logs as a first-class bottom panel or activity screen.
- Add destructive confirmations locally or through a reusable dialog host.
- Make every action produce a task ID and track it until completion where Proxmox returns one.

### Phase 5: Tests And CI

Minimum baseline:

- Unit tests for auth/session, API factory policy, repositories, and feature view-models.
- XML localization consistency test.
- Compose screenshot tests for login, dashboard, VM list, and LXC list.
- GitHub Actions or equivalent for `test`, `lint`, and `assembleDebug`.

## Immediate Backlog

1. Fix remaining lint warnings that represent product risk: networking security, stale Android/Compose dependencies, and plural resources.
2. Validate the imported CA and SHA-256 fingerprint guidance against real Proxmox appliances and reverse proxies.
3. Continue adding unit tests around feature ViewModel behavior and auth UI state.
4. Move remaining session, credential, cached-node, and global dialog ownership out of `MainViewModel` where practical.
5. Design and implement the planned backup, user, settings, and LXC-detail workflows deliberately.

## Official References

- Proxmox VE documentation index: https://pve.proxmox.com/pve-docs/
- Proxmox VE graphical user interface: https://pve.proxmox.com/pve-docs/chapter-pve-gui.html
- Proxmox VE API overview: https://pve.proxmox.com/wiki/Proxmox_VE_API

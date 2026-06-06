# Changelog

All notable changes to this project will be documented in this file.

The project follows the spirit of Keep a Changelog and will use Semantic Versioning once public releases are tagged.

## [Unreleased]

### Beta Release Candidate

- `v0.1.0-beta.1` is the planned first official beta tag.
- This beta is intended for public testing of login, TLS handling, dashboard, node navigation, VM/LXC lifecycle actions, task follow-up, and read-only infrastructure slices.
- Known limitations for the beta include no guest console, no VM/LXC configuration editing, no snapshot mutation, no backup create/restore/download/delete, no user mutation flows, no node power actions, and no Proxmox Backup Server management.
- Beta APK distribution is planned through GitHub Releases first. Play Store and F-Droid packaging are deferred.
- Real Proxmox smoke QA is still required before tagging.
- Before tagging, this section must be promoted to `## [v0.1.0-beta.1] - YYYY-MM-DD`, the smoke status must be updated from pending to verified or known limitation, and the release notes must reference only sanitized screenshots or media.

### Added

- Project audit covering current feature state, QA results, risks, and feature parity direction.
- Development cycle plan for product direction, refactor phases, QA workflow, community process, and staged delivery.
- Beta release plan for `v0.1.0-beta.1` scope, gates, smoke QA, packaging, and timeline.
- Android CI workflow for pull requests and pushes to `main`.
- GitHub issue templates for bug reports and feature requests.
- Pull request template with QA, risk, and contributor checklists.
- Beta APK release workflow for `v*-beta.*` tag pushes and manual dry runs.
- Consolidated beta gate script for version matching, public hygiene checks, tests, lint, debug build, and release build.
- Navigation route registry tests for registered beta destinations, planned detail helpers, and duplicate route patterns.
- Beta-safe Settings About copy that avoids implying full administrative coverage.
- VM/LXC lifecycle ViewModel coverage for shutdown, force stop, reboot, blank task IDs, failed action cleanup, and task-detail route handoff.
- VM/LXC lifecycle coverage for duplicate action suppression, delete guard status handling, stale task handoff cleanup, and valid Proxmox UPID task IDs.
- Emulator smoke coverage for dashboard, node detail, VM list/detail, and LXC list routing on a Pixel-class Android emulator.
- GitHub Actions workflows opt into Node 24 action execution ahead of the hosted runner default change.
- README beta APK install instructions for the first GitHub Releases distribution path.
- Public-safe screenshot/media checklist and release-note readiness guidance for the first beta.
- Public release media manifest and GitHub Release note draft template for `v0.1.0-beta.1` preparation.
- Reusable beta QA status script for summarizing pending, passed, failed, and blocked smoke evidence.
- Dashboard quick actions for Backups and Cluster so read-only beta routes are discoverable during smoke QA.
- Contributing and security policy documentation.
- Self-signed TLS guidance covering Android trusted/imported CAs, SHA-256 certificate fingerprints, and trust-on-first-use requirements.
- CODEOWNERS file for default review ownership.
- Centralized `ProxmoxApiFactory` for Retrofit/OkHttp creation, auth headers, logging, and TLS policy.
- `ProxmoxApiServiceFactory` and `ProxmoxAuthenticationService` seams for session/auth unit testing.
- `SessionManager` for password/API-token authentication state and API service creation.
- API token login controls with encrypted token ID/secret storage.
- Explicit SSL verification toggle for trusted self-signed lab servers.
- Optional SHA-256 certificate fingerprint pinning for self-signed Proxmox servers.
- VM repository and API seam for QEMU guest list and lifecycle actions.
- VM list feature ViewModel for refresh, loading, error, action, and task notice state.
- VM graceful shutdown lifecycle action with task notice handoff and list refresh.
- VM reboot lifecycle action with task notice handoff and list refresh.
- Unit tests for VM repository filtering, task ID mapping, and missing-session errors.
- VM detail lookup through `VmRepository` + `VmDetailViewModel`, with preferred-node lookup and cached-node fallback.
- Node-aware VM detail routes plus a visible Details action on VM cards.
- LXC repository and API seam for container list and lifecycle actions.
- LXC list feature ViewModel for refresh, loading, error, action, and task notice state.
- LXC graceful shutdown lifecycle action with task notice handoff and list refresh.
- LXC reboot lifecycle action with task notice handoff and list refresh.
- Unit tests for LXC repository filtering, sorting, task ID mapping, and missing-session errors.
- LXC detail lookup through `LxcRepository` + `LxcDetailViewModel`, with preferred-node lookup and cached-node fallback.
- Node-aware LXC detail routes plus a visible Details action on container cards.
- LXC snapshot loading through `LxcRepository` + `LxcDetailViewModel`, shown read-only on container detail.
- VM snapshot loading through `VmRepository` + `VmDetailViewModel`, shown read-only on VM detail.
- VM config loading through `VmRepository` + `VmDetailViewModel`, shown read-only on VM detail with sensitive values redacted.
- Task repository and API seam for task history, task status, task logs, and running-task stop requests.
- Task list/detail feature ViewModels for node selection, refresh, errors, task log loading, and stop state.
- Network repository and feature ViewModel for node-scoped interface loading, refresh, retry, and cached-node selection.
- Storage repository and feature ViewModel for node-scoped storage loading, read-only content browsing, refresh, retry, and error state.
- User repository and feature ViewModel for access user loading, refresh, retry, and error state.
- Backup repository and feature ViewModel for node-scoped backup storage discovery, storage filtering, refresh, retry, and partial-storage warnings.
- Dashboard repository and feature ViewModel for node loading, cached node hydration, task activity summary, polling, and manual refresh.
- Dashboard task activity indicator for running/recent task visibility across cached nodes.
- Node repository, node detail ViewModel, and read-only node detail screen backed by `/nodes/{node}/status`.
- Cluster repository, cluster status ViewModel, and read-only cluster screen backed by `/cluster/status`.
- Node-aware node detail, node-filtered task routes, and node-scoped network routing.
- `AuthSessionController` and `AuthSessionService` seams for authentication UI state and API service access.
- `CredentialStore` seam plus neutral credential auth method constants for saved login data.
- Task center filters for status, task type, and VMID, passed through to Proxmox task query parameters.
- Unit tests for task repository filtering, UPID handling, task log ordering, stop requests, and missing-session errors.
- Unit tests for network and storage repository filtering, sorting, storage-content filtering, blank-node validation, and missing-session errors.
- Unit tests for network ViewModel node preselection and cached-node normalization.
- Unit tests for user repository filtering, sorting, and missing-session errors.
- Unit tests for backup repository storage discovery, backup filtering, partial storage failures, blank-node validation, and missing-session errors.
- Unit tests for dashboard repository node filtering, task-summary handoff, task-summary error isolation, empty-node behavior, and missing-session errors.
- Unit tests for node detail status loading, blank-node validation, invalid payload handling, and missing-session errors.
- Unit tests for node detail normalization of Proxmox node status endpoint identity, status, CPU, and memory fields.
- Unit tests for cluster status mapping, standalone-node handling, and missing-session errors.
- Unit tests for VM snapshot filtering, sorting, validation errors, and missing-session errors.
- Unit tests for LXC snapshot filtering, sorting, validation errors, and missing-session errors.
- Unit tests for VM config sorting, sensitive-value redaction, validation errors, and missing-session errors.
- Unit tests for VM/LXC shutdown and reboot action API value mapping.
- Unit tests for VM/LXC lifecycle duplicate action suppression, delete guard status edges, stale task notice clearing, and task ID normalization.
- Unit tests for `SessionManager` password sessions, API-token sessions, secret stripping, API service creation, validation failures, and logout.
- Unit tests for auth session UI state success, failure cleanup, API-service access, and logout reset.
- Unit tests for VM detail preferred-node lookup, cached-node fallback, not-found handling, missing-node validation, and missing-session errors.
- Unit tests for LXC detail preferred-node lookup, cached-node fallback, not-found handling, missing-node validation, and missing-session errors.
- Unit test coverage for TLS authentication failure messaging.
- Unit test that keeps English, German, and Spanish string keys and format specifiers aligned.

### Fixed

- Lint-blocking dashboard indentation issue.
- Global confirmation dialog rendering for destructive VM/LXC actions.
- LXC action progress state during lifecycle/delete operations.
- VM/LXC lifecycle cards now show progress for the specific action in flight instead of every action on the card.
- VM/LXC shutdown, force stop, and reboot actions now use contextual confirmation dialogs before sending disruptive lifecycle requests.
- VM/LXC cards now distinguish graceful shutdown from force stop.
- VM/LXC delete controls are now disabled until the guest is stopped.
- VM/LXC compact lifecycle action labels no longer wrap or truncate on Pixel-sized screens.
- Task statistics and task details now treat Proxmox `OK` task results as successful finished tasks.
- Node, storage, user, and task detail routes now encode path segments consistently for names and IDs containing reserved characters.
- Storage parsing now accepts Proxmox storage `content` as either a comma-separated string or an array, plus numeric storage flags.
- Storage cards now keep capacity labels readable when storage content lists are long.
- `verifySsl` is now honored by the networking layer instead of being ignored.
- VM list actions now surface returned Proxmox task IDs in snackbar/task context.
- LXC list actions now surface returned Proxmox task IDs in snackbar/task context.
- VM and LXC task notices now link directly to the task detail/log screen from the snackbar action and persistent task card.
- VM and LXC cards now expose task history entry points that open the task center filtered by node and VMID.
- VM detail no longer exists only as an unused route placeholder; it now loads QEMU status through the feature repository path.
- Dashboard, VM list, and LXC list settings icons now navigate to the settings screen instead of using placeholder handlers.
- Backup create/download/restore/delete controls are disabled and labeled as read-only beta scope instead of appearing functional.
- User create/edit/delete controls are disabled and labeled as read-only beta scope instead of appearing functional.
- Settings controls that are not wired to runtime behavior are disabled and scoped to beta-safe copy.
- LXC detail resource, start/stop, and console placeholder controls are disabled and scoped to beta-safe copy.
- LXC detail no longer scans cached nodes or calls Retrofit directly from the Composable.
- Task operations now use the Proxmox UPID field instead of the display/resource ID.
- Node detail now accepts node status endpoint payloads that omit list-style node/status fields.
- Node detail now maps nested node status CPU and memory payloads into the rendered resource summary.
- VM/LXC lifecycle actions now suppress duplicate requests before queued coroutine work can race.
- VM/LXC lifecycle task handoff now ignores blank or non-UPID task identifiers.
- VM/LXC blocked delete attempts now clear stale task-detail handoff state.
- VM/LXC repeated resource cards use tighter action spacing and paired detail/task controls on Pixel-class screens.
- The task screen now labels the task stop endpoint as stopping/aborting a running task instead of deleting history.
- Dashboard node refresh logging no longer treats non-null API response lists as nullable.
- Network retry now performs a real reload instead of only resetting local UI state.
- Storage and network screens no longer call Retrofit directly from Composables.
- Storage screen can browse storage content read-only and labels upload/download/delete/restore as planned.
- User retry now performs a real reload instead of only resetting local UI state.
- User management no longer calls Retrofit directly from Composables.
- Backup retry now performs a real reload instead of only resetting local UI state.
- Backup management no longer calls Retrofit directly from Composables.
- Cluster screen now loads live read-only status instead of showing a coming-soon placeholder.
- Dashboard node loading and task summary loading no longer call Retrofit directly from Composables.
- Dashboard now has a manual refresh action in addition to periodic polling.
- Dashboard node cards now open node detail instead of jumping directly to the VM list.
- Unused direct VM/LXC/user/backup action helpers were removed from `MainViewModel`; active actions now live in feature repositories/ViewModels or remain disabled until implemented.
- Dead LXC screen metrics collection code and its direct `ProxmoxApiService` dependency were removed.
- `MainViewModel` now delegates authentication state/API-service access to `AuthSessionController` and saved credential persistence to `CredentialStore`.
- TLS authentication failures now point users toward Android trusted CAs, SHA-256 fingerprint pinning, or debug-only insecure lab mode.
- Network parsing now accepts Proxmox interface flags as booleans, numbers, or strings.
- User parsing now accepts numeric/string enabled flags and nullable profile fields.
- User and network populated lists now respect top app bar padding on Pixel-sized screens.
- Backup planned-action labels now stay readable on Pixel-sized screens.
- Settings version and build labels now use Gradle build metadata.
- Beta release workflow now blocks tag/APK version metadata mismatches before packaging.
- Beta release gate now delegates public hygiene scanning to a reusable script with an optional local deny-list pattern.
- Signed beta release workflow artifacts no longer include the staged unsigned APK copy.
- Signed beta release workflow refuses to mutate an existing release unless it is still a draft prerelease.
- Failed password/API-token authentication now clears any previous active session instead of leaving stale API access alive.
- Failed VM/LXC lifecycle actions now clear stale last-task notices instead of leaving an old task handoff card visible.
- Release builds now force SSL verification in login and saved-credential restore while preserving certificate fingerprint pinning.
- VM/LXC delete now has ViewModel-level stopped-state gating in addition to disabled UI controls.
- Task detail loading now accepts valid Proxmox status payloads that omit embedded UPID, using the routed UPID for task handoff.
- Activity recreation now retains the app-level session ViewModel instead of rebuilding it from Compose-only state.
- Non-secret login form drafts and task filter drafts now survive rotation before submission.

### Security

- Documented trust-all TLS and cleartext traffic as beta release blockers.
- Default HTTPS connections now use platform TLS validation.
- Self-signed servers can now use a pinned SHA-256 certificate fingerprint instead of disabling all SSL verification.
- Insecure certificate acceptance is limited to explicit debug builds instead of being available in release builds.
- HTTP body logging was replaced with basic request logging and auth header redaction.
- Release builds no longer request app-wide cleartext traffic through the manifest.

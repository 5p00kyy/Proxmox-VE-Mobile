# Proxmox VE Mobile Development Cycle

Date: 2026-06-06

This plan turns the current project audit into a development cycle for rebuilding Proxmox VE Mobile as a credible open Android client. It is intentionally broader than a bug list: it covers product direction, refactor strategy, repo process, QA, community workflow, and staged feature delivery.

## Executive Direction

The project should become a native Android operator console for Proxmox VE and Proxmox Backup Server environments. It should not try to copy every desktop web UI panel one-for-one. The right target is mobile feature parity: the same important work can be done from a phone or tablet with mobile-native navigation, safer confirmations, clear task feedback, and honest limits where desktop is still better.

The current codebase already points in that direction. It has login, dashboard, VM, LXC, storage, network, users, tasks, backups, cluster, settings, and localization surface. The main problem is that the app is wider than it is deep. Some screens work, some are read-only, and some expose controls that are not actually implemented. The next cycle should convert that broad prototype into a reliable vertical-slice architecture, then expand feature parity in waves.

## Product Positioning

The app should be differentiated by these principles:

1. Operator-safe by default.
   Every destructive or disruptive action needs context, confirmation, and task follow-up. A mobile app will often be used under pressure, so it must reduce accidental shutdowns, deletes, and wrong-node actions.

2. Task-first operations.
   Proxmox actions frequently return task IDs. The mobile experience should make tasks first-class: active task drawer, task logs, retry paths, and clear completion/failure states.

3. Mobile-native parity.
   Full parity means "can perform the workflow safely on mobile", not "every desktop field appears on one tiny screen." Desktop-style forms should be broken into guided, reviewable steps.

4. Open and transparent.
   The README, roadmap, issues, and release notes should state what works, what is read-only, what is experimental, and what is intentionally out of scope.

5. Secure homelab reality.
   Proxmox users often have self-signed certificates, VPNs, reverse proxies, API tokens, TFA, and nonstandard ports. The app should support that reality without silently disabling security.

6. Community-maintainable.
   Contributors should be able to run the build, understand the module structure, write focused tests, and find well-scoped issues.

## Research Findings

### Official Proxmox Feature Surface

The official Proxmox VE Mobile page describes a modern Android app plus the older HTML5 mobile client. Its feature list is a strong baseline for mobile parity:

- Dashboard overview for cluster or node status.
- Login manager for multiple clusters or nodes.
- Search and filter for guests, storage, and nodes.
- Users, API tokens, groups, roles, and domains overview.
- VM/container power controls such as start, stop, and reboot.
- RRD diagrams for nodes and guests.
- Guest migration between cluster nodes.
- Backup to storage and Proxmox Backup Server.
- Storage content view.
- Task history and current task overview.
- SPICE and HTML5 console support in the mobile context.
- TFA support and VPN guidance for remote access.

The Proxmox VE API is REST-like, JSON-based, and formally described with JSON Schema. The same API surface is exposed through the API viewer and `pvesh`, which means this app can use official endpoint documentation, representative `pvesh` payloads, and schema-driven tests to keep API models grounded in real Proxmox behavior.

### Community And Market Signals

Recent community and app research shows active interest in better mobile Proxmox management:

- ProxMobo positions itself around Proxmox VE and PBS management, resource tracking, performance monitoring, and interactive VNC/terminal access.
- ProxMate exposes a broad paid iOS feature set: TOTP, VM/LXC lifecycle actions, noVNC console, node terminal, node actions, utilization monitoring, storage views, task details, backups, reverse proxy support, custom headers, API tokens, app lock, and SSL handling.
- ProxUI is not a native app, but it is a useful design signal: it focuses on a simplified responsive interface, mobile-friendly workflows, multi-cluster management, touch-friendly console, guest agent data, and cloud image templates.
- Older Android/community clients and viewers show repeated attempts at the same problem. That is encouraging, but it also shows the usual failure mode: partial feature coverage without enough maintenance, QA, or security posture.

The opportunity for this repository is not just "another Proxmox app." The opportunity is an open Kotlin/Compose Android client with a professional roadmap, strong security defaults, first-class task tracking, localizable UI, and a contributor-friendly architecture.

### Android Engineering Guidance

Android's official architecture guidance supports the direction this project needs:

- Use a layered architecture: UI layer, data layer, and optional domain layer.
- Use unidirectional data flow: state flows down, events flow up.
- Keep repositories as the data abstraction consumed by ViewModels.
- Expose screen UI state from ViewModels rather than letting composables perform API calls.
- Use coroutines and flows for asynchronous and reactive data.
- Modularize when the codebase is large enough that ownership, visibility, testability, and build scalability matter.

Android's testing guidance also matches the project's biggest risk. Tests should give rapid feedback, catch failures early, make refactoring safer, and stabilize velocity. Compose testing APIs can verify semantics, attributes, and user actions, which makes them a good fit for the login, dashboard, VM, LXC, task, and settings surfaces.

### Workflow Guidance

For a public repository, the workflow should be explicit:

- GitHub Actions should build and test pull requests.
- Branch protection should require CI before merging to `main` and the active integration branch.
- Issue templates should capture Proxmox version, Android version, device, connection mode, auth method, and logs.
- Pull request templates should require screenshots for UI changes, QA commands, risk notes, and linked issues.
- CODEOWNERS can be added once there are regular maintainers or area owners.
- Releases should use Semantic Versioning for public version semantics and Keep a Changelog style notes for user-facing change history.

## Scope Model

The app should use three scope levels so the roadmap stays honest.

### Tier 1: Core Mobile Operator

This is the first real milestone and should be treated as the minimum viable public beta:

- Login/session management.
- Saved server accounts.
- API token login as the preferred saved credential mode.
- TFA-aware login flow.
- Secure TLS defaults with explicit self-signed certificate handling.
- Cluster/node dashboard.
- VM list, details, and safe lifecycle actions.
- LXC list, details, and safe lifecycle actions.
- Task history, active task tracking, and task logs.
- Minimal settings that actually affect runtime behavior.
- English plus maintained German and Spanish resources.

### Tier 2: Daily Operations

This tier turns the app from a power-control utility into a useful daily operator tool:

- Console access strategy: external SPICE, noVNC web view, or both.
- Guest snapshots.
- Guest backup creation and restore.
- Guest migration.
- Resource edits for common CPU, memory, disk, network, and autostart fields.
- Storage content browsing.
- Node service/status views.
- Node shutdown/reboot with strong safeguards.
- Search, filters, tags, and favorites.
- Per-resource task history.

### Tier 3: Admin And Cluster Parity

This tier targets deeper desktop-web style administration, adapted for mobile:

- Users, groups, API tokens, roles, permissions, and realms.
- Pools.
- Firewall views and common edits.
- Storage configuration.
- Network interface configuration.
- Cluster status and logs.
- HA resources.
- Replication jobs.
- Ceph status where available.
- Proxmox Backup Server integration.
- Notifications and alert routing.
- Metric server visibility.
- Support/subscription and update status where appropriate.

## Development Cycle

### Cycle 0: Stabilize The Project Baseline

Goal: make the repository honest, buildable, and safe to iterate.

Deliverables:

- README status updated with current feature truth.
- Audit document kept in `docs/`.
- Feature roadmap document added.
- Build requirements documented: JDK 17, Android SDK, expected Gradle commands.
- Local build verified with `test`, `lint`, and `assembleDebug`.
- Placeholder actions either hidden, disabled, or labeled experimental.
- Trust-all TLS called out as a release blocker.
- First GitHub Actions workflow added for build, lint, and unit tests.
- Pull request template added.
- Bug and feature issue templates added.
- Changelog initialized.

Exit criteria:

- A new contributor can clone the repo and understand what works.
- CI blocks broken builds.
- No visible button pretends to do something it cannot do.

### Cycle 1: Security And Session Foundation

Goal: centralize auth, credentials, API clients, and TLS policy before adding more features.

Deliverables:

- `SessionManager` owns server config, auth state, ticket, CSRF token, token auth, logout, and expiration behavior.
- `ServerAccountRepository` stores saved accounts without spreading credential logic through UI code.
- `ProxmoxApiFactory` is the only place that creates Retrofit and OkHttp clients.
- Default TLS validation uses platform trust.
- Self-signed support is explicit: certificate pin/trust-on-first-use/imported CA or a clearly labeled insecure development mode.
- Cleartext HTTP is disabled for release unless there is a deliberate debug-only exception.
- API token login is implemented and preferred for saved credentials.
- TFA flow is modeled instead of treated as a generic login failure.
- Production logging redacts auth headers, cookies, tickets, tokens, and request bodies containing secrets.

Exit criteria:

- Login, logout, saved accounts, and API token auth work through one session owner.
- Feature screens no longer construct API services directly.
- Security decisions are visible to the user and testable in code.

### Cycle 2: First Clean Vertical Slice

Goal: build one complete feature slice that becomes the template for the rest of the app.

Recommended slice: VM list and lifecycle operations.

Deliverables:

- DTOs for VM list, VM status, lifecycle responses, and task response.
- Domain/UI models separate from DTOs.
- `VmRepository` wraps all VM API calls used by the feature.
- `VmListViewModel` exposes immutable UI state.
- Compose screen renders loading, empty, error, content, refreshing, and action-in-progress states.
- Actions return task IDs and hand them to task tracking.
- Destructive actions require confirmation with resource name, VMID, node, and operation.
- Tests cover repository response mapping, ViewModel states, and action error handling.
- Compose UI tests cover list rendering and confirmation/action states.

Exit criteria:

- The VM feature can be used as a copyable pattern for LXC, tasks, backups, and storage.
- The screen contains no Retrofit knowledge and no hidden action state.

### Cycle 3: LXC And Task Center

Goal: bring containers and task follow-up to the same quality level as the VM slice.

Deliverables:

- `LxcRepository`, `LxcListViewModel`, and LXC detail state.
- Start, shutdown, stop, reboot, and delete with task follow-up where supported.
- LXC detail only exposes controls that are implemented.
- `TaskRepository` for active/history/task-log endpoints.
- Task center screen with active tasks, recent tasks, filters, and log detail.
- Global task indicator visible from the main app surface.
- Per-resource task history entry points from VM/LXC details.

Exit criteria:

- VM and LXC operations feel consistent.
- A user can see what happened after tapping an action.
- Task failures are not reduced to generic toasts.

### Cycle 4: Navigation And UX Overhaul

Goal: make the app feel professional, dense enough for infrastructure work, and reliable on phones, tablets, and foldables.

Deliverables:

- Resource-first navigation: Overview, Guests, Nodes, Storage, Tasks, Settings.
- Node identity carried through routes instead of guessed from the first cached node.
- Guest identity routes include node, type, and VMID.
- Adaptive layout for compact and expanded widths.
- Reusable UI kit for status chips, metric rows, action bars, progress states, task status, resource cards, and confirmation dialogs.
- Consistent empty/error/loading components.
- Material 3 theming with a restrained operations-focused visual system.
- Accessibility pass for content descriptions, semantic actions, contrast, touch targets, and dynamic text.
- Screenshots for README and release notes.

Exit criteria:

- The app no longer feels like separate prototype screens.
- Important operational context is always visible before a dangerous action.
- Tablet layout uses available space instead of stretching phone cards.

### Cycle 5: Daily Operations

Goal: expand beyond lifecycle actions into workflows that make the app worth keeping installed.

Deliverables:

- Console strategy decided and implemented for one guest type first.
- Snapshots for VM and LXC.
- Backup create, list, protect/unprotect/delete, and restore flow.
- Storage content browser for ISO, vzdump, snippets, container templates, and disk images.
- Guest migration flow with node/storage choices and task tracking.
- Guest resource edit for the safest common settings.
- Search and filters across guests and storage.
- Favorites or pinned resources.

Exit criteria:

- A homelab operator can handle routine issues from mobile without opening desktop web UI.
- Complex operations use review steps before submitting.

### Cycle 6: Admin And Cluster Expansion

Goal: add deeper administrative parity after the core operator experience is stable.

Deliverables:

- Users/groups/API token/role/permission views.
- Safe add/edit/delete flows for user management.
- Cluster status, quorum, node membership, and cluster log.
- HA status and common resource operations.
- Replication jobs.
- Ceph status where endpoint data is available.
- Network/storage configuration edits only after robust validation exists.
- Proxmox Backup Server account/support if the API and UX shape are clear.

Exit criteria:

- Admin screens meet the same quality bar as VM/LXC.
- Advanced features do not compromise the clarity of the core app.

## Proposed Repository Structure

The project should not jump straight into heavy modularization before a clean slice exists. Use a two-step structure plan.

### Short Term Package Structure

Keep the single Gradle module while making ownership clearer:

```text
app/src/main/java/com/proxmoxmobile/
|-- core/
|   |-- auth/
|   |-- network/
|   |-- model/
|   |-- result/
|   |-- settings/
|   |-- task/
|   `-- ui/
|-- data/
|   |-- account/
|   |-- node/
|   |-- vm/
|   |-- lxc/
|   |-- storage/
|   `-- user/
|-- domain/
|   |-- guest/
|   |-- task/
|   `-- session/
`-- feature/
    |-- auth/
    |-- dashboard/
    |-- guests/
    |-- nodes/
    |-- storage/
    |-- tasks/
    `-- settings/
```

This gives immediate structure without Gradle overhead.

### Long Term Gradle Modules

Move to modules when the first clean VM/LXC/task slices are stable:

```text
:app
:core:model
:core:network
:core:session
:core:settings
:core:task
:core:ui
:core:testing
:data:proxmox
:data:account
:feature:auth
:feature:dashboard
:feature:guests
:feature:nodes
:feature:storage
:feature:tasks
:feature:settings
```

Module rules:

- Feature modules depend on core contracts and repositories, not Retrofit services.
- Data modules own DTOs, API services, and mapping into domain models.
- `:core:ui` owns reusable Compose components and theme tokens.
- `:core:testing` owns fake repositories, sample payloads, coroutine rules, and test helpers.
- `:app` wires navigation, dependency injection, and app-level lifecycle.
- Build logic should be introduced only when duplicate Gradle configuration becomes painful.

## Architecture Standards

Every production feature should follow this shape:

```text
Composable Screen
    -> Feature ViewModel
        -> Use Case, when workflow logic is non-trivial
            -> Repository
                -> API Service / Storage / Data Source
```

Rules:

- Composables render state and emit events.
- ViewModels own screen state, validation, refresh, and user events.
- Repositories own network/storage interaction and mapping.
- DTOs should be nullable and close to API payloads.
- Domain/UI models should be safer and more explicit than DTOs.
- All network responses should map into a project result type.
- Avoid global mutable state for active node, current VM, or current task.
- Routes must carry identity: cluster/server, node, guest type, VMID, storage ID, task UPID.
- Errors should be typed enough to distinguish auth, permission, TLS, network, Proxmox task failure, validation, and unknown failure.

## QA Strategy

### Automated Checks

Minimum PR gate:

```bash
./gradlew test lint assembleDebug
```

Add these checks as the project matures:

- Kotlin formatting or ktlint.
- Static analysis with detekt once noise is manageable.
- Dependency vulnerability/deprecation review.
- Localization key and placeholder consistency test.
- Unit tests for repositories, mappers, session, and ViewModels.
- Compose UI tests for critical screens.
- Screenshot tests for visual regression on compact and expanded layouts.
- Instrumentation smoke tests against a fake server or controlled Proxmox fixture.

### Manual QA Matrix

Every beta candidate should be tested against:

- Standalone Proxmox VE node.
- Multi-node cluster.
- Proxmox VE with self-signed certificate.
- Proxmox VE behind reverse proxy, if supported.
- Password login.
- API token login.
- TFA-enabled account.
- Restricted permission user.
- Offline/unreachable server.
- Guest lifecycle operations for both QEMU and LXC.
- Failed task and successful task paths.
- English, German, and Spanish UI.
- Small phone, large phone, tablet/foldable.

### Release Blockers

These should block public beta releases:

- Trust-all TLS in release builds.
- Cleartext traffic enabled in release without explicit debug-only gating.
- Unredacted auth data in logs.
- Any visible destructive action without confirmation.
- Any high-traffic screen that crashes on missing/nullable API fields.
- Placeholder action presented as functional.
- CI failing on `main`.

## Community Workflow

### Repository Health Files

Add:

```text
.github/workflows/android.yml
.github/ISSUE_TEMPLATE/bug_report.yml
.github/ISSUE_TEMPLATE/feature_request.yml
.github/ISSUE_TEMPLATE/config.yml
.github/pull_request_template.md
CHANGELOG.md
CONTRIBUTING.md
SECURITY.md
CODEOWNERS
```

`CODEOWNERS` can start minimal or be delayed until there are regular collaborators.

### Issue Labels

Recommended labels:

- `area:auth`
- `area:network`
- `area:vm`
- `area:lxc`
- `area:tasks`
- `area:storage`
- `area:backup`
- `area:ui`
- `area:localization`
- `area:security`
- `area:ci`
- `type:bug`
- `type:feature`
- `type:refactor`
- `type:docs`
- `type:test`
- `priority:blocker`
- `priority:high`
- `good-first-issue`
- `needs-proxmox-fixture`

### Pull Request Expectations

Each PR should state:

- What changed.
- Why it changed.
- Screenshots or screen recordings for UI changes.
- QA commands run.
- Proxmox versions or fixtures tested.
- Security impact.
- Localization impact.
- Follow-up issues.

### Branch Strategy

Use a small branch model that keeps public releases clean while giving the next beta cycle room to integrate work:

- `main`: release-ready public branch for beta tags and hotfixes.
- `dev`: integration branch for the next beta cycle and default target for feature/fix PRs.
- `feature/*`: short-lived feature branches merged into `dev`.
- `fix/*`: targeted bugfix branches merged into `dev`, or into `main` first for release hotfixes.

Promote `dev` to `main` only when the next beta scope is frozen, CI is green, smoke QA is recorded, release notes are prepared, and the beta gate passes. Cut public beta tags only from `main`. After a `main` hotfix, merge the same fix back into `dev` so the integration branch does not drift.

### Versioning And Releases

Use a simple release path:

- `0.1.x`: first beta line covering repository recovery, CI, security foundation, core login/dashboard/VM/LXC/task operations, and read-only infrastructure slices.
- `0.2.x`: beta stabilization and daily operator workflow expansion.
- `0.3.x`: daily operations beta: snapshots, backups, storage, migration.
- `0.4.x`: admin workflows.
- `1.0.0`: stable operator app with secure defaults, documented support matrix, and no misleading placeholder surface.

Use Semantic Versioning for public releases and a Keep a Changelog style `CHANGELOG.md` with `Added`, `Changed`, `Deprecated`, `Removed`, `Fixed`, and `Security` sections.

## Twelve-Week Execution Plan

### Week 1: Repository Recovery

- Merge or commit current audit fixes.
- Add this development plan to the README.
- Add GitHub Actions for `test`, `lint`, and `assembleDebug`.
- Add issue and PR templates.
- Add `CHANGELOG.md`, `CONTRIBUTING.md`, and `SECURITY.md`.
- Open issues for every release blocker from the audit.

### Week 2: Security And Session Design

- Replace direct API client construction with a central factory.
- Design session/account models.
- Implement API token login path.
- Add imported CA guidance after the SHA-256 fingerprint pinning path; keep trust-on-first-use as a deliberate later design.
- Add tests around auth header/cookie behavior and credential storage.

### Week 3: VM Vertical Slice

- Create VM repository and DTO mapping.
- Create VM UI state and ViewModel.
- Move VM list/actions to the new pattern.
- Add task ID handoff after lifecycle actions.
- Add unit tests for success, permission failure, network failure, and task response parsing.

### Week 4: VM UX And Task Follow-Up

- Add task detail/log view.
- Add VM detail surface with implemented-only actions.
- Add confirmation dialog improvements.
- Add Compose tests for VM list and destructive confirmation.

### Week 5: LXC Slice

- Apply the VM pattern to LXC list and detail.
- Remove or disable unfinished LXC resource controls.
- Add LXC lifecycle and task tests.
- Verify node identity flows through navigation.

### Week 6: Dashboard And Navigation

- Refactor dashboard through repositories/ViewModels.
- Replace first-node assumptions with explicit node/resource identity.
- Redesign navigation around Overview, Guests, Nodes, Storage, Tasks, Settings.
- Add adaptive layouts for compact and expanded screens.

### Week 7: Visual System

- Build reusable status, metric, resource, task, and action components.
- Normalize loading/error/empty states.
- Run accessibility pass.
- Capture screenshots for README.
- Add screenshot or UI smoke tests where practical.

### Week 8: Hardening Beta 0.2

- Remove release-blocking TLS/cleartext issues.
- Redact logs.
- Add localization consistency test.
- Test against standalone node and cluster.
- Publish a beta checklist and known gaps.

### Week 9: Storage And Backups

- Extend read-only storage content browser with upload/download/delete/protect actions.
- Implement backup listing/details from real endpoints.
- Implement backup create/delete/protect where safe.
- Design restore flow with strong review step before execution.

### Week 10: Snapshots And Migration

- Add snapshot list/create/delete/rollback workflows.
- Add guest migration workflow.
- Add task tracking to both.
- Test partial permission and failure states.

### Week 11: Beta Polish

- Prepare GitHub release notes.
- Add screenshots, roadmap, and support matrix to README.
- Triage community issues into the scope model.
- Mark advanced/admin features clearly as post-beta.

### Week 12: Beta Release And Feedback Loop

- Release `v0.1.0-beta.1` or the next appropriate beta tag.
- Open a feedback discussion or tracking issue.
- Convert feedback into labeled issues.
- Pick the next development wave based on real usage, not speculation.

## Definition Of Done

A feature is done only when:

- It has loading, empty, error, refresh, and success states.
- It handles permission errors distinctly.
- It handles missing/nullable Proxmox fields without crashing.
- It has localized user-facing strings.
- It has no hardcoded secrets or unredacted auth logging.
- It does not call Retrofit directly from composables.
- It has unit tests for mapping and ViewModel behavior.
- It has Compose tests or manual QA notes for critical UI paths.
- Destructive or disruptive operations show strong confirmation.
- Proxmox task-returning operations expose task progress or task history.
- The README, docs, or changelog are updated when user-visible behavior changes.

## Immediate Backlog

1. [x] Add `.github/workflows/android.yml` with `test`, `lint`, and `assembleDebug`.
2. [x] Add issue and PR templates.
3. [x] Add `CHANGELOG.md`, `CONTRIBUTING.md`, and `SECURITY.md`.
4. [x] Create the session/API factory refactor branch.
5. [x] Replace silent trust-all TLS with default platform TLS and explicit insecure lab mode.
6. [x] Implement API token login as first-class.
7. [x] Move VM list/actions into repository + feature ViewModel architecture.
8. [x] Surface returned task IDs after VM lifecycle actions.
9. [x] Move LXC list/actions into repository + feature ViewModel architecture.
10. [x] Surface returned task IDs after LXC lifecycle actions.
11. [x] Add repository-backed task log/detail views and running-task stop requests.
12. [x] Hide or disable unfinished actions in backups, users, settings, and LXC detail.
13. [x] Add localization consistency test.
14. [x] Add direct task-detail links from VM/LXC action notices.
15. [x] Add dashboard task activity as a global task indicator.
16. [x] Add task filters for status, type, and VMID.
17. [x] Add VM/LXC card entry points into VMID-filtered task history.
18. [x] Add SHA-256 certificate fingerprint pinning for self-signed Proxmox servers.
19. [x] Restrict trust-all TLS fallback to debug builds.
20. [x] Move network and storage read-only screens into repository + feature ViewModel architecture.
21. [x] Add network/storage repository tests and real network retry behavior.
22. [x] Move user management read-only screen into repository + feature ViewModel architecture.
23. [x] Add user repository tests and real user retry behavior.
24. [x] Move backup read-only screen into repository + feature ViewModel architecture.
25. [x] Add backup repository tests, node/storage selection, and real backup retry behavior.
26. [x] Move dashboard node loading and task summary into repository + feature ViewModel architecture.
27. [x] Add dashboard repository tests and manual dashboard refresh.
28. [x] Move LXC detail status loading into repository + feature ViewModel architecture.
29. [x] Add node-aware LXC detail routing and LXC detail repository tests.
30. [x] Move VM detail status loading into repository + feature ViewModel architecture.
31. [x] Add node-aware VM detail routing and VM detail repository tests.
32. [x] Add imported CA guidance and document trust-on-first-use requirements.
33. [x] Add repository-backed node detail status screen.
34. [x] Add node detail repository tests and node-aware dashboard routing.
35. [x] Remove unused direct VM/LXC/user/backup action helpers from `MainViewModel`.
36. [x] Add session/auth factory seams and `SessionManager` unit tests.
37. [x] Add node-scoped network routing and network ViewModel selection tests.
38. [x] Remove dead LXC screen metrics collector and direct API dependency.
39. [x] Split authentication state/API-service access into `AuthSessionController` with unit tests.
40. [x] Add `CredentialStore` seam and remove UI dependency on `SecureStorage` auth constants.
41. [x] Replace cluster placeholder with repository-backed read-only cluster status.
42. [x] Add cluster repository tests for status mapping and missing sessions.
43. [x] Add repository-backed read-only storage content browsing.
44. [x] Add storage-content repository tests and broaden storage content models.
45. [x] Add repository-backed read-only VM snapshot visibility.
46. [x] Add VM snapshot repository tests and typed snapshot models.
47. [x] Add repository-backed read-only LXC snapshot visibility.
48. [x] Add LXC snapshot repository tests and typed snapshot models.
49. [x] Add repository-backed read-only VM configuration visibility.
50. [x] Add VM configuration repository tests for sorting, redaction, validation, and missing sessions.
51. [x] Add VM reboot lifecycle action with task follow-up.
52. [x] Add LXC reboot lifecycle action with task follow-up.
53. [x] Add VM/LXC reboot repository tests and action-specific progress state.
54. [x] Add confirmation dialogs for disruptive VM/LXC stop and reboot requests.
55. [x] Add VM/LXC graceful shutdown lifecycle actions with task follow-up.
56. [x] Distinguish graceful shutdown from force stop in lifecycle UI and confirmations.
57. [ ] Validate TLS guidance against real Proxmox appliances and reverse proxies.

## Research Sources

- Official Proxmox VE Mobile: https://pve.proxmox.com/wiki/Proxmox_VE_Mobile
- Official Proxmox VE API: https://pve.proxmox.com/wiki/Proxmox_VE_API
- Proxmox VE API viewer: https://pve.proxmox.com/pve-docs/api-viewer/
- Proxmox VE GUI documentation: https://pve.proxmox.com/pve-docs/chapter-pve-gui.html
- ProxMobo: https://proxmobo.app/
- ProxMate App Store listing: https://apps.apple.com/nz/app/proxmate/id6470526961
- ProxUI: https://proxui.app/
- Android app architecture: https://developer.android.com/topic/architecture
- Android modularization: https://developer.android.com/topic/modularization
- Android testing: https://developer.android.com/training/testing
- Compose testing: https://developer.android.com/develop/ui/compose/testing
- Now in Android sample: https://github.com/android/nowinandroid
- GitHub Actions Gradle guidance: https://docs.github.com/en/actions/tutorials/build-and-test-code/java-with-gradle
- GitHub issue templates: https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/configuring-issue-templates-for-your-repository
- GitHub pull request templates: https://docs.github.com/articles/creating-a-pull-request-template-for-your-repository
- GitHub CODEOWNERS: https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/about-code-owners
- GitHub protected branches: https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches
- Semantic Versioning: https://semver.org/
- Keep a Changelog: https://keepachangelog.com/

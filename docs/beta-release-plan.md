# Proxmox VE Mobile Beta Release Plan

Target: `v0.1.0-beta.1`

This plan defines the work required to turn the current revival baseline into the first official beta release. The beta is intended to be a credible public test build for common Proxmox VE mobile operations, not a full desktop web UI replacement.

## Release Positioning

`v0.1.0-beta.1` should be described as a native Android Proxmox VE companion app for:

- Logging in with password or API token credentials.
- Inspecting cluster, node, guest, task, storage, network, user, backup, and cluster status surfaces.
- Performing common VM and LXC lifecycle actions with confirmation and task follow-up.
- Browsing read-only infrastructure surfaces that are not yet safe to edit from mobile.
- Testing mobile-first workflows and reporting real Proxmox compatibility issues.

The beta should not claim full feature parity, Proxmox Backup Server parity, or complete administrative coverage.

## Progress

```text
Revival baseline pushed      [##################..] 90%
Beta scope frozen            [############........] 60%
Automated release gate       [##################..] 88%
Real Proxmox smoke QA        [#############.......] 65%
UX/copy release polish       [###############.....] 75%
Release packaging            [################....] 78%
Official beta readiness      [################....] 81%
```

## Release Gates

All required gates must pass before tagging `v0.1.0-beta.1`.

| Gate | Required | Evidence |
| --- | --- | --- |
| Unit tests | Yes | `./gradlew test` passes locally or in CI |
| Lint | Yes | `./gradlew lint` passes locally or in CI |
| Debug build | Yes | `./gradlew assembleDebug` passes locally or in CI |
| Release build | Yes | `./gradlew assembleRelease` passes locally or in CI |
| Public branch hygiene | Yes | No machine-specific paths, hostnames, tokens, private IPs, or credentials in tracked docs/source |
| Real Proxmox login smoke | Yes | Password and API token login tested against a real Proxmox VE target |
| TLS smoke | Yes | Platform-trusted certificate and self-signed/fingerprint flow tested or documented as known limitation |
| VM/LXC lifecycle smoke | Yes | Start, graceful shutdown, force stop, reboot, and delete tested only on disposable guests |
| Task follow-up smoke | Yes | Returned task IDs open task detail/log state after VM/LXC actions |
| Navigation smoke | Yes | Dashboard, node detail, VM, LXC, storage, network, users, backups, tasks, cluster, settings do not crash during normal navigation |
| Known limitations | Yes | README and release notes clearly identify read-only and planned areas |
| Release notes | Yes | `CHANGELOG.md` has a `v0.1.0-beta.1` section before tagging |
| Release media | Yes | Public-safe screenshots or short recordings are captured, sanitized, and referenced without private environment details |
| Release artifact | Yes | Signed APK attached to GitHub release with release notes and install guidance |

## Release Packaging Workflow

The beta APK release path is handled by `.github/workflows/beta-release.yml`.

The Android CI and beta release workflows opt into Node 24 JavaScript action execution so hosted-runner behavior is tested before GitHub's Node 24 default takes effect.

Triggers:

- `push` tags matching `v*-beta.*`, including `v0.1.0-beta.1`.
- Manual `workflow_dispatch` dry runs with a beta tag name input for artifact naming.

The workflow runs the beta gate with:

```bash
./scripts/beta-gate.sh v0.1.0-beta.1
```

Manual smoke progress can be summarized locally with:

```bash
./scripts/beta-qa-status.sh
```

Before tagging, use the same checker as a hard completion gate:

```bash
./scripts/beta-qa-status.sh --require-complete
```

The gate verifies the beta tag format, confirms the tag matches the Gradle `versionName` with a leading `v`, runs `git diff --check`, calls `scripts/public-hygiene-check.sh`, then runs `test`, `lint`, `assembleDebug`, and `assembleRelease`. The hygiene check scans tracked public docs/workflows/scripts and application source for local environment details, inline secret-looking values, and optional locally supplied deny-list patterns through `EXTRA_PUBLIC_HYGIENE_PATTERN`. For `v0.1.0-beta.1`, `app/build.gradle.kts` must report `versionName = "0.1.0-beta.1"`. This prevents a release filename from advertising a different version than the installed APK metadata.

Manual dry runs upload the produced release APK as a GitHub Actions artifact. Tag runs must also provide signing material through repository secrets before a signed APK can be attached to a GitHub Release:

- `ANDROID_RELEASE_KEYSTORE_BASE64`
- `ANDROID_RELEASE_KEYSTORE_PASSWORD`
- `ANDROID_RELEASE_KEY_ALIAS`
- `ANDROID_RELEASE_KEY_PASSWORD`

`ANDROID_RELEASE_KEYSTORE_BASE64` should contain the base64-encoded Android signing keystore. The workflow decodes it into the GitHub runner's temporary directory, signs the release APK with Android build tools, verifies it with `apksigner`, and attaches only the signed APK to the release. It does not depend on machine-specific SDK paths, local keystore files, or local `local.properties` content.

Manual `workflow_dispatch` runs can be used to confirm the release build and artifact packaging before signing secrets are configured. Unsigned release APKs from dry runs are for inspection only and should not be published as beta downloads. Signed tag runs create or update a draft prerelease, remove the unsigned APK copy from the uploaded workflow artifact after signing, and attach only the signed APK. If a release already exists for the tag, the workflow refuses to upload unless it is still a draft prerelease. Publish the GitHub Release manually after the APK, changelog, release notes, and media are verified.

GitHub only accepts `workflow_dispatch` triggers for workflows that exist on the repository default branch. While the beta baseline is still only on the draft PR branch, the release workflow can be linted and reviewed, but the manual dry run cannot be triggered from GitHub until the workflow is merged or otherwise present on the default branch.

## Beta Scope

### Required

- Password login.
- API token login.
- Saved credential restore.
- Platform TLS by default.
- SHA-256 fingerprint pinning path for self-signed servers.
- Dashboard node/resource/task summary.
- Node detail navigation.
- VM list/detail with read-only config and snapshots.
- VM start, graceful shutdown, force stop, reboot, and delete.
- LXC list/detail with read-only snapshots.
- LXC start, graceful shutdown, force stop, reboot, and delete.
- Task list, filters, details, logs, and running task stop where supported.
- Storage list and read-only content browsing.
- Network interface list.
- User list.
- Backup list.
- Cluster status.
- English, German, and Spanish string key/placeholder consistency.

### Explicitly Read-Only Or Planned

- Backup create, restore, download, and delete.
- User create, edit, and delete.
- Storage upload, download, delete, and restore.
- VM configuration editing.
- LXC resource editing.
- Guest console.
- Node reboot, shutdown, shell, and advanced service actions.
- Proxmox Backup Server management.

### Deferred Post-Beta

- TFA-specific login flow.
- Multiple saved server/account manager.
- Search across nodes, guests, storage, and tasks.
- Guest migration.
- Snapshot create/delete/rollback.
- Backup creation and restore.
- Console strategy: external SPICE, noVNC WebView, or both.
- Push notifications and alert routing.
- F-Droid or Play Store packaging.

## Manual Smoke Matrix

Run this matrix against a disposable or non-production Proxmox VE environment before release.

| Area | Test | Blocker |
| --- | --- | --- |
| Auth | Password login succeeds with valid credentials | Yes |
| Auth | Password login shows useful error for invalid credentials | Yes |
| Auth | API token login succeeds with valid token ID/secret | Yes |
| Auth | Saved credentials restore and can be cleared | Yes |
| TLS | Platform-trusted HTTPS connects without override | Yes |
| TLS | Self-signed certificate path is understandable and functional | Yes |
| TLS | Release build does not expose insecure trust-all mode | Yes |
| Dashboard | Dashboard loads nodes and resource summary | Yes |
| Dashboard | Manual refresh updates without duplicating state | Yes |
| Nodes | Node card opens matching node detail | Yes |
| Navigation | Node-scoped VM/LXC/storage/network/task routes keep node context | Yes |
| VM | VM list loads and empty/error states render | Yes |
| VM | VM detail loads status, config, and snapshots read-only | Yes |
| VM | Start on disposable guest prompts, submits, returns task notice, and links to task detail/log | Yes |
| VM | Graceful shutdown on disposable guest prompts, submits, returns task notice, and links to task detail/log | Yes |
| VM | Force stop on disposable guest prompts, submits, returns task notice, and links to task detail/log | Yes |
| VM | Reboot on disposable guest prompts, submits, returns task notice, and links to task detail/log | Yes |
| VM | Delete on stopped disposable guest prompts, submits, returns task notice, and links to task detail/log | Yes |
| LXC | Container list loads and empty/error states render | Yes |
| LXC | Container detail loads status and snapshots read-only | Yes |
| LXC | Start on disposable container prompts, submits, returns task notice, and links to task detail/log | Yes |
| LXC | Graceful shutdown on disposable container prompts, submits, returns task notice, and links to task detail/log | Yes |
| LXC | Force stop on disposable container prompts, submits, returns task notice, and links to task detail/log | Yes |
| LXC | Reboot on disposable container prompts, submits, returns task notice, and links to task detail/log | Yes |
| LXC | Delete on stopped disposable container prompts, submits, returns task notice, and links to task detail/log | Yes |
| Tasks | Task list result links open detail/log screen | Yes |
| Tasks | Task filters do not crash with empty or partial data | Yes |
| Storage | Storage list and content browser load read-only | Yes |
| Network | Interface list loads for selected node | Yes |
| Users | User list loads read-only | Yes |
| Backups | Backup list loads and partial-storage failures are understandable | Yes |
| Cluster | Cluster status loads standalone and clustered responses | Yes |
| Settings | Planned settings are disabled or do not imply runtime behavior | Yes |

### Navigation Route Matrix

These routes are in beta navigation scope and should be included in route, rotation, and resume smoke:

- Login.
- Dashboard.
- Node detail.
- VM list and VM detail, including node-scoped detail routes.
- LXC list and LXC detail, including node-scoped detail routes.
- Storage list and read-only content browser.
- Network list, both global and node-scoped.
- Users.
- Backups.
- Tasks, including global, node-scoped, resource-filtered, task detail, and task log states.
- Cluster.
- Settings.

These route helpers exist but are not registered beta destinations and should not be claimed in beta release notes until implemented:

- Storage detail.
- User detail.

The source route registry and unit tests keep registered beta route patterns, planned route helpers, and duplicate route patterns explicit while the app moves toward broader navigation coverage.

VM and LXC lifecycle ViewModel tests now cover start, shutdown, force stop, reboot, delete guard behavior, duplicate action suppression, blank/invalid returned task IDs, failed action cleanup, stale task handoff cleanup, and task-detail route generation. This does not replace disposable guest smoke, but it reduces the chance of shipping a broken task handoff path into manual QA.

Pixel-class Android emulator smoke now covers password login, dashboard load, node detail navigation, VM list/detail routing, and LXC list routing against live Proxmox data. That smoke found and verified fixes for node status endpoint payload normalization, including missing list-style status fields and nested CPU/memory metrics. It does not replace API-token, TLS/fingerprint, disposable guest lifecycle, task follow-up, rotation, resume, or public-media smoke.

The latest UX polish pass tightened repeated VM/LXC resource cards, paired detail/task controls, and moved disabled surfaces toward read-only beta copy instead of raw implementation-status language. The next polish pass should verify the full route matrix on-device and capture public-safe media only from a disposable or redacted environment.

### Automation Plan For Remaining Blockers

The route registry and unit tests prove beta route patterns, route encoding, lifecycle ViewModel behavior, and task-detail route generation, but they do not prove Activity recreation, background resume, Compose layout, or live Proxmox behavior. Before the beta tag, keep the manual smoke matrix as the source of truth while adding only narrow automation that can run without private infrastructure:

- Add Compose instrumentation smoke for every registered beta route using fake session data and fake repositories.
- Add Activity recreation coverage for login non-secret drafts and task filter drafts.
- Add fake-API UI coverage for VM/LXC task notices opening task detail routes.
- Leave API-token login, TLS/fingerprint behavior, disposable lifecycle actions, and task-log handoff as lab-backed smoke until a disposable Proxmox fixture can be provisioned in CI.

## Development Work Remaining

1. Open the pushed baseline branch as a draft PR.
2. Run the automated gate in CI and locally after any blocker fixes.
3. Execute the real Proxmox smoke matrix.
4. Fix only beta blockers on the baseline branch.
5. Add release screenshots or short screen recordings to the README/release notes.
6. Promote `CHANGELOG.md` from `Unreleased` to `v0.1.0-beta.1`.
7. Merge or otherwise land the beta release workflow on the repository default branch so `workflow_dispatch` can run.
8. Configure release signing secrets and run the beta APK release workflow on the beta tag.
9. Confirm the signed APK is attached to the GitHub release and the README install steps match the published artifact name.
10. Keep a post-beta issue list for deferred parity work.

## Release Notes And Media

The beta release notes should be concise and honest about scope:

- Lead with the app purpose: an Android Proxmox VE companion for login, mobile inspection, VM/LXC lifecycle actions, and task follow-up.
- State that this is a beta for public testing, not full desktop web UI feature parity.
- Link the signed APK attached to the GitHub Release and call out that debug/unsigned artifacts are not public beta downloads.
- Summarize verified smoke coverage only after the smoke matrix is complete.
- Keep unfinished areas in a known limitations section rather than implying they work.
- Ask testers to report Proxmox version, Android version, auth method, TLS setup, and sanitized logs/screenshots.

Use this release-note shape before publishing the draft GitHub Release:

```text
Proxmox VE Mobile v0.1.0-beta.1

Status
- First public beta for Android operators testing mobile Proxmox VE workflows.
- Not a full desktop web UI replacement.

Install
- Download only the signed APK attached to this GitHub Release.
- Do not use debug APKs or unsigned workflow artifacts as public beta builds.

Included In This Beta
- Password and API token login.
- TLS validation with fingerprint pinning support for self-signed environments.
- Dashboard, node, VM, LXC, task, storage, network, user, backup, and cluster inspection.
- VM/LXC lifecycle actions with confirmation and task follow-up.

Known Limitations
- Guest console, snapshot mutation, backup mutation, user mutation, node power actions, and Proxmox Backup Server management are outside this beta scope.
- Storage, network, user, backup, and cluster areas are primarily read-only.
- TFA-specific password login handling is not complete.

Verified Smoke Coverage
- Fill this section only from completed entries in docs/beta-smoke-qa.md.

Public Media
- List only disposable-lab or fully sanitized screenshots/recordings.

Report Issues
- Include Android version, Proxmox VE major version, auth method, TLS mode, and sanitized logs/screenshots.
```

Before tagging, promote `CHANGELOG.md` from `Unreleased` to:

```text
## [v0.1.0-beta.1] - YYYY-MM-DD
```

Use the actual tag date, keep any unfinished future work under a new `Unreleased` section, and do not move pending smoke claims into the released section unless there is QA evidence.

### Public Media Checklist

Capture release media from a disposable lab, sanitized demo environment, or fully redacted screenshots. Do not reuse private emulator screenshots that show real hostnames, usernames, IP addresses, node names, backup comments, task logs, tokens, cookies, tickets, certificate fingerprints, or local desktop paths.

Required release captures:

| Capture | Purpose | Sanitization |
| --- | --- | --- |
| Login/TLS | Show password/API token mode and fingerprint support | Use placeholder host text or a disposable lab endpoint |
| Dashboard | Show first-screen mobile layout and node/task summary | Use generic lab node and guest names |
| VM list/detail | Show lifecycle controls and read-only detail/config/snapshot areas | Use disposable guest names and no production IDs |
| LXC list/detail | Show container lifecycle controls and detail screen | Use disposable guest names and no production IDs |
| Task detail/log | Show returned-task follow-up | Redact UPIDs, usernames, node names, IPs, and log values unless from a throwaway lab |
| Read-only admin slice | Show storage, network, users, backups, or cluster status | Prefer storage/cluster; avoid exposing network addresses or real account names |

Recommended media set:

- 4-6 PNG screenshots for the GitHub Release body.
- 1 short screen recording or GIF showing dashboard to guest action to task detail, only after disposable lifecycle smoke passes.
- Alt text or captions that describe the workflow without environment identifiers.
- A media manifest that maps every file to its screen/workflow, source type, sanitized identifiers, caption or alt text, and QA status.

If public-safe media cannot be captured before the beta tag, ship the release notes without screenshots rather than publishing redacted private infrastructure accidentally.

## Estimated Timeline

```text
Fast beta        2 days     Automated gate, one Proxmox smoke target, blocker-only fixes
Recommended beta 3-5 days   Smoke QA, UX/copy polish, screenshots, release notes
Careful beta     1-2 weeks  Multiple Proxmox versions, reverse proxy/TLS variants, broader device QA
```

The recommended path is `3-5 days`: enough real validation to avoid a brittle public release while keeping the project moving.

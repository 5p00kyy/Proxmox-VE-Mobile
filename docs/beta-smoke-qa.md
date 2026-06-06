# Beta Smoke QA

This document records manual validation evidence for the `v0.1.0-beta.1` track. Do not record private hostnames, public IPs, usernames, tokens, screenshots with sensitive data, or other environment details that should not be public.

## Public-Safe Evidence Template

Use this shape for every manual QA pass:

```text
Date:
Build SHA:
APK type: debug | signed release
Android device/API class:
Proxmox VE major version:
Auth method: password | API token
TLS mode: platform trusted | fingerprint pin | debug insecure lab mode
Result: Pending | Pass | Fail | Blocked
Sanitized notes:
```

Do not include hostnames, IP addresses, usernames, tokens, cookies, tickets, certificate fingerprints from private servers, task IDs from private environments, backup comments, local machine paths, or screenshots with private details.

## 2026-06-06 Android Studio Emulator Pass

Environment:

- Android Studio project opened from the repository.
- Pixel 8 API 36 emulator.
- Debug APK built from `revival-2026-baseline`.
- Real user-supplied Proxmox VE environment. Private connection details intentionally not recorded.

Automated gate before/after this pass:

```bash
./gradlew test lint assembleDebug
```

Result: passed.

Observed pass:

- App installs on the emulator.
- App launches from the emulator.
- Login reaches the dashboard against a real Proxmox VE environment.
- Dashboard renders on a Pixel-sized viewport without obvious first-screen layout breakage.
- System status card renders CPU, memory, and uptime values.
- Task activity card renders running/recent/latest task data.
- Node list renders at least one online node.
- Quick action cards render for VM/LXC/storage/network entry points.
- VM list opens from the dashboard and renders stopped/running guests from a real Proxmox VE node.
- VM lifecycle labels render as readable single-line labels on a Pixel-sized viewport.
- VM delete is available for stopped guests and disabled with explicit copy for running guests.
- LXC list opens from the dashboard and renders running containers from a real Proxmox VE node.
- LXC lifecycle labels render as readable single-line labels on a Pixel-sized viewport.
- LXC delete is disabled with explicit copy for running containers.
- LXC task history opens with node and VMID filters populated.
- Task statistics count Proxmox `OK` task results as finished.
- Task detail opens from task history and loads task metadata plus task log output.
- Storage initially exposed a live Proxmox payload compatibility issue where storage `content` arrived as a comma-separated string rather than an array.
- Rebuilt app parses string/array storage content shapes plus numeric storage flags.
- Storage list opens from the dashboard and renders real storage rows on a Pixel-sized viewport.
- Storage card capacity fields remain readable when storage content lists are long.
- Storage content browser opens read-only content for a selected storage.
- Network initially exposed a live Proxmox payload compatibility issue where network boolean fields arrived as numeric flags.
- Rebuilt app parses numeric/string/boolean network flags and renders the network interface list.
- User list opens from the dashboard and renders read-only user rows with disabled planned mutation actions.
- Dashboard quick actions now expose Backups and Cluster routes for beta smoke coverage.
- Backup list opens from the dashboard and renders read-only backup rows plus storage filtering.
- Backup disabled action labels remain readable on a Pixel-sized viewport.
- Cluster screen opens from the dashboard and renders standalone-node cluster status.
- Settings opens and shows the beta version plus disabled planned settings without implying runtime behavior.

## 2026-06-06 Rotation/Resume Source Audit

Source-level fixes:

- App-level session state now uses the AndroidX ViewModel store instead of Compose-only `remember`, reducing rotation risk for the authenticated session, current server, and cached nodes.
- Saved credential restore is initialized in an ordered Activity effect and skips rewriting the current server when the app session is already active.
- Login form drafts use saveable state for non-secret fields so host, port, username, realm, TLS, fingerprint, save-credentials, and API-token ID survive rotation before submission. Passwords and API token secrets remain in in-memory state instead of Android saved-instance-state bundles.
- Saved login prefill runs once per login composition, keeps restored non-secret drafts from being overwritten, and reloads memory-only saved password/API-token secret values from encrypted storage only when the restored non-secret draft still matches the saved credential entry.
- Task filter drafts for status, type, and VMID use saveable state so in-progress filter edits survive rotation before applying.

Emulator smoke evidence:

- Rebuilt debug APK installed over existing app data without clearing saved credentials.
- Saved login form remained populated after an Activity relaunch triggered by an attempted rotation.
- Saved login reached the dashboard after the Activity/ViewModel recreation fix.
- Latest rebuilt debug APK also reached the dashboard from encrypted saved credentials after reinstall with app data preserved.
- Dashboard remained visible and usable after Home/background plus launcher resume.
- Post-resume logcat scan found no fatal app crash entries.
- Forced landscape could not be counted as verified because the emulator returned to portrait after relaunch.
- Login screen was manually forced into landscape during a later emulator pass; the form remained scrollable and the submit button was reachable. This is partial login-screen coverage only, not a full route-matrix landscape pass.

## 2026-06-07 Focused Emulator And Instrumentation Pass

Environment:

- Pixel 8 API 36 emulator.
- Debug APK built from `revival-2026-baseline`.
- Private user-supplied Proxmox VE environment used only for already-authenticated route resume smoke.
- No private screenshots, hostnames, guest names, task IDs, fingerprints, or endpoint details recorded.

Automated checks:

```bash
./gradlew compileDebugAndroidTestKotlin connectedDebugAndroidTest
```

Result: passed with 30 instrumentation tests after fake-backed users, backups, cluster, and dashboard route smoke was added.

Observed pass:

- Login screen instrumentation renders the real `MainActivity` without saved credentials.
- Local API-token mode can be enabled from the login UI without contacting a Proxmox host.
- An invalid SHA-256 fingerprint shows validation copy and keeps the connect action disabled.
- A well-formed SHA-256 fingerprint allows the local form to become submittable when required API-token fields are present.
- Activity recreation preserves non-secret API-token login draft state and certificate fingerprint text.
- Activity recreation preserves unsaved task-filter type and VMID drafts before filters are applied.
- Activity recreation preserves a fake-authenticated post-login node-scoped task route and its route argument.
- Fake authenticated instrumentation can render Settings, server list, dashboard, tasks, node-scoped tasks, resource-filtered tasks, task detail, network, node-scoped network, storage, users, backups, and cluster route entry points without live Proxmox data.
- Fake-backed instrumentation can render populated node, VM, LXC, task detail, storage, node-scoped network, users, backups, cluster, and dashboard routes through the real navigation host without live Proxmox data.
- Fake task-detail instrumentation can render a synthetic task summary and log lines from a fake repository through the real task-detail route without live Proxmox data.
- Fake storage instrumentation can render a synthetic storage card, browse synthetic storage content, and show read-only content rows without live Proxmox data.
- Fake network instrumentation can render synthetic bridge and Ethernet interfaces for a node-scoped route without live Proxmox data.
- Fake users instrumentation can render synthetic enabled and disabled user rows without live Proxmox data.
- Fake backups instrumentation can render synthetic backup storage and read-only backup rows without live Proxmox data.
- Fake cluster instrumentation can render synthetic cluster quorum and node status rows without live Proxmox data.
- Fake dashboard instrumentation can render synthetic node and task summary data without live Proxmox data.
- Fake VM/LXC instrumentation can submit a synthetic lifecycle start action, receive a synthetic Proxmox-style UPID, show the persistent task handoff card, and navigate through the real View Task UI to the task-detail route without live Proxmox data.
- Fake VM/LXC instrumentation can recreate the Activity after a synthetic lifecycle task is returned, keep the persistent task handoff card through the retained ViewModel, and still navigate to the task-detail route without live Proxmox data.
- LXC detail route resumed from Home/launcher on the emulator with top app bar, loaded data, scroll position, read-only snapshot copy, and read-only resource copy still visible.
- Focused post-resume logcat scan found no fatal app crash entries.

Not counted as complete:

- Forced landscape route evidence was not counted because the emulator returned to the launcher during the attempt.
- The instrumentation pass does not prove live API-token authentication, live TLS/fingerprint connection success, live task-log loading, process-death session restore, or disposable lifecycle task handoff.
- The lifecycle task-handoff recreation smoke proves config-change retention only; it does not prove process-death persistence.

Still requires emulator/manual smoke:

- Rotate while logged in on dashboard and verify the app stays authenticated, the dashboard remains on screen, and refresh still works.
- Rotate on VM, LXC, storage, network, users, backups, tasks, task detail, cluster, and settings screens and verify the route, top app bar padding, scrollability, and loaded data remain usable.
- Rotate while typing unsaved login/task-filter drafts is covered by instrumentation; keep manual spot checks for visual clipping and keyboard behavior.
- Background and resume from dashboard, task detail, VM list, and LXC list and verify polling resumes without duplicated snackbar/task notices.
- Test one narrow/small-phone viewport and landscape mode for clipped top bars, wrapped action labels, and inaccessible bottom content.

Still pending:

| Blocker | Status | Public-safe evidence required |
| --- | --- | --- |
| API token login smoke | Pending | Auth method, Proxmox major version, APK type, pass/fail notes without token ID or username |
| TLS fingerprint/self-signed smoke | Pending | TLS mode, certificate flow outcome, pass/fail notes without private fingerprint values |
| Invalid credential error-state smoke | Pending | Auth mode and user-visible error behavior without attempted username or host |
| Invalid TLS error-state smoke | Pending | TLS mode and user-visible error behavior without endpoint details |
| VM lifecycle smoke on disposable guest | Pending | Action names, task handoff result, and guest type/name sanitized |
| LXC lifecycle smoke on disposable guest | Pending | Action names, task handoff result, and container type/name sanitized |
| Task detail/log handoff smoke after lifecycle actions | Pending | Whether returned task notice opens task detail/log without private UPID/log content |
| Route matrix rotation and background-resume smoke | Pending | Route name, orientation/resume result, and any clipping/crash notes without screenshots unless sanitized |
| Small-phone and landscape viewport smoke | Pending | Device/API class and route names with clipping/accessibility notes |

## Beta Evidence Capture Matrix

Record each pass with sanitized route names and outcomes only. Do not record real node names, guest names, VMIDs, UPIDs, usernames, hosts, IP addresses, fingerprints, tokens, or screenshots from private environments.

### Auth And TLS Smoke

| Scenario | Required evidence | Result |
| --- | --- | --- |
| Password login succeeds | APK type, Android device/API class, Proxmox VE major version, TLS mode, dashboard reached | Pending |
| Password login fails with invalid credentials | APK type, auth method, visible error behavior, no active session remains | Pending |
| API token login succeeds | APK type, Android device/API class, Proxmox VE major version, TLS mode, dashboard reached | Pending |
| API token login fails with invalid token | APK type, visible error behavior, no active session remains | Pending |
| Platform-trusted HTTPS succeeds | TLS mode, Proxmox VE major version, dashboard reached | Pending |
| Fingerprint-pinned self-signed HTTPS succeeds | TLS mode as fingerprint pin, visible certificate flow outcome, dashboard reached | Pending |
| Invalid fingerprint or invalid TLS fails clearly | TLS mode, visible error behavior, no active session remains | Pending |
| Release build blocks insecure trust-all mode | APK type as signed release or release candidate, visible behavior, no insecure connection path | Pending |

### Disposable Lifecycle Smoke

Run only against disposable lab guests. Capture action outcomes without guest names, IDs, task IDs, or task log content.

| Resource | Action | Required evidence | Result |
| --- | --- | --- | --- |
| VM | Start | Confirmation shown, action submitted once, returned task notice appears, task detail/log route opens | Pending |
| VM | Graceful shutdown | Confirmation shown, action submitted once, returned task notice appears, task detail/log route opens | Pending |
| VM | Force stop | Confirmation shown, action submitted once, returned task notice appears, task detail/log route opens | Pending |
| VM | Reboot | Confirmation shown, action submitted once, returned task notice appears, task detail/log route opens | Pending |
| VM | Delete stopped guest | Confirmation shown, delete submitted once, returned task notice appears, task detail/log route opens | Pending |
| VM | Delete running guest | Delete blocked with clear copy, no task notice is carried over | Pending |
| LXC | Start | Confirmation shown, action submitted once, returned task notice appears, task detail/log route opens | Pending |
| LXC | Graceful shutdown | Confirmation shown, action submitted once, returned task notice appears, task detail/log route opens | Pending |
| LXC | Force stop | Confirmation shown, action submitted once, returned task notice appears, task detail/log route opens | Pending |
| LXC | Reboot | Confirmation shown, action submitted once, returned task notice appears, task detail/log route opens | Pending |
| LXC | Delete stopped container | Confirmation shown, delete submitted once, returned task notice appears, task detail/log route opens | Pending |
| LXC | Delete running container | Delete blocked with clear copy, no task notice is carried over | Pending |

### Route Rotation And Resume Smoke

For every route, verify portrait, forced landscape where possible, Home/background plus launcher resume, back navigation, top app bar padding, scrollability, and loaded-data usability. Logcat should be scanned for fatal app crash entries after resume. Evidence should state `Pass`, `Fail`, or `Blocked` per route.

| Route | Rotation evidence | Resume evidence | Notes |
| --- | --- | --- | --- |
| Login | Pending | Pending | Include unsaved non-secret draft preservation |
| Dashboard | Pending | Pending | Include manual refresh after resume |
| Node detail | Pending | Pending | Include node context preservation |
| VM list | Pending | Pending | Include action labels and task notice behavior |
| VM detail | Pending | Pending | Include read-only config/snapshot areas |
| LXC list | Pending | Pending | Include action labels and task notice behavior |
| LXC detail | Pending | Pass | Portrait route resumed from Home/launcher with read-only snapshot/resource areas visible; no fatal crash entries after resume |
| Storage list | Pending | Pending | Include long content labels and capacity fields |
| Storage content browser | Pending | Pending | Include read-only content browsing |
| Network list | Pending | Pending | Include global and node-scoped entry paths |
| Users | Pending | Pending | Include disabled mutation actions |
| Backups | Pending | Pending | Include storage filtering and partial failure state if available |
| Tasks | Pending | Pending | Include global, node-scoped, and resource-filtered paths |
| Task detail/log | Pending | Pending | Include returned lifecycle task handoff after disposable smoke |
| Cluster | Pending | Pending | Include standalone or clustered response shape |
| Settings | Pending | Pending | Include beta version and disabled settings |

## Automation Candidates

The current beta blocker evidence is still mostly manual, but checked-in instrumentation smoke now covers local login rendering, API-token mode controls, fingerprint validation, Activity recreation for non-secret login and task-filter draft state, fake authenticated post-login route recreation, fake authenticated route-host entry points, fake-backed node/VM/LXC/task detail/storage/network/users/backups/cluster/dashboard route rendering, fake VM/LXC returned-task handoff to task-detail routes, and fake lifecycle task-handoff card persistence across Activity recreation. The narrowest next automatable steps are:

- Add additional instrumentation rotation/resume coverage for other post-login routes, preserving only non-secret fields across Activity recreation.
- Expand fake-backed route tests for empty, error, partial-data, and guarded-action states where those states materially affect beta confidence.
- Add fake-API Compose tests for VM/LXC blocked delete and failed-action task notice states.
- Keep API-token, TLS/fingerprint, and disposable lifecycle passes as manual or lab-backed tests until a disposable Proxmox fixture exists.

Media readiness:

- Private emulator screenshots from this pass must not be used as release media.
- Public release screenshots remain pending until they can be captured from a disposable lab or fully sanitized sample environment.
- Release media must avoid hostnames, IP addresses, usernames, backup notes, task log identifiers, tokens, tickets, cookies, certificate fingerprints tied to a private server, and local machine paths.

## Public Release Media Manifest

Use this manifest before adding screenshots or recordings to the README or GitHub Release notes. Leave entries as `Pending` until the file is captured from a disposable lab or fully sanitized sample environment.

| File | Screen or workflow | Source type | Sanitized identifiers | Caption or alt text | QA status |
| --- | --- | --- | --- | --- | --- |
| `login-tls.png` | Login with TLS/fingerprint controls | Disposable lab or fully sanitized capture | Pending | Pending | Pending |
| `dashboard.png` | Dashboard summary and node/task overview | Disposable lab or fully sanitized capture | Pending | Pending | Pending |
| `vm-detail.png` | VM list/detail with lifecycle and read-only data | Disposable lab or fully sanitized capture | Pending | Pending | Pending |
| `lxc-detail.png` | LXC list/detail with lifecycle and read-only data | Disposable lab or fully sanitized capture | Pending | Pending | Pending |
| `task-detail.png` | Returned task follow-up and task log view | Disposable lab or fully sanitized capture | Pending | Pending | Pending |
| `read-only-admin.png` | Storage, backup, cluster, network, or users read-only slice | Disposable lab or fully sanitized capture | Pending | Pending | Pending |

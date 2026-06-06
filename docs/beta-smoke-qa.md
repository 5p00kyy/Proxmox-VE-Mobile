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

Still requires emulator/manual smoke:

- Rotate while logged in on dashboard and verify the app stays authenticated, the dashboard remains on screen, and refresh still works.
- Rotate on VM, LXC, storage, network, users, backups, tasks, task detail, cluster, and settings screens and verify the route, top app bar padding, scrollability, and loaded data remain usable.
- Rotate while typing an unsaved login form and task filter form and verify drafts survive.
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

Media readiness:

- Private emulator screenshots from this pass must not be used as release media.
- Public release screenshots remain pending until they can be captured from a disposable lab or fully sanitized sample environment.
- Release media must avoid hostnames, IP addresses, usernames, backup notes, task log identifiers, tokens, tickets, cookies, certificate fingerprints tied to a private server, and local machine paths.

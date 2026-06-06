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
Automated release gate       [################....] 80%
Real Proxmox smoke QA        [########............] 40%
UX/copy release polish       [########............] 40%
Release packaging            [##..................] 10%
Official beta readiness      [##########..........] 50%
```

## Release Gates

All required gates must pass before tagging `v0.1.0-beta.1`.

| Gate | Required | Evidence |
| --- | --- | --- |
| Unit tests | Yes | `./gradlew test` passes locally or in CI |
| Lint | Yes | `./gradlew lint` passes locally or in CI |
| Debug build | Yes | `./gradlew assembleDebug` passes locally or in CI |
| Public branch hygiene | Yes | No machine-specific paths, hostnames, tokens, private IPs, or credentials in tracked docs/source |
| Real Proxmox login smoke | Yes | Password and API token login tested against a real Proxmox VE target |
| TLS smoke | Yes | Platform-trusted certificate and self-signed/fingerprint flow tested or documented as known limitation |
| VM/LXC lifecycle smoke | Yes | Start, graceful shutdown, force stop, reboot, and delete tested only on disposable guests |
| Task follow-up smoke | Yes | Returned task IDs open task detail/log state after VM/LXC actions |
| Navigation smoke | Yes | Dashboard, node detail, VM, LXC, storage, network, users, backups, tasks, cluster, settings do not crash during normal navigation |
| Known limitations | Yes | README and release notes clearly identify read-only and planned areas |
| Release notes | Yes | `CHANGELOG.md` has a `v0.1.0-beta.1` section before tagging |
| Release artifact | Yes | APK attached to GitHub release |

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
| VM | Start/shutdown/stop/reboot/delete prompt and submit on disposable guest | Yes |
| LXC | Container list loads and empty/error states render | Yes |
| LXC | Container detail loads status and snapshots read-only | Yes |
| LXC | Start/shutdown/stop/reboot/delete prompt and submit on disposable guest | Yes |
| Tasks | Task result links open detail/log screen | Yes |
| Tasks | Task filters do not crash with empty or partial data | Yes |
| Storage | Storage list and content browser load read-only | Yes |
| Network | Interface list loads for selected node | Yes |
| Users | User list loads read-only | Yes |
| Backups | Backup list loads and partial-storage failures are understandable | Yes |
| Cluster | Cluster status loads standalone and clustered responses | Yes |
| Settings | Planned settings are disabled or do not imply runtime behavior | Yes |

## Development Work Remaining

1. Open the pushed baseline branch as a draft PR.
2. Run the automated gate in CI and locally after any blocker fixes.
3. Execute the real Proxmox smoke matrix.
4. Fix only beta blockers on the baseline branch.
5. Add release screenshots or short screen recordings to the README/release notes.
6. Promote `CHANGELOG.md` from `Unreleased` to `v0.1.0-beta.1`.
7. Build and attach the beta APK to a GitHub release.
8. Keep a post-beta issue list for deferred parity work.

## Estimated Timeline

```text
Fast beta        2 days     Automated gate, one Proxmox smoke target, blocker-only fixes
Recommended beta 3-5 days   Smoke QA, UX/copy polish, screenshots, release notes
Careful beta     1-2 weeks  Multiple Proxmox versions, reverse proxy/TLS variants, broader device QA
```

The recommended path is `3-5 days`: enough real validation to avoid a brittle public release while keeping the project moving.

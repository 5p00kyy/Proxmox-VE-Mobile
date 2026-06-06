# Beta Smoke QA

This document records manual validation evidence for the `v0.1.0-beta.1` track. Do not record private hostnames, public IPs, usernames, tokens, screenshots with sensitive data, or other environment details that should not be public.

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

Still pending:

- API token login smoke.
- TLS fingerprint/self-signed smoke.
- Invalid credential and invalid TLS error-state smoke.
- VM/LXC lifecycle smoke on disposable guests.
- Task detail/log handoff smoke after lifecycle actions.
- Storage, network, users, backups, and cluster read-only navigation smoke.
- Small-screen/rotation/background-resume pass.

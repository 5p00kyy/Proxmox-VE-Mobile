# Proxmox VE Mobile Revival Plan

This plan assumes the current repository remains a single Android app and focuses on getting it trustworthy, testable, and shippable before adding major new features.

## What Exists Today

From code inspection, the project already has:

- authentication and saved credentials
- dashboard and node status loading
- VM and container listing with some lifecycle actions
- screens for tasks, storage, network, users, backups, cluster status, and settings
- a Compose-first UI and a working Gradle Android project layout

The app is past the idea stage. The main problem is not "start over". The main problem is that it stalled in the messy middle.

## Main Issues Holding It Back

### 1. Trustworthiness is unclear
There are many screens, but not all actions are fully wired. Right now the repo looks more complete than it actually is.

### 2. Architecture is under strain
The app is heavily centered around a large `MainViewModel` and direct API calls. That was a fine way to get moving, but it will become a drag as more real features land.

### 3. Security is not production-ready
The current networking code accepts any TLS certificate and hostname, and the manifest allows cleartext HTTP. That is acceptable for homelab prototyping, not a serious release.

### 4. No verification safety net
There are no committed tests and no CI. That makes cleanup and feature work riskier than it needs to be.

### 5. Scope is too wide for current confidence
The app already exposes many surfaces. Before adding more, the existing ones need to be verified and either hardened or cut back.

## Recommended Revival Strategy

### Phase 1: Reality Pass
Goal: turn assumptions into facts.

Do this on the desktop with Android Studio and a real Proxmox instance available.

For each screen, mark it as one of:

- works end-to-end
- loads data but actions are incomplete
- placeholder or misleading
- broken

Suggested verification checklist:

1. Login with password auth
2. Saved credentials and relaunch flow
3. Dashboard data freshness
4. VM list and start/stop/delete
5. Container list and start/stop/delete
6. Tasks screen refresh and task accuracy
7. Storage/network/users/backups/cluster/settings screens
8. Error handling for unreachable host, bad credentials, and TLS issues

Deliverable:

- a checked current-state matrix in the repo
- screenshots of what is genuinely working
- a short list of broken or misleading surfaces to cut or fix

### Phase 2: Honest Scope Tightening
Goal: reduce fake breadth.

After the reality pass, either:

- fully support a screen/action, or
- hide/remove it until it is real

If this were my call, I would bias the first "serious" version toward:

- login/session handling
- dashboard
- VM list plus safe lifecycle actions
- container list plus safe lifecycle actions
- tasks/history
- minimal settings

Everything else should earn its place.

### Phase 3: Architecture Cleanup
Goal: make future work cheaper.

Recommended sequence:

1. Split `MainViewModel` into feature-specific view-models
2. Introduce repositories between UI and Retrofit services
3. Extract common polling/refresh patterns
4. Separate read models from action/request models where useful
5. Centralize API client creation and auth/session handling

Do not do a big-bang rewrite. Move one vertical slice at a time.

### Phase 4: Security Hardening
Goal: stop treating the app like a prototype.

Priority items:

1. Remove trust-all TLS and hostname verification
2. Decide on certificate strategy for homelab use
3. Revisit `usesCleartextTraffic`
4. Review credential storage and session expiry behavior
5. Add explicit operator messaging when the app is in insecure compatibility mode

### Phase 5: Automated Verification
Goal: make refactoring safe.

Minimum worthwhile baseline:

- unit tests for auth/session parsing and API mapping
- unit tests for key view-model state transitions
- one or two instrumentation or screenshot tests for major screens
- GitHub Actions CI for build, lint, and tests

### Phase 6: Product Direction
Goal: decide what this app is trying to be.

There are at least three plausible directions:

1. **Personal homelab companion**
   - optimized for your own Proxmox usage
   - fastest path
   - least ceremony

2. **Polished open-source mobile client**
   - smaller, tighter feature set
   - stronger UX and reliability focus
   - better docs and contributor setup

3. **Power-user operator app**
   - task visibility, cluster awareness, deeper management flows
   - more demanding architecture and testing needs

I would start with option 1 and build it in a way that could later become option 2.

## Suggested Near-Term Backlog

### Immediate
- verify real behavior in Android Studio
- create a current-state matrix of screens and actions
- prune or hide misleading unfinished actions
- add CI scaffold once local builds are confirmed

### Next
- split the first feature out of `MainViewModel`
- harden TLS handling
- add the first unit tests
- tighten settings to only show real behavior

### Later
- console access
- backup workflows
- multi-server support
- richer task and alerting surfaces

## What I Would Avoid

- adding more screens before verification
- a full architecture rewrite before confirming scope
- pretending insecure TLS defaults are acceptable long-term
- keeping half-implemented actions visible just because they look ambitious

## Practical Working Model

Use the two-seat workflow:

- **Desktop:** Android Studio, emulator/device, logcat, UI inspection, real manual verification
- **OpenClaw host:** refactors, docs, reviews, CI setup, code cleanup, smaller implementation slices

That split gives the project a real development loop without forcing all Android work into a headless container.

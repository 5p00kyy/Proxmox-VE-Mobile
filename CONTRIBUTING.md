# Contributing

Thanks for helping improve Proxmox VE Mobile. This project is being rebuilt from a broad prototype into a reliable, secure, mobile-native Proxmox operator app.

## Current Priorities

The immediate priority is not adding every missing Proxmox screen. The project first needs:

1. Secure session and API client ownership.
2. CI, tests, and repeatable QA.
3. A clean VM vertical slice using repository + ViewModel architecture.
4. Task tracking after Proxmox lifecycle actions.
5. Honest UI that does not expose unfinished actions as working features.

See `docs/development-cycle-2026.md` for the full roadmap.

## Local Setup

Requirements:

- JDK 17
- Android SDK with compile SDK 34 available
- Android Studio or command-line Android SDK tools

Useful commands:

```bash
./gradlew test
./gradlew lint
./gradlew assembleDebug
```

If your shell default Java is not JDK 17, set `JAVA_HOME` before running Gradle.

## Pull Request Workflow

Branch model:

- `main` is reserved for public release readiness, beta tags, and release hotfixes.
- `dev` is the normal integration target for the next beta cycle.
- Use short-lived `feature/*` and `fix/*` branches for focused work.
- Merge `dev` into `main` only when the next beta is ready to gate, tag, and publish.
- Branch hotfixes from `main`, then merge the fix back into both `main` and `dev`.

PR expectations:

1. Target `dev` unless the change is a release hotfix.
2. Keep PRs focused on one feature, fix, or refactor.
3. Link the issue when one exists.
4. Run the relevant Gradle checks before opening the PR.
5. Include screenshots or screen recordings for UI changes.
6. State the Proxmox version and Android device/emulator used for manual QA.
7. Call out security, auth, TLS, destructive action, and localization impact.

## Engineering Standards

- Composables render state and emit events.
- ViewModels own screen state and user events.
- Repositories own API/storage calls and DTO mapping.
- Do not create Retrofit or OkHttp clients directly from screens.
- Routes should carry resource identity instead of guessing from cached state.
- User-facing strings belong in Android string resources.
- Destructive or disruptive actions need confirmation and task follow-up.
- Auth headers, tickets, cookies, API tokens, passwords, and private keys must never be logged.

## Localization

The project currently has English, German, and Spanish string resources. When adding or changing a string:

- Add the key to every locale file.
- Preserve placeholders and format specifiers.
- Avoid hardcoded user-facing text in Kotlin.
- Keep error messages specific enough to be actionable.

## Testing Expectations

For now, at minimum run:

```bash
./gradlew test lint assembleDebug
```

As the architecture refactor progresses, new feature work should include focused unit tests for repositories, mappers, and ViewModels. UI-critical changes should include Compose tests or clearly documented manual QA.

## Security-Sensitive Changes

Security-sensitive changes include auth, saved credentials, TLS, logging, network clients, Proxmox task submission, destructive actions, and backup/restore flows. Treat those PRs as higher risk and include extra QA notes.

# Android Instrumentation Smoke Tests

These tests are intended to be checked in and run from Android Studio or Gradle on a local emulator/device.

Run the full instrumentation smoke set from the command line:

```bash
./gradlew connectedDebugAndroidTest
```

In Android Studio, open this directory or an individual `*SmokeTest` class and use the gutter run action while an emulator/device is connected.

The checked-in smoke tests use placeholder fixtures such as `example.test`, `demo.example.test`, `tester`, synthetic task IDs, and in-memory fake repositories. They should not require or contact a Proxmox host. Some tests launch the debug app and clear `proxmox_secure_prefs`, so run them before or after manual logged-in QA sessions instead of during an active session.

Live Proxmox validation belongs in the manual beta smoke matrix in `docs/beta-smoke-qa.md`. Record only device class, Proxmox major version, auth/TLS mode, route/action names, and sanitized pass/fail notes.

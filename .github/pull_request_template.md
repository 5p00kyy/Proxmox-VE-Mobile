## Summary

Describe what changed and why.

## Screenshots

Add screenshots or screen recordings for UI changes. Use "N/A" for non-UI changes.

For release or README media, use a disposable lab or sanitize all hostnames, IP addresses, usernames, backup notes, task log identifiers, tokens, cookies, tickets, certificate fingerprints, and local machine paths.

## QA

Commands run:

- [ ] `./gradlew test`
- [ ] `./gradlew lint`
- [ ] `./gradlew assembleDebug`
- [ ] Manual app test
- [ ] Not run, reason:

Beta release checks:

- [ ] Version name/code reviewed for release.
- [ ] Known limitations updated.
- [ ] Changelog/release notes updated.
- [ ] Screenshots or release media updated, or intentionally deferred.
- [ ] Release media is public-safe and does not expose private infrastructure details.
- [ ] Beta smoke matrix updated when release behavior changes.

Test environment:

- Android/device:
- Proxmox VE version:
- Standalone or cluster:
- Auth method:

Avoid recording private hostnames, IP addresses, usernames, or tokens in the public PR body.

## Risk

- [ ] Security-sensitive change
- [ ] Auth/session change
- [ ] Network/TLS change
- [ ] Destructive Proxmox action
- [ ] Database/storage/preferences change
- [ ] Localization change
- [ ] No unusual risk

Notes:

## Checklist

- [ ] User-facing strings are in Android resources.
- [ ] New or changed destructive actions have confirmation and task follow-up.
- [ ] Errors distinguish auth, permission, network/TLS, validation, and unknown failures where practical.
- [ ] Placeholder or unfinished UI is not presented as functional.
- [ ] Docs or changelog were updated when user-visible behavior changed.

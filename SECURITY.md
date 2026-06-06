# Security Policy

Proxmox VE Mobile manages infrastructure resources, credentials, API tokens, and destructive actions. Security issues should be handled carefully and privately.

## Supported Versions

The project has not reached a stable public release yet. Until a beta or stable release is tagged, security fixes target the `main` branch.

| Version | Supported |
| --- | --- |
| `main` | Yes |
| Tagged releases before beta | No |

## Reporting A Vulnerability

Do not open a public issue for:

- Credential leaks.
- API token, ticket, cookie, or CSRF handling bugs.
- TLS validation bypasses.
- Insecure credential storage.
- Destructive action bypasses.
- Backup/restore vulnerabilities.
- Logs that expose secrets.

Preferred reporting path:

1. Use GitHub private vulnerability reporting or a private security advisory for this repository when available.
2. If private reporting is not available, open a minimal public issue asking for a private security contact. Do not include exploit details, tokens, logs, hostnames, or private IP details.

Reports should include:

- Affected app version or commit.
- Android version and device, if relevant.
- Proxmox VE version.
- Authentication method involved.
- Whether the connection used direct LAN/VPN, reverse proxy, HTTP, self-signed TLS, or imported CA.
- Minimal reproduction steps.
- Impact and suggested fix, if known.

## Current Known Security Gaps

The current prototype has known security gaps and follow-up work:

- SHA-256 certificate fingerprint pinning is available for trusted self-signed Proxmox servers and should be preferred over disabling verification.
- The insecure TLS fallback is restricted to debug builds as an explicit opt-in for trusted lab servers. Release builds require platform TLS validation or a configured SHA-256 certificate fingerprint.
- Debug builds still allow cleartext for local testing. Release builds no longer request app-wide cleartext traffic, but the product policy for HTTP should be tightened before beta.
- API token login is available, but token permissions and onboarding guidance still need documentation.
- Logging now redacts auth headers and avoids body logs, but formal redaction tests are still missing.

These are documented so they can be fixed before public beta. Please do not treat the current debug build as production-ready.

## TLS For Self-Signed Proxmox Servers

Release builds should use one of these trust paths:

1. Use a certificate issued by a CA trusted by Android.
2. Import the issuing private CA into Android's trusted credential store, then keep SSL verification enabled.
3. Enter the Proxmox server certificate SHA-256 fingerprint in the login form.

For a direct Proxmox server certificate, verify the fingerprint from a trusted machine or the Proxmox host itself:

```bash
openssl s_client -connect HOST:8006 -servername HOST </dev/null 2>/dev/null \
  | openssl x509 -noout -fingerprint -sha256
```

The value may be entered with or without colons and with or without the `SHA256/` prefix. The app normalizes it before matching the server certificate.

Do not use trust-on-first-use silently for this app. If TOFU is added later, it should show the fingerprint, require an explicit confirmation, store the accepted certificate identity, and warn clearly when the certificate changes.

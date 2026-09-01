# Security Policy

## Supported version

Security fixes are expected to target the latest version on `main` and the latest GitHub Release.

Older debug builds and historical tags are not actively supported unless explicitly stated.

## Reporting a vulnerability

If you find a security issue:

1. Do not include API keys, tokens, private Base URLs, keystores, passwords, backups or personal schedule data in a public issue.
2. Create a minimal report describing the affected feature, expected impact and reproduction steps.
3. If the report requires private data, first contact the repository owner through GitHub and agree on a safe channel.

## Secret handling rules

Never commit:

- `keystore.properties`
- `release/PaykiTodo-release.jks`
- `*.jks`, `*.keystore`, `*.p12`, `*.pem`, `*.key`
- `.env` or `.env.*`
- APK signing passwords
- AI API keys
- private Base URLs
- desktop sync tokens
- cloud access / refresh tokens and account snapshot secrets
- administrator credentials and private cloud deployment files
- cloud SQLite databases
- real user backups or logs containing personal data

The repository `.gitignore` already covers the common local secret and build-artifact paths. Verify before committing:

```powershell
git status --short
git check-ignore -v keystore.properties release/PaykiTodo-release.jks
```

## Desktop sync warning

Desktop sync exposes a local web service on the phone while enabled. Use it only on trusted networks and disable it when not needed.

## Account sync warning

The account API should listen only on loopback (default `127.0.0.1:18766`) and be exposed through an HTTPS reverse proxy. Do not expose the backend port directly. Keep the real environment file, administrator password, password pepper, hostname, certificate and cloud database outside Git. Configure Cloudflare/Nginx rate limiting before opening registration publicly.

The current synchronization contract blocks revision conflicts but does not automatically merge concurrent field edits. A conflict must never be bypassed by manually forcing `If-Match` without first deciding which local copy should win.

## AI provider warning

AI recognition and reports may send selected text to the configured third-party provider. Use providers you trust, and avoid sending highly sensitive personal information.

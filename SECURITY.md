# Security Policy

## Supported Versions

| Version | Supported          |
|:--------|:-------------------|
| 1.x     | ✅ Active support  |

---

## Reporting a Vulnerability

**Please do not report security vulnerabilities through public GitHub issues.**

If you discover a security vulnerability in Secret Manager, we appreciate your help in disclosing it responsibly.

### How to Report

Send an email to: **[security@mehrez-ben-salem.com](mailto:mehrez.bensalem.mobile@gmail.com)**

Please include:

1. **Description** of the vulnerability
2. **Steps to reproduce** or a proof-of-concept
3. **Impact assessment** — what an attacker could achieve
4. **Affected versions** (if known)
5. **Suggested fix** (if you have one)

### What to Expect

- **Acknowledgement** within **48 hours**
- **Initial assessment** within **5 business days**
- **Regular updates** as we work on a fix
- **Credit** in the release notes (unless you prefer to remain anonymous)

### What We Ask

- Give us reasonable time to address the issue before public disclosure
- Do not exploit the vulnerability beyond what is necessary to demonstrate it
- Do not access or modify data belonging to other users

---

## Security Design Principles

Secret Manager is built with the following security guarantees:

| Principle                                 | Implementation                                            |
|:------------------------------------------|:----------------------------------------------------------|
| **Secrets encrypted at rest in cache**    | AES-256-GCM with unique IV per entry                      |
| **Rotation detection without decryption** | SHA-256 hash comparison only                              |
| **Master key never hardcoded**            | Sourced from environment variables                        |
| **No secret logging**                     | All values masked in log output                           |
| **TTL-based eviction**                    | Stale secrets are automatically removed from cache        |
| **Zero vendor SDK dependency**            | Pure Java HTTP clients reduce supply-chain attack surface |

For a detailed overview, see [docs/security.md](docs/security.md).

---

## Scope

This policy applies to the Secret Manager library itself. It does **not** cover:

- Vulnerabilities in upstream vault systems (CyberArk Conjur, HashiCorp Vault, etc.)
- Vulnerabilities in the Java runtime or Spring Framework
- Misconfiguration by end users (e.g., committing secrets to source control)

If you're unsure whether something is in scope, please report it anyway — we'd rather investigate and close than miss a real issue.

---

Thank you for helping keep Secret Manager secure. 🔐

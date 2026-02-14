# Security Model

> How Secret Manager protects secrets at every stage — from vault retrieval to application delivery.

---

## Table of Contents

- [Threat Model](#threat-model)
- [Encryption at Rest (in Cache)](#encryption-at-rest-in-cache)
- [Rotation Detection](#rotation-detection)
- [Key Management](#key-management)
- [Logging and Audit](#logging-and-audit)
- [Supply Chain](#supply-chain)
- [Compliance Considerations](#compliance-considerations)

---

## Threat Model

| Threat | Mitigation |
|:-------|:-----------|
| Memory dump reveals secrets | All cached values encrypted with AES-256-GCM |
| Stale credentials in use | TTL-based cache eviction + automatic rotation detection |
| Vault migration exposes secrets | Abstraction layer — application code never references a specific vault |
| Log files contain secrets | All secret values masked in log output |
| Compromised encryption key | Key sourced from environment, not stored in code or config files |

---

## Encryption at Rest (in Cache)

Every secret stored in the cache is wrapped in an `EncryptedSecret` object:

- **Algorithm:** AES-256-GCM (authenticated encryption with associated data)
- **IV:** Unique random initialization vector per cache entry (12 bytes)
- **Hash:** SHA-256 digest of the plaintext for rotation comparison
- **TTL:** Expiration timestamp for automatic eviction

Secrets are decrypted **only at the moment of retrieval** and delivered as a plaintext `String` to the calling code. The plaintext is never persisted.

---

## Rotation Detection

The rotation detector compares the **SHA-256 hash** of the current vault value against the hash stored in the cache. This means:

- No decryption is needed to detect a change
- The encrypted cache entry is only updated when a rotation is confirmed
- A `SecretRotationEvent` is published for application-level handling

---

## Key Management

The AES-256 master key is sourced from:

1. `secrets.encryption.key` property (Base64-encoded)
2. `SECRETS_ENCRYPTION_KEY` environment variable
3. Auto-generated at startup (development only)

**Production guidance:**
- Always set the key explicitly via environment variable or secrets management in your orchestrator (Kubernetes Secrets, OpenShift, etc.)
- Rotate the encryption key by restarting the application with a new key value — the cache will be repopulated from the vault
- Never commit the encryption key to source control

---

## Logging and Audit

- Secret values are **never written to logs** at any log level
- Secret paths and metadata (TTL, cache hit/miss) may be logged at `DEBUG` level
- Rotation events are logged at `INFO` level with the secret path (not the value)

---

## Supply Chain

- The Conjur provider uses Java's built-in `HttpClient` — **no vendor SDK dependency**
- This minimizes the attack surface from transitive dependencies
- All modules depend only on Spring Boot and the JDK standard library

---

## Compliance Considerations

Secret Manager's design supports compliance with:

- **DORA** (Digital Operational Resilience Act) — automated rotation detection reduces manual operational risk
- **GDPR / RGPD** — encrypted caching and secret isolation support data protection requirements
- **PCI DSS** — AES-256 encryption, no plaintext storage, key management via environment variables
- **SOC 2** — audit-friendly logging, automated rotation, and access abstraction

> **Note:** Secret Manager is a library, not a certified product. Compliance depends on how it is deployed and configured within your broader infrastructure.

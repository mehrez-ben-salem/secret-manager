# Configuration Guide

> Complete reference for all Secret Manager properties and vault provider settings.

---

## Table of Contents

- [Core Properties](#core-properties)
- [Vault Providers](#vault-providers)
  - [CyberArk Conjur](#cyberark-conjur)
  - [Map Vault (Development)](#map-vault-development)
  - [Mock Vault (Testing)](#mock-vault-testing)
- [Cache Settings](#cache-settings)
- [Rotation Detection](#rotation-detection)
- [Encryption](#encryption)
- [Environment Variables](#environment-variables)

---

## Core Properties

| Property | Default | Description |
|:---------|:--------|:------------|
| `secrets.enabled` | `true` | Master switch — set to `false` to disable the entire library |
| `secrets.provider` | `conjur-vault` | Active vault provider (must match `getProviderName()`) |
| `secrets.cache.type` | `memory` | Active cache backend (must match `getType()`) |
| `secrets.cache.default-ttl` | `3600` | Default cache TTL in seconds. `-1` disables expiration |
| `secrets.encryption.key` | *auto-generated* | Base64-encoded AES-256 key for cache encryption |
| `secrets.rotation-detection.enabled` | `true` | Enable automatic rotation detection |
| `secrets.rotation-detection.check-interval` | `300000` | Rotation check interval in milliseconds (default: 5 min) |

---

## Vault Providers

### CyberArk Conjur

```yaml
secrets:
  provider: conjur-vault
  conjur-vault:
    url: ${CONJUR_URL}
    account: ${CONJUR_ACCOUNT}
    authn-login: ${CONJUR_AUTHN_LOGIN}
    api-key: ${CONJUR_API_KEY}
    ssl-verify: true
```

| Property | Required | Description |
|:---------|:---------|:------------|
| `secrets.conjur-vault.url` | Yes | Conjur server base URL |
| `secrets.conjur-vault.account` | Yes | Conjur account name |
| `secrets.conjur-vault.authn-login` | Yes | Authentication identity (e.g., `host/my-app`) |
| `secrets.conjur-vault.api-key` | Yes | API key — **always use an environment variable** |
| `secrets.conjur-vault.ssl-verify` | No | Verify TLS certificates (default: `true`) |

### Map Vault (Development)

Ideal for local development — secrets are defined directly in your YAML configuration.

```yaml
secrets:
  provider: map-vault
  map-vault:
    entry-set:
      - key: "database/prod/password"
        value: "dev-password-123"
      - key: "api/stripe/key"
        value: "sk_test_xxx"
```

### Mock Vault (Testing)

Extends Map Vault with a `updateSecret()` method for simulating rotation in tests.

```yaml
secrets:
  provider: mock-vault
  mock-vault:
    entry-set:
      - key: "database/prod/password"
        value: "test-password"
```

---

## Cache Settings

| Property | Default | Description |
|:---------|:--------|:------------|
| `secrets.cache.type` | `memory` | Cache backend. Built-in: `memory` |
| `secrets.cache.default-ttl` | `3600` | Time-to-live in seconds. `-1` = never expire |

The in-memory cache encrypts all values with AES-256-GCM before storing them in a `ConcurrentHashMap`.

---

## Rotation Detection

| Property | Default | Description |
|:---------|:--------|:------------|
| `secrets.rotation-detection.enabled` | `true` | Enable/disable rotation polling |
| `secrets.rotation-detection.check-interval` | `300000` | Polling interval in ms (default: 5 min) |

When a rotation is detected, a `SecretRotationEvent` is published via Spring's event system. Your application reacts with a standard `@EventListener`.

---

## Encryption

| Property | Default | Description |
|:---------|:--------|:------------|
| `secrets.encryption.key` | *auto-generated* | Base64-encoded 256-bit AES key |

**Important:** In production, always set this explicitly via an environment variable:

```bash
export SECRETS_ENCRYPTION_KEY="your-base64-encoded-256-bit-key"
```

If not set, a random key is generated at startup. This means the cache cannot survive a restart (all cached secrets will need to be re-fetched from the vault).

---

## Environment Variables

All properties can be set via environment variables using Spring Boot's relaxed binding:

| Property | Environment Variable |
|:---------|:--------------------|
| `secrets.provider` | `SECRETS_PROVIDER` |
| `secrets.conjur-vault.url` | `SECRETS_CONJUR_VAULT_URL` |
| `secrets.conjur-vault.api-key` | `SECRETS_CONJUR_VAULT_API_KEY` |
| `secrets.encryption.key` | `SECRETS_ENCRYPTION_KEY` |
| `secrets.cache.default-ttl` | `SECRETS_CACHE_DEFAULT_TTL` |

This is the recommended approach for production deployments — **never commit vault credentials to source control**.

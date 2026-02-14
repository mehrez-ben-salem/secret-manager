# Frequently Asked Questions

> Common questions, troubleshooting tips, and practical guidance.

---

## General

### What is Secret Manager?

Secret Manager is a Spring Boot library that provides a unified way to access secrets (passwords, API keys, certificates) from any vault provider. It handles caching, encryption, and automatic rotation detection so your application code doesn't have to.

### Who is it for?

Any team building Spring Boot applications that need to access secrets from a vault — especially teams that want to avoid vendor lock-in, eliminate boilerplate, or improve security posture.

### Does it replace my vault?

No. Secret Manager sits **between** your application and your vault. It provides a uniform API and handles caching, encryption, and rotation. You still need a vault (CyberArk Conjur, HashiCorp Vault, Azure Key Vault, etc.) to store the actual secrets.

---

## Usage

### How do I use secrets in YAML configuration?

Use the `${secret://path}` syntax:

```yaml
spring:
  datasource:
    password: ${secret://database/prod/password}
```

### How do I use secrets in Java code?

Use the `@Secret` annotation:

```java
@Secret(path = "api/stripe/key")
private String stripeApiKey;
```

### Can I use both `${secret://}` and `@Secret` in the same application?

Yes. They are complementary — `${secret://}` resolves during property processing (early), while `@Secret` injects after bean creation (later).

### What happens if the vault is unavailable at startup?

The application will fail to start if it cannot resolve required secrets. This is intentional — running with missing secrets is a security risk. Use the `map-vault` provider for development and testing scenarios.

---

## Rotation

### How does rotation detection work?

A scheduled task (configurable interval) fetches the current value from the vault and compares its SHA-256 hash against the cached hash. If they differ, the cache is updated and a `SecretRotationEvent` is published.

### Do I need to restart my application when a secret rotates?

No. That's the whole point. Listen for `SecretRotationEvent` and react programmatically — for example, refreshing a database connection pool.

### What's the default check interval?

5 minutes (300,000 milliseconds). You can change it via `secrets.rotation-detection.check-interval`.

---

## Security

### Are secrets stored in plaintext in the cache?

No. All cached secrets are encrypted with AES-256-GCM using a unique initialization vector per entry.

### Where is the encryption key stored?

It should be provided via the `SECRETS_ENCRYPTION_KEY` environment variable (or `secrets.encryption.key` property). If not set, a random key is generated at startup — suitable for development but not for production.

### Are secret values ever logged?

No. Secret Manager masks all secret values in log output at every log level.

---

## Troubleshooting

### My `${secret://}` placeholders aren't resolving

1. Verify that `secrets.enabled` is `true` (default)
2. Verify that the provider is configured and the vault is reachable
3. Check that the secret path is correct (paths are case-sensitive)
4. Look for error messages in the startup log

### Rotation events aren't firing

1. Verify `secrets.rotation-detection.enabled` is `true`
2. Confirm the check interval isn't set too high
3. Ensure the secret was actually changed in the vault (not just in the cache)
4. Check that your `@EventListener` method accepts `SecretRotationEvent`

### How do I debug vault connectivity issues?

Set the log level to `DEBUG`:

```yaml
logging:
  level:
    edu.m4z.secrets: DEBUG
```

This will show vault requests, cache operations, and rotation checks (without revealing secret values).

# Architecture Guide

> A deep dive into how Secret Manager is structured, how data flows, and why each design decision was made.

---

## Table of Contents

- [Design Philosophy](#design-philosophy)
- [Module Structure](#module-structure)
- [Data Flow](#data-flow)
- [SPI Plugin Model](#spi-plugin-model)
- [Auto-Configuration](#auto-configuration)
- [Event Model](#event-model)

---

## Design Philosophy

Secret Manager is designed around three core principles:

1. **Zero friction** — developers should not need to learn a new API or change their coding habits. Secrets appear as regular Spring properties or annotated fields.

2. **Vault independence** — switching from CyberArk Conjur to HashiCorp Vault (or any other provider) should require changing a single configuration line, not refactoring application code.

3. **Security by default** — secrets are encrypted in cache, never logged in plaintext, and rotation is detected and propagated automatically.

---

## Module Structure

```
secret-manager/
├── core/                  # The engine — SPI contracts, auto-config, encryption, rotation
├── memory-cache/          # In-memory AES-encrypted cache implementation
├── map-vault/             # YAML-driven vault for development
├── mock-vault/            # Testable vault with rotation simulation
├── conjur-vault/          # CyberArk Conjur provider (pure Java HTTP)
└── demo/                  # Full Spring Boot demo application
```

Each module is independently versioned and can be included or excluded based on need. The `core` module is always required; everything else is optional.

---

## Data Flow

### Secret Resolution (startup)

```
application.yml                     @Secret annotation
       │                                   │
       ▼                                   ▼
SecretPropertySource              SecretAnnotationProcessor
       │                                   │
       └──────────┐         ┌──────────────┘
                  ▼         ▼
              SecretService (facade)
                     │
            ┌────────┴────────┐
            ▼                 ▼
       SecretCache    SecretVaultProvider
       (encrypted)       (SPI lookup)
```

### Secret Rotation (runtime)

```
RotationDetector (scheduled)
       │
       ▼
  Fetch from vault ──► Compare SHA-256 hash with cache
       │
       ├── No change ──► Do nothing
       │
       └── Changed ──► Update cache ──► Publish SecretRotationEvent
                                                │
                                                ▼
                                     Your @EventListener
```

---

## SPI Plugin Model

Secret Manager uses Java's `ServiceLoader` mechanism, not Spring component scanning. This means:

- No `@Component` or `@Bean` annotations on providers
- No classpath scanning overhead
- Providers are discovered at startup via `META-INF/services/` files
- Multiple providers can coexist — the active one is selected by configuration

This approach was chosen deliberately to keep the plugin boundary clean and framework-independent.

---

## Auto-Configuration

Secret Manager integrates with Spring Boot's auto-configuration lifecycle:

1. **`EnvironmentPostProcessor`** — runs before bean creation to resolve `${secret://}` placeholders in property sources
2. **`AutoConfiguration`** — creates the `SecretService`, `SecretCache`, and `RotationDetector` beans
3. **`BeanPostProcessor`** — injects `@Secret`-annotated fields after bean instantiation

This ordering ensures that secrets are available both in configuration properties (resolved early) and in bean fields (resolved later).

---

## Event Model

When the `RotationDetector` detects a changed secret:

1. It fetches the new value from the vault
2. Updates the encrypted cache
3. Publishes a `SecretRotationEvent` via Spring's `ApplicationEventPublisher`

Application code listens with standard `@EventListener` methods — no Secret Manager-specific interfaces required.

```java
@EventListener
public void onRotation(SecretRotationEvent event) {
    // event.getSecretPath()  — which secret changed
    // event.getNewValue()    — the new value (already cached)
    // event.getTimestamp()   — when the rotation was detected
}
```

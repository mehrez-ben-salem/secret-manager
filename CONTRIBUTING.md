# Contributing to Secret Manager

First off, **thank you** for considering a contribution! Every bug report, feature idea, documentation fix, or code improvement makes Secret Manager better for everyone.

---

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [How Can I Contribute?](#how-can-i-contribute)
- [Reporting Bugs](#reporting-bugs)
- [Suggesting Features](#suggesting-features)
- [Development Setup](#development-setup)
- [Pull Request Process](#pull-request-process)
- [Coding Standards](#coding-standards)
- [Adding a Vault Provider](#adding-a-vault-provider)
- [Adding a Cache Backend](#adding-a-cache-backend)

---

## Code of Conduct

This project follows a simple rule: **be kind, be constructive, be professional.** We are all here to build something useful together.

---

## How Can I Contribute?

| Contribution | Where to Start |
|:-------------|:---------------|
| 🐛 Found a bug | [Open a bug report](#reporting-bugs) |
| 💡 Have an idea | [Suggest a feature](#suggesting-features) |
| 📖 Improve docs | Fork → edit → PR (no issue needed for typos) |
| 🔌 New vault provider | See [Adding a Vault Provider](#adding-a-vault-provider) |
| 🧪 Write tests | Always welcome — especially integration tests |

---

## Reporting Bugs

Please [open an issue](https://github.com/your-org/secret-manager/issues/new) with:

1. **What you expected** to happen
2. **What actually happened** (include stack traces if applicable)
3. **Steps to reproduce** (minimal example preferred)
4. **Environment**: Java version, Spring Boot version, vault provider, OS

> ⚠️ **Security vulnerabilities** should be reported privately. See [SECURITY.md](SECURITY.md).

---

## Suggesting Features

Open an issue tagged `enhancement` with:

1. **The problem** you're trying to solve
2. **Your proposed solution** (even a rough sketch is helpful)
3. **Alternatives** you considered

---

## Development Setup

### Prerequisites

- Java 21+
- Maven 3.9+
- Git

### Build

```bash
git clone https://github.com/your-org/secret-manager.git
cd secret-manager
mvn clean install
```

### Run the Demo

```bash
cd demo
mvn spring-boot:run
```

### Run Tests

```bash
mvn test                    # Unit tests
mvn verify                  # Unit + integration tests
```

---

## Pull Request Process

1. **Fork** the repository and create a branch from `main`:
   ```bash
   git checkout -b feature/my-improvement
   ```

2. **Make your changes** — keep commits focused and well-described.

3. **Add tests** for any new functionality.

4. **Ensure all tests pass**:
   ```bash
   mvn clean verify
   ```

5. **Submit a pull request** against `main` with:
   - A clear title (e.g., `feat: add Azure Key Vault provider`)
   - A description of what changed and why
   - A reference to the related issue (if any)

### Commit Message Convention

We follow [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: add Azure Key Vault provider
fix: handle null TTL in cache eviction
docs: update configuration reference
refactor: simplify SPI loader initialization
test: add rotation detection integration test
```

---

## Coding Standards

- **Java 21** features are welcome (records, sealed classes, pattern matching, etc.)
- Follow the existing code style — consistent formatting matters more than any specific style
- **No secret values in tests** — use `mock-vault` or `map-vault`
- **No external SDK dependencies** in vault providers when a pure Java HTTP approach is viable
- Javadoc on all public interfaces and SPI contracts
- Meaningful log messages at appropriate levels (never log secret values)

---

## Adding a Vault Provider

New vault providers are the most impactful contribution you can make. Here's the process:

1. **Create a new Maven module** (e.g., `azure-vault/`)
2. **Implement** `SecretVaultProvider`:
   ```java
   public class AzureKeyVaultProvider implements SecretVaultProvider {
       @Override public String getProviderName() { return "azure-keyvault"; }
       @Override public void initialize(ConfigurableEnvironment env) { /* ... */ }
       @Override public String getSecret(String path) throws SecretNotFoundException { /* ... */ }
       @Override public boolean isAvailable() { return true; }
   }
   ```
3. **Register via SPI** in `META-INF/services/edu.m4z.secrets.provider.SecretVaultProvider`
4. **Add configuration examples** in the module's README and in `docs/configuration.md`
5. **Write tests** — at minimum: initialization, get secret, secret not found, unavailable vault
6. **Submit a PR**

See the [Custom Provider Guide](docs/custom-provider.md) for a detailed walkthrough.

---

## Adding a Cache Backend

Same pattern as vault providers:

1. **Implement** `SecretCache`
2. **Register** via SPI in `META-INF/services/edu.m4z.secrets.cache.SecretCache`
3. **Configure** via `secrets.cache.type`
4. **Test** caching, TTL eviction, and encrypted storage

---

## Questions?

Open a [discussion](https://github.com/your-org/secret-manager/discussions) or reach out in the issues. We're happy to help!

Thank you for helping make secret management invisible. 🔐

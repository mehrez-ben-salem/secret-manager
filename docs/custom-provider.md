# Writing a Custom Provider

> Step-by-step guide to adding your own vault provider or cache backend to Secret Manager.

---

## Table of Contents

- [Overview](#overview)
- [Creating a Vault Provider](#creating-a-vault-provider)
  - [Step 1: Create the Module](#step-1-create-the-module)
  - [Step 2: Implement the Interface](#step-2-implement-the-interface)
  - [Step 3: Register via SPI](#step-3-register-via-spi)
  - [Step 4: Configure and Use](#step-4-configure-and-use)
- [Creating a Cache Backend](#creating-a-cache-backend)
- [Testing Your Provider](#testing-your-provider)
- [Publishing](#publishing)

---

## Overview

Secret Manager uses Java's **Service Provider Interface (SPI)** for extensibility. This means you can add a new vault or cache by:

1. Implementing a Java interface
2. Registering it in a `META-INF/services/` file
3. Adding it to the classpath

No changes to the core library are needed. No Spring annotations required.

---

## Creating a Vault Provider

### Step 1: Create the Module

Create a new Maven module in your project:

```xml
<project>
    <artifactId>secret-manager-azure-keyvault</artifactId>
    <dependencies>
        <dependency>
            <groupId>edu.m4z</groupId>
            <artifactId>secret-manager-core</artifactId>
            <version>${secret-manager.version}</version>
        </dependency>
    </dependencies>
</project>
```

### Step 2: Implement the Interface

```java
package com.yourcompany.vault;

import edu.m4z.secrets.provider.SecretVaultProvider;
import edu.m4z.secrets.exception.SecretNotFoundException;
import org.springframework.core.env.ConfigurableEnvironment;

public class AzureKeyVaultProvider implements SecretVaultProvider {

    private String vaultUrl;
    private String tenantId;

    @Override
    public String getProviderName() {
        return "azure-keyvault";
    }

    @Override
    public void initialize(ConfigurableEnvironment env) {
        this.vaultUrl = env.getProperty("secrets.azure-keyvault.vault-url");
        this.tenantId = env.getProperty("secrets.azure-keyvault.tenant-id");
        // Set up HTTP client, authentication, etc.
    }

    @Override
    public String getSecret(String path) throws SecretNotFoundException {
        // Call Azure Key Vault REST API
        // Return the secret value as a String
        // Throw SecretNotFoundException if the secret does not exist
    }

    @Override
    public boolean isAvailable() {
        return vaultUrl != null && !vaultUrl.isBlank();
    }
}
```

### Step 3: Register via SPI

Create the file `src/main/resources/META-INF/services/edu.m4z.secrets.provider.SecretVaultProvider`:

```
com.yourcompany.vault.AzureKeyVaultProvider
```

### Step 4: Configure and Use

Add the JAR to your application's classpath and configure:

```yaml
secrets:
  provider: azure-keyvault
  azure-keyvault:
    vault-url: https://my-vault.vault.azure.net/
    tenant-id: ${AZURE_TENANT_ID}
```

That's it. The `SecretService` will discover and use your provider automatically.

---

## Creating a Cache Backend

The process is identical — implement `SecretCache` instead of `SecretVaultProvider`:

```java
public class RedisSecretCache implements SecretCache {

    @Override
    public String getType() { return "redis"; }

    @Override
    public void initialize(ConfigurableEnvironment env) { /* ... */ }

    @Override
    public Optional<EncryptedSecret> get(String path) { /* ... */ }

    @Override
    public void put(String path, EncryptedSecret secret) { /* ... */ }

    @Override
    public void evict(String path) { /* ... */ }

    @Override
    public void clear() { /* ... */ }
}
```

Register in `META-INF/services/edu.m4z.secrets.cache.SecretCache` and configure with `secrets.cache.type: redis`.

---

## Testing Your Provider

Use the `mock-vault` module as a reference. At minimum, test:

- Initialization with valid and invalid configuration
- Fetching an existing secret
- Handling a missing secret (`SecretNotFoundException`)
- Behavior when the vault is unavailable (`isAvailable()` returns `false`)
- Integration with `SecretService` end-to-end

---

## Publishing

If you'd like your provider included in the official Secret Manager repository:

1. Follow the [Contributing Guide](../CONTRIBUTING.md)
2. Ensure your module has zero external SDK dependencies (if feasible)
3. Include documentation and configuration examples
4. Submit a pull request

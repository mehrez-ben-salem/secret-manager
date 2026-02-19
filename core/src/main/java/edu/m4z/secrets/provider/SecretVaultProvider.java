package edu.m4z.secrets.provider;

import edu.m4z.secrets.exception.SecretNotFoundException;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Service Provider Interface (SPI) for vault implementations.
 * 
 * <p>Implementations provide integration with specific secret vaults
 * such as CyberArk Conjur, HashiCorp Vault, Azure Key Vault, etc.
 * 
 * @author Mehrez Ben Salem
 * @since 1.0.0
 */
public interface SecretVaultProvider {

    void initialize(ConfigurableEnvironment environment);
    /**
     * Get the provider name (e.g., "conjur", "vault", "azure-keyvault").
     * 
     * @return the provider identifier
     */
    String getProviderName();
    
    /**
     * Retrieve a single secret from the vault.
     * 
     * @param path the secret path
     * @return the secret value
     * @throws SecretNotFoundException if secret is not found
     */
    String getSecret(String path) throws SecretNotFoundException;
    
    /**
     * Batch retrieve multiple secrets from the vault.
     * 
     * <p>Default implementation calls {@link #getSecret(String)} for each path.
     * Providers should override this for better performance.
     * 
     * @param paths the set of secret paths
     * @return map of path to secret value
     */
    default Map<String, String> getSecrets(Set<String> paths) {
        Map<String, String> results = new java.util.HashMap<>();
        for (String path : paths) {
            try {
                results.put(path, getSecret(path));
            } catch (SecretNotFoundException e) {
                // Skip missing secrets in batch operations
            }
        }
        return results;
    }
    
    /**
     * Check if the provider is available and healthy.
     * 
     * @return true if provider can be reached, false otherwise
     */
    boolean isAvailable();
    
    /**
     * Get provider-specific metadata for a secret.
     * 
     * <p>May include version information, creation time, etc.
     * 
     * @param path the secret path
     * @return metadata map, or empty map if not supported
     */
    default Map<String, Object> getMetadata(String path) {
        return Collections.emptyMap();
    }
}

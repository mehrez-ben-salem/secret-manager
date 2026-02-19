package edu.m4z.secrets.cache;

import edu.m4z.secrets.encryption.SecretEncryptionService;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Optional;
import java.util.Set;

/**
 * Cache abstraction for storing encrypted secrets.
 * 
 * <p>Implementations should ensure thread-safety and provide efficient
 * storage and retrieval of encrypted secret values.
 * 
 * @author Mehrez Ben Salem
 * @since 1.0.0
 */
public interface SecretCache {

    void initialize(ConfigurableEnvironment environment);
    void initialize(SecretEncryptionService encryptionService);

    String getType();

    long getDefaultTtl();

    /**
     * Store an encrypted secret in the cache.
     * 
     * @param key the secret key/path
     * @param secret the encrypted secret to store
     */
    void put(String key, EncryptedSecret secret);
    
    /**
     * Retrieve an encrypted secret from the cache.
     * 
     * @param key the secret key/path
     * @return the encrypted secret, or empty if not found or expired
     */
    Optional<EncryptedSecret> getEncrypted(String key);
    
    /**
     * Retrieve and decrypt a secret from the cache.
     * 
     * @param key the secret key/path
     * @return the decrypted secret value, or empty if not found or expired
     */
    Optional<String> get(String key);
    
    /**
     * Get all cached secret keys.
     * Used for rotation detection.
     * 
     * @return set of all cached secret keys
     */
    Set<String> getAllKeys();
    
    /**
     * Remove a secret from the cache.
     * 
     * @param key the secret key/path to remove
     */
    void evict(String key);
    
    /**
     * Clear all cached secrets.
     */
    void clear();
    
    /**
     * Get the number of cached secrets.
     * 
     * @return the cache size
     */
    int size();
}

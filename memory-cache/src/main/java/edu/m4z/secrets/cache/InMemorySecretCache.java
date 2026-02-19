package edu.m4z.secrets.cache;

import edu.m4z.secrets.config.SecretsProperties;
import edu.m4z.secrets.cache.SecretCache;
import edu.m4z.secrets.cache.EncryptedSecret;
import edu.m4z.secrets.encryption.SecretEncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.core.env.ConfigurableEnvironment;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of {@link SecretCache}.
 * 
 * <p>Uses a thread-safe ConcurrentHashMap for storage.
 * Secrets are stored encrypted and decrypted on retrieval.
 * 
 * @author Mehrez Ben Salem
 * @since 1.0.0
 */
@Slf4j
public class InMemorySecretCache implements SecretCache {
    
    private final ConcurrentHashMap<String, EncryptedSecret> cache = new ConcurrentHashMap<>();
    private SecretEncryptionService encryptionService;
    private long defaultTtl = -1;

    public InMemorySecretCache() {
    }

    @Override
    public void initialize(SecretEncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }
    @Override
    public void initialize(ConfigurableEnvironment environment){
        SecretsProperties properties = Binder.get(environment)
                .bind("secrets", SecretsProperties.class)
                .orElseGet(SecretsProperties::new);

        this.defaultTtl = properties.getCache().getDefaultTtl();
    }

    @Override
    public String getType() {
        return "memory";
    }

    @Override
    public long getDefaultTtl() {
        return defaultTtl;
    }

    @Override
    public void put(String key, EncryptedSecret secret) {
        log.debug("Caching secret: {}", key);
        cache.put(key, secret);
    }
    
    @Override
    public Optional<EncryptedSecret> getEncrypted(String key) {
        EncryptedSecret encrypted = cache.get(key);
        
        if (encrypted == null) {
            log.debug("Secret {} not found in cache: {}", key, getType());
            return Optional.empty();
        }
        
        if (encrypted.isExpired()) {
            log.debug("Secret expired in cache: {}", key);
            cache.remove(key);
            return Optional.empty();
        }
        
        return Optional.of(encrypted);
    }
    
    @Override
    public Optional<String> get(String key) {
        return getEncrypted(key)
            .map(encrypted -> {
                try {
                    return encryptionService.decrypt(encrypted);
                } catch (Exception e) {
                    log.error("Failed to decrypt secret: {}", key, e);
                    cache.remove(key);
                    return null;
                }
            });
    }
    
    @Override
    public Set<String> getAllKeys() {
        // Remove expired entries first
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        return new HashSet<>(cache.keySet());
    }
    
    @Override
    public void evict(String key) {
        log.debug("Evicting secret from cache: {}", key);
        cache.remove(key);
    }
    
    @Override
    public void clear() {
        log.info("Clearing all cached secrets");
        cache.clear();
    }
    
    @Override
    public int size() {
        // Remove expired entries first
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        return cache.size();
    }
}

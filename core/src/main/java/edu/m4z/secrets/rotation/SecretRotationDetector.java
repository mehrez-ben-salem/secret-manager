package edu.m4z.secrets.rotation;

import edu.m4z.secrets.cache.EncryptedSecret;
import edu.m4z.secrets.cache.SecretCache;
import edu.m4z.secrets.encryption.SecretEncryptionService;
import edu.m4z.secrets.event.SecretRotationEvent;
import edu.m4z.secrets.provider.SecretVaultProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;


import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Scheduled task that detects secret rotation by comparing cached values
 * with current values in the vault.
 * 
 * <p>When rotation is detected, publishes {@link SecretRotationEvent} for
 * application components to react.
 * 
 * @author Mehrez Ben Salem
 * @since 1.0.0
 */
@Slf4j
@Component
public class SecretRotationDetector {
    
    private final SecretCache cache;
    private final SecretVaultProvider vaultProvider;
    private final ApplicationEventPublisher eventPublisher;
    private final SecretEncryptionService encryptionService;


    public SecretRotationDetector(SecretCache cache,
                                  SecretVaultProvider vaultProvider,
                                  ApplicationEventPublisher eventPublisher,
                                  SecretEncryptionService encryptionService) {
        this.cache = cache;
        this.vaultProvider = vaultProvider;
        this.eventPublisher = eventPublisher;
        this.encryptionService = encryptionService;
    }
    
    /**
     * Periodically check for secret rotation.
     * Default interval is 5 minutes (300000ms).
     */
    @Scheduled(fixedDelayString = "${secrets.rotation-detection.check-interval:300000}")
    public void checkRotation() {
        log.info("Starting rotation check...");
        
        Set<String> cachedPaths = cache.getAllKeys();
        
        if (cachedPaths.isEmpty()) {
            log.debug("No secrets in cache to check for rotation");
            return;
        }
        
        log.debug("Checking rotation for {} cached secrets", cachedPaths.size());
        
        try {
            // Batch retrieve current values from vault
            Map<String, String> currentSecrets = vaultProvider.getSecrets(cachedPaths);
            
            if (currentSecrets.isEmpty()) {
                log.warn("No secrets retrieved from vault during rotation check");
                return;
            }
            
            int rotatedCount = 0;
            
            for (Map.Entry<String, String> entry : currentSecrets.entrySet()) {
                String path = entry.getKey();
                String currentValue = entry.getValue();
                
                if (hasSecretRotated(path, currentValue)) {
                    rotatedCount++;
                    handleRotation(path, currentValue);
                }
            }
            
            if (rotatedCount > 0) {
                log.info("Detected {} rotated secret(s)", rotatedCount);
            }
            else {
                log.debug("No secret rotation detected");
            }
            
        } catch (Exception e) {
            log.error("Error during rotation check", e);
        }
    }
    
    /**
     * Check if a secret has been rotated by comparing hashes.
     * 
     * @param path the secret path
     * @param currentValue the current value from vault
     * @return true if rotated, false otherwise
     */
    private boolean hasSecretRotated(String path, String currentValue) {
        Optional<EncryptedSecret> cachedSecret = cache.getEncrypted(path);
        
        if (cachedSecret.isEmpty()) {
            // Secret expired or evicted from cache
            log.info("Secret expired or evicted from cache {}", path);
            return false;
        }
        
        String currentHash = encryptionService.computeHash(currentValue);
        String cachedHash = cachedSecret.get().getHash();
        
        return !currentHash.equals(cachedHash);
    }
    
    /**
     * Handle detected rotation by updating cache and publishing event.
     * 
     * @param path the secret path
     * @param newValue the new secret value
     */
    private void handleRotation(String path, String newValue) {
        log.info("Secret rotation detected for: {}", path);
        
        // Get old hash before updating
        String oldHash = cache.getEncrypted(path)
            .map(EncryptedSecret::getHash)
            .orElse("unknown");
        
        // Encrypt and update cache
        EncryptedSecret encrypted = encryptionService.encrypt(newValue);
        cache.put(path, encrypted);
        
        // Compute new hash
        String newHash = encrypted.getHash();
        
        // Publish event for listeners
        SecretRotationEvent event = new SecretRotationEvent(this, path, newValue);
        
        try {
            eventPublisher.publishEvent(event);
            log.debug("Published rotation event for: {}", path);
        } catch (Exception e) {
            log.error("Error publishing rotation event for: {}", path, e);
        }
    }
}

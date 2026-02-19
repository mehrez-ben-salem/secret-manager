package edu.m4z.secrets.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Represents an encrypted secret stored in the cache.
 * 
 * <p>Contains the encrypted value, initialization vector for decryption,
 * hash for rotation detection, and metadata about caching.
 * 
 * @author Mehrez Ben Salem
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EncryptedSecret {
    
    /**
     * The encrypted secret value.
     */
    private byte[] encryptedValue;
    
    /**
     * Initialization vector used for encryption.
     */
    private byte[] iv;
    
    /**
     * SHA-256 hash of the plaintext value.
     * Used to detect secret rotation without decryption.
     */
    private String hash;
    
    /**
     * Timestamp when the secret was cached.
     */
    private Instant cachedAt;
    
    /**
     * Time-to-live in seconds.
     * -1 means no expiration.
     */
    private long ttl;
    
    /**
     * Check if the secret has expired based on TTL.
     * 
     * @return true if expired, false otherwise
     */
    public boolean isExpired() {
        if (ttl <= 0) {
            return false;
        }
        return Instant.now().isAfter(cachedAt.plusSeconds(ttl));
    }
}

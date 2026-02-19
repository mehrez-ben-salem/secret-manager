package edu.m4z.secrets.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;


/**
 * Configuration properties for Secret Manager.
 * 
 * @author Mehrez Ben Salem
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "secrets")
public class SecretsProperties {
    
    /**
     * Enable/disable secret manager.
     */
    private boolean enabled = true;
    
    /**
     * Vault provider name (e.g., "conjur-vault", "map-vault", "mock-vault").
     */
    private String provider = "conjur-vault";

    /**
     * Encryption configuration.
     */
    private EncryptionConfig encryption = new EncryptionConfig();

    /**
     * Cache configuration.
     */
    private CacheConfig cache = new CacheConfig();

    /**
     * Rotation detection configuration.
     */
    private RotationDetectionConfig rotationDetection = new RotationDetectionConfig();
    
    /**
     * Cache configuration.
     */
    @Data
    public static class CacheConfig {
        /**
         * Cache type: "memory" or "redis".
         */
        private String type = "memory";

        /**
         * Default cache TTL in seconds.
         * -1 means no expiration.
         */
        private long defaultTtl = 3600; // 1 hour
    }

    /**
     * Encryption configuration.
     */
    @Data
    public static class EncryptionConfig {
        /**
         * Base64-encoded master encryption key.
         * If not provided, a random key is generated (NOT FOR PRODUCTION).
         */
        private String key;
        
        /**
         * Encryption algorithm.
         */
        private String algorithm = "AES/GCM/NoPadding";
    }
    
    /**
     * Rotation detection configuration.
     */
    @Data
    public static class RotationDetectionConfig {
        /**
         * Enable/disable rotation detection.
         */
        private boolean enabled = true;

        /**
         * Interval in milliseconds for checking secret rotation.
         * Default: 5 minutes (300000ms).
         */
        private long checkInterval = 300000;

    }
}

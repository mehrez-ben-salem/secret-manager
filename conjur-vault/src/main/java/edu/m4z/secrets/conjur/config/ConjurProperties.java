package edu.m4z.secrets.conjur.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for CyberArk Conjur.
 * Maps to application.yml properties under "secrets.conjur.*"
 * 
 * @author Mehrez Ben Salem
 * @since 1.0.0
 */

@Data
//@Component
//@ConfigurationProperties(prefix = "secrets.conjur-vault")
public class ConjurProperties {
    
    /**
     * Conjur server URL (required)
     */
    private String url;
    
    /**
     * Conjur account name (required)
     * Example: prod, test, dev
     */
    private String account;
    
    /**
     * Authentication login (required)
     * Format: host/application-name
     * Example: host/app-name
     */
    private String authnLogin;
    
    /**
     * API key for authentication (required)
     * Should be loaded from environment variable
     * Example: ${CONJUR_API_KEY}
     */
    private String apiKey;
    
    /**
     * Enable SSL certificate verification (optional)
     * Default: true
     * Set to false only in development environments
     */
    private boolean sslVerify = true;
    
    /**
     * Connection timeout in milliseconds (optional)
     * Default: 5000 (5 seconds)
     */
    private int connectionTimeout = 5000;
    
    /**
     * Read timeout in milliseconds (optional)
     * Default: 10000 (10 seconds)
     */
    private int readTimeout = 10000;

    /**
     * Creates a new ConjurProperties instance.
     */
    public ConjurProperties() {
    }

    /**
     * Validate that all required properties are set.
     * 
     * @throws IllegalStateException if any required property is missing
     */
    public void validate() {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalStateException("Conjur URL is required");
        }
        if (account == null || account.trim().isEmpty()) {
            throw new IllegalStateException("Conjur account is required");
        }
        if (authnLogin == null || authnLogin.trim().isEmpty()) {
            throw new IllegalStateException("Conjur authn-login is required");
        }
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("Conjur api-key is required");
        }
    }
    
    @Override
    public String toString() {
        return "ConjurProperties{" +
                "url='" + url + '\'' +
                ", account='" + account + '\'' +
                ", authnLogin='" + authnLogin + '\'' +
                ", apiKey='***'" +
                ", sslVerify=" + sslVerify +
                ", connectionTimeout=" + connectionTimeout +
                ", readTimeout=" + readTimeout +
                '}';
    }
}

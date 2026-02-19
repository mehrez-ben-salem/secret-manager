package edu.m4z.secrets.demo.service;

import edu.m4z.secrets.annotation.Secret;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

/**
 * Demo service showing @Secret annotation usage.
 * 
 * @author Mehrez Ben Salem
 * @since 1.0.0
 */
@Slf4j
@Service
public class PaymentService {
    
    @Secret(path = "api/external/key")
    private String apiKey;
    
    @Secret(path = "api/external/secret")
    private String apiSecret;
    
    @Secret(path = "app/jwt/secret", cacheTtl = 600)
    private String jwtSecret;
    
    @Secret(
        path = "app/optional/setting",
        defaultValue = "default-value",
        required = false
    )
    private String optionalSetting;
    
    @PostConstruct
    public void init() {
        log.info("PaymentService initialized");
        log.info("API Key: {}...", maskSecret(apiKey));
        log.info("API Secret: {}...", maskSecret(apiSecret));
        log.info("JWT Secret: {}...", maskSecret(jwtSecret));
        log.info("Optional Setting: {}", optionalSetting);
    }
    
    /**
     * Process a payment using the API credentials.
     */
    public void processPayment(String amount) {
        log.info("Processing payment of {} using API key: {}...", 
            amount, maskSecret(apiKey));
        
        // Simulate API call
        // In real scenario, would use apiKey and apiSecret to authenticate
    }
    
    /**
     * Get current API configuration.
     */
    public String getApiConfiguration() {
        return String.format("API Key: %s, API Secret: %s", 
            maskSecret(apiKey), maskSecret(apiSecret));
    }
    
    /**
     * Update API credentials (called after rotation).
     */
    public void updateApiCredentials(String newApiKey, String newApiSecret) {
        this.apiKey = newApiKey;
        this.apiSecret = newApiSecret;
        log.info("API credentials updated - New API Key: {}...", maskSecret(newApiKey));
    }
    
    /**
     * Mask secret for logging.
     */
    private String maskSecret(String secret) {
        if (secret == null || secret.length() < 4) {
            return "***";
        }
        return secret.substring(0, 4) + "***";
    }
}

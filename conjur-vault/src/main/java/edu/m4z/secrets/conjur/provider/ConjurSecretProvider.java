package edu.m4z.secrets.conjur.provider;

import edu.m4z.secrets.exception.SecretNotFoundException;
import edu.m4z.secrets.provider.SecretVaultProvider;

import edu.m4z.secrets.conjur.config.ConjurProperties;
import edu.m4z.secrets.conjur.client.ConjurClient;
import edu.m4z.secrets.conjur.exception.ConjurException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;


import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.*;

/**
 * CyberArk Conjur implementation of SecretVaultProvider.
 * 
 * <p>Pure Java HTTP implementation - NO Conjur SDK dependency.
 * Uses only JDK 11+ HttpClient for all Conjur interactions.
 * 
 * <p>Features:
 * <ul>
 *   <li>Implicit authentication (automatic)</li>
 *   <li>Retrieve secrets only (read-only)</li>
 *   <li>Automatic token refresh on expiration (401)</li>
 *   <li>Batch secret retrieval</li>
 *   <li>Thread-safe operations</li>
 * </ul>
 *
 * @author Mehrez Ben Salem
 * @since 1.0.0
 */
@Slf4j
//@Component
//@ConditionalOnProperty(name = "secrets.provider", havingValue = "conjur")
public class ConjurSecretProvider implements SecretVaultProvider {
    
    private ConjurProperties properties;
    private ConjurClient conjurClient;
    private String account;
    
    /**
     * Create Conjur Secret Provider with auto-configured properties.
     *
     * @param properties Conjur configuration from application.yml
     */
    public ConjurSecretProvider() {
        log.info("Conjur Secret Provider created");
    }

    @Override
    public void initialize(ConfigurableEnvironment environment){
        ConjurProperties properties = Binder.get(environment)
                .bind("secrets.conjur-vault", ConjurProperties.class)
                .orElseGet(ConjurProperties::new);

        initialize(properties);
    }

    /**
     * Initialize the provider (authenticate with Conjur).
     * Called automatically by Spring after construction.
     *
     * @throws ConjurException if initialization/authentication fails
     */
    public void initialize(ConjurProperties properties) throws ConjurException {
        log.info("Initializing Conjur Secret Provider...");
        this.properties = properties;
        this.account = properties.getAccount();
        this.conjurClient = new ConjurClient(properties);

        try {
            properties.validate();
            conjurClient.authenticate();
            
            log.info("Conjur Secret Provider initialized successfully");
            
        } catch (Exception e) {
            log.error("Failed to initialize Conjur Secret Provider", e);
            throw e;
        }
    }
    
    /**
     * Get the provider name.
     *
     * @return "conjur"
     */
    @Override
    public String getProviderName() {
        return "conjur-vault";
    }
    
    /**
     * Retrieve a secret from Conjur.
     * 
     * <p>Automatically authenticates if needed and retries on token expiration.
     * Secret path is automatically formatted to Conjur variable ID format.
     *
     * @param path Secret path (will be formatted as account:variable:path)
     * @return Secret value
     * @throws SecretNotFoundException if secret not found or retrieval fails
     */
    @Override
    public String getSecret(String path) throws SecretNotFoundException {
        try {
            // Format path to Conjur variable ID
            String variableId = formatVariableId(path);
            
            log.debug("Retrieving secret from Conjur: {}", variableId);
            
            // Retrieve using pure Java HTTP client
            String value = conjurClient.retrieveSecret(variableId);
            
            if (value == null || value.isEmpty()) {
                throw new SecretNotFoundException(path);
            }
            
            return value;
            
        } catch (ConjurException e) {
            log.error("Failed to retrieve secret from Conjur: {}", path, e);
            throw new SecretNotFoundException(path, e);
        }
    }
    
    /**
     * Batch retrieve multiple secrets.
     * More efficient than calling getSecret() multiple times.
     *
     * @param paths Set of secret paths
     * @return Map of path to secret value (excludes failed retrievals)
     */
    @Override
    public Map<String, String> getSecrets(Set<String> paths) {
        if (paths == null || paths.isEmpty()) {
            return Collections.emptyMap();
        }
        
        log.debug("Batch retrieving {} secrets from Conjur", paths.size());
        
        Map<String, String> results = new HashMap<>();
        
        // Format all paths to variable IDs
        Set<String> variableIds = new HashSet<>();
        Map<String, String> idToPathMap = new HashMap<>();
        
        for (String path : paths) {
            String variableId = formatVariableId(path);
            variableIds.add(variableId);
            idToPathMap.put(variableId, path);
        }
        
        // Retrieve using ConjurClient (handles individual retrieval)
        Map<String, String> batchResults = conjurClient.retrieveSecrets(variableIds);
        
        // Map back to original paths
        for (Map.Entry<String, String> entry : batchResults.entrySet()) {
            String variableId = entry.getKey();
            String originalPath = idToPathMap.get(variableId);
            if (originalPath != null) {
                results.put(originalPath, entry.getValue());
            }
        }
        
        log.debug("Successfully retrieved {}/{} secrets", results.size(), paths.size());
        
        return results;
    }
    
    /**
     * Check if Conjur is available and accessible.
     *
     * @return true if Conjur is available, false otherwise
     */
    @Override
    public boolean isAvailable() {
        try {
            // Try to authenticate to verify connectivity
            return conjurClient.isAvailable();
        } catch (Exception e) {
            log.error("Conjur health check failed", e);
            return false;
        }
    }
    
    /**
     * Get metadata about the provider and a specific secret.
     *
     * @param path Secret path
     * @return Metadata map containing provider info
     */
    @Override
    public Map<String, Object> getMetadata(String path) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("provider", "conjur");
        metadata.put("account", account);
        metadata.put("baseUrl", properties.getUrl());
        metadata.put("variableId", formatVariableId(path));
        metadata.put("authenticated", conjurClient.isAuthenticated());
        metadata.put("sslVerify", properties.isSslVerify());
        return metadata;
    }
    
    /**
     * Get the Conjur properties.
     *
     * @return Conjur configuration properties
     */
    public ConjurProperties getProperties() {
        return properties;
    }
    
    /**
     * Get the underlying Conjur client.
     *
     * @return ConjurClient instance
     */
    public ConjurClient getConjurClient() {
        return conjurClient;
    }
    
    /**
     * Destroy the provider (cleanup resources).
     * Called automatically by Spring on shutdown.
     */
    @PreDestroy
    public void destroy() {
        log.info("Destroying Conjur Secret Provider");
        conjurClient.clearToken();
    }
    
    /**
     * Format a secret path to Conjur variable ID.
     * Format: account:variable:path
     * 
     * @param path the secret path
     * @return Conjur variable ID
     */
    private String formatVariableId(String path) {
        // Remove leading/trailing slashes
        path = path.replaceAll("^/+|/+$", "");
        
        // Conjur format: account:variable:path
        return String.format("%s:variable:%s", account, path);
    }
}

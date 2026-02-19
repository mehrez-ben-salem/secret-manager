package edu.m4z.secrets.processor;

import edu.m4z.secrets.cache.EncryptedSecret;
import edu.m4z.secrets.cache.SecretCache;
import edu.m4z.secrets.config.SecretsProperties;
import edu.m4z.secrets.encryption.SecretEncryptionService;
import edu.m4z.secrets.exception.SecretNotFoundException;
import edu.m4z.secrets.provider.SecretVaultProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.PropertySource;

/**
 * Custom PropertySource that resolves ${secret://path} placeholders.
 * 
 * <p>Allows secrets to be referenced in application.yml using the syntax:
 * <pre>
 * spring:
 *   datasource:
 *     password: ${secret://database/prod/password}
 *     username: ${secret://database/prod/username:default-user}
 * </pre>
 * 
 * @author Mehrez Ben Salem
 * @since 1.0.0
 */
@Slf4j
public class SecretPropertySource extends PropertySource<SecretVaultProvider> {
    
    private static final String PREFIX = "secret://";
    
    private final SecretCache cache;
    private final SecretEncryptionService encryptionService;
    private final SecretsProperties properties;

    public SecretPropertySource(String name, SecretVaultProvider provider,
                                SecretCache cache, SecretEncryptionService encryptionService,
                                SecretsProperties properties) {
        super(name, provider);
        this.cache = cache;
        this.encryptionService = encryptionService;
        this.properties = properties;
    }

    
    @Override
    public Object getProperty(String name) {
        // Only handle secret:// properties
        if (!name.startsWith(PREFIX)) {
            return null;
        }
        
        // Parse: secret://path/to/secret:defaultValue
        String[] parts = name.substring(PREFIX.length()).split(":", 2);
        String path = parts[0];
        String defaultValue = parts.length > 1 ? parts[1] : null;
        
        log.debug("Resolving secret property: {}", path);
        
        // Try cache first
        return cache.get(path).orElseGet(() -> {
            try {
                // Fetch from vault
                String value = source.getSecret(path);
                
                log.debug("Secret property retrieved from vault: {}", path);
                
                // Cache it
                EncryptedSecret encrypted = encryptionService.encrypt(value);
                encrypted.setTtl(properties.getCache().getDefaultTtl());
                cache.put(path, encrypted);
                
                return value;
                
            } catch (SecretNotFoundException e) {
                if (defaultValue != null) {
                    log.warn("Secret property not found '{}', using default value", path);
                    return defaultValue;
                }
                
                log.error("Secret property not found and no default value: {}", path);
                throw new IllegalStateException("Secret not found: " + path, e);
            }
        });
    }
}

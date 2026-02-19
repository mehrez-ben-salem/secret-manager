package edu.m4z.secrets.provider;

import edu.m4z.secrets.exception.SecretNotFoundException;
import edu.m4z.secrets.provider.SecretVaultProvider;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock vault provider for demo and testing purposes.
 * 
 * <p>Simulates a secret vault with in-memory storage.
 * Allows simulation of secret rotation for demonstration.
 * 
 * @author Mehrez Ben Salem
 * @since 1.0.0
 */
@Slf4j
public class MapVaultProvider implements SecretVaultProvider {
    
    protected final ConcurrentHashMap<String, String> secrets = new ConcurrentHashMap<>();

    public MapVaultProvider() {
        super();
    }

    @Override
    public String getProviderName() {
        return "map-vault";
    }
    
    @Override
    public String getSecret(String path) throws SecretNotFoundException {
        String value = secrets.get(path);
        
        if (value == null) {
            log.debug("Vault: {} hoesn't Secret {}", getProviderName(), path);
            throw new SecretNotFoundException(path);
        }
        
        log.debug("Retrieved secret from {} vault: {}", getProviderName(), path);
        return value;
    }
    
    @Override
    public Map<String, String> getSecrets(Set<String> paths) {
        Map<String, String> results = new HashMap<>();
        
        for (String path : paths) {
            String value = secrets.get(path);
            if (value != null) {
                results.put(path, value);
            }
        }
        
        log.debug("Batch retrieved {} secrets from {} vault", results.size(), getProviderName());
        return results;
    }
    
    @Override
    public boolean isAvailable() {
        return true;
    }
    
    @Override
    public Map<String, Object> getMetadata(String path) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("provider", getProviderName());
        metadata.put("exists", secrets.containsKey(path));
        return metadata;
    }

    @Override
    public void initialize(ConfigurableEnvironment environment) {
        // Bind the map-vault entries directly from the environment
        Binder binder = Binder.get(environment);
        binder.bind("secrets.map-vault.entry-set", Bindable.listOf(Map.class))
                .ifBound(entries -> {
                    for (Map<String, String> entry : entries) {
                        String key = entry.get("key");
                        String value = entry.get("value");
                        if (key != null && value != null) {
                            secrets.put(key, value);
                        }
                    }
                });

        log.info("{} vault initialized with {} secrets", getProviderName(), secrets.size());
    }

}

package edu.m4z.secrets.provider;

import edu.m4z.secrets.provider.MapVaultProvider;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Map;
import java.util.Set;

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
public class MockVaultProvider extends MapVaultProvider {

    public MockVaultProvider() {
        super();
    }

    @Override
    public void initialize(ConfigurableEnvironment environment) {
        // Bind the map-vault entries directly from the environment
        Binder binder = Binder.get(environment);
        binder.bind("secrets.mock-vault.entry-set", Bindable.listOf(Map.class))
                .ifBound(entries -> {
                    for (Map<String, String> entry : entries) {
                        String key = entry.get("key");
                        String value = entry.get("value");
                        if (key != null && value != null) {
                            secrets.put(key, value);
                            log.info("{} have a new secret", getProviderName(), secrets.size());
                        }
                    }
                });

        log.info("{} initialized with {} secrets", getProviderName(), secrets.size());
    }

    @Override
    public String getProviderName() {
        return "mock-vault";
    }

    /**
     * Update a secret (for simulating rotation).
     * 
     * @param path the secret path
     * @param newValue the new value
     */
    public void updateSecret(String path, String newValue) {
        secrets.put(path, newValue);
        log.info("Secret updated in mock vault: {}", path);
    }
    
    /**
     * Get all secret paths.
     * 
     * @return set of all paths
     */
    public Set<String> getAllPaths() {
        return secrets.keySet();
    }
}

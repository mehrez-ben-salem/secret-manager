package edu.m4z.secrets.autoconfigure;

import edu.m4z.secrets.cache.SecretCache;
import edu.m4z.secrets.config.SecretsProperties;
import edu.m4z.secrets.encryption.SecretEncryptionService;
import edu.m4z.secrets.provider.SecretVaultProvider;
import edu.m4z.secrets.processor.SecretPropertySource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.ServiceLoader;

/**
 * EnvironmentPostProcessor that registers SecretPropertySource EARLY.
 *
 * <p>This runs BEFORE property placeholder resolution, allowing ${secret://...}
 * placeholders to be resolved when Spring processes application.yml.
 *
 * <p>Uses Java ServiceLoader to discover SecretVaultProvider implementations,
 * making it completely agnostic to the actual provider implementation.
 *
 * <p>Provider implementations must:
 * 1. Implement {@link SecretVaultProvider}
 * 2. Have a no-arg constructor
 * 3. Be registered in META-INF/services/edu.m4z.secrets.provider.SecretVaultProvider
 *
 * @author Mehrez Ben Salem
 * @since 1.0.0
 */
@Slf4j
public class SecretEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
                                       SpringApplication application) {

        // Bind configuration properties early (before @ConfigurationProperties processing)
        SecretsProperties properties = Binder.get(environment)
                .bind("secrets", SecretsProperties.class)
                .orElseGet(SecretsProperties::new);

        // Check if enabled
        if (!properties.isEnabled()) {
            log.debug("Secret Manager is disabled");
            return;
        }

        log.info("Secret Manager Environment Post-Processor starting - Provider: {}", properties.getProvider());

        // Create encryption service and cache
        SecretEncryptionService encryptionService = new SecretEncryptionService();
        encryptionService.initialize(environment);

        // Discover cache by type from application.yml
        SecretCache cache = discoverCache(properties.getCache().getType());
        if (cache == null) {
            log.warn("No SecretCache found for type '{}'. %nMake sure the provider is registered in META-INF/services/edu.m4z.secrets.cache.SecretCache", properties.getCache().getType());
            return;
        }
        cache.initialize(environment);
        cache.initialize(encryptionService);

        // Discover and load SecretVaultProvider using Java ServiceLoader
        SecretVaultProvider provider = discoverProvider(properties.getProvider());

        if (provider == null) {
            log.warn("No SecretVaultProvider found for provider '{}'. %nMake sure the provider is registered in META-INF/services/edu.m4z.secrets.provider.SecretVaultProvider", properties.getProvider());
            return;
        }

        log.info("Discovered SecretVaultProvider: {} ({})", provider.getProviderName(), provider.getClass().getName());

        // Initialize with environment so provider can read its config
        provider.initialize(environment);

        // Check if provider is available
        if (!provider.isAvailable()) {
            log.warn("SecretVaultProvider '{}' is not available. Skipping PropertySource registration.", provider.getProviderName());
            return;
        }

        // Create and register PropertySource
        SecretPropertySource propertySource = new SecretPropertySource(
                "SecretPropertySource",
                provider,
                cache,
                encryptionService,
                properties
        );

        // Add as FIRST (highest priority after command line args)
        environment.getPropertySources().addFirst(propertySource);

        // Register all instances as beans for the Spring context
        final SecretCache finalCache = cache;
        application.addInitializers(context -> {
            context.getBeanFactory().registerSingleton("secretVaultProvider", provider);
            context.getBeanFactory().registerSingleton("secretsProperties", properties);
            context.getBeanFactory().registerSingleton("secretEncryptionService", encryptionService);
            context.getBeanFactory().registerSingleton("secretCache", finalCache);
        });


        log.info("SecretPropertySource registered successfully with provider: {}", provider.getProviderName());
    }

    /**
     * Discover SecretVaultProvider using Java ServiceLoader mechanism.
     *
     * <p>This allows providers to be pluggable - any implementation that:
     * 1. Implements SecretVaultProvider
     * 2. Has a no-arg constructor
     * 3. Is registered in META-INF/services/edu.m4z.secrets.provider.SecretVaultProvider
     *
     * Will be automatically discovered.
     *
     * @param providerName the configured provider name (e.g., "mock", "conjur", "vault")
     * @return the matching provider, or null if not found
     */
    private SecretVaultProvider discoverProvider(String providerName) {
        log.debug("Discovering SecretVaultProvider implementations using ServiceLoader");

        // Use ServiceLoader to discover all provider implementations
        ServiceLoader<SecretVaultProvider> serviceLoader = ServiceLoader.load(
                SecretVaultProvider.class,
                Thread.currentThread().getContextClassLoader()
        );

        // Find the provider matching the configured name
        for (SecretVaultProvider provider : serviceLoader) {
            log.debug("Found provider: {} ({})",
                    provider.getProviderName(),
                    provider.getClass().getName());

            if (providerName.equals(provider.getProviderName())) {
                log.info("Matched provider '{}' to implementation: {}",
                        providerName,
                        provider.getClass().getName());
                return provider;
            }
        }

        log.warn("No provider implementation found matching name: '{}'", providerName);
        log.warn("Available providers discovered: " +
                getAvailableProviderNames(serviceLoader));

        return null;
    }


    /**
     * Get list of available provider names for debugging.
     */
    private String getAvailableProviderNames(ServiceLoader<SecretVaultProvider> serviceLoader) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (SecretVaultProvider provider : serviceLoader) {
            if (!first) sb.append(", ");
            sb.append(provider.getProviderName());
            first = false;
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Discover SecretCache using Java ServiceLoader mechanism.
     *
     * <p>This allows providers to be pluggable - any implementation that:
     * 1. Implements SecretVaultProvider
     * 2. Has a no-arg constructor
     * 3. Is registered in META-INF/services/edu.m4z.secrets.cache.SecretCache
     *
     * Will be automatically discovered.
     *
     * @param cacheType the configured cache type (e.g., "memory", "redis")
     * @return the matching cache, or null if not found
     */
    private SecretCache discoverCache(String cacheType) {
        log.debug("Discovering SecretCache implementations using ServiceLoader");

        // Use ServiceLoader to discover all cache implementations
        ServiceLoader<SecretCache> serviceLoader = ServiceLoader.load(
                SecretCache.class,
                Thread.currentThread().getContextClassLoader()
        );

        // Find the cache matching the configured type
        for (SecretCache cache : serviceLoader) {
            log.debug("Found cache: {} ({})", cache.getType(), cache.getClass().getName());

            if (cacheType.equals(cache.getType())) {
                log.info("Matched cache '{}' to implementation: {}", cacheType, cache.getClass().getName());
                return cache;
            }
        }

        log.warn("No cache implementation found matching name: '{}'", cacheType);
        log.warn("Available caches discovered: {}", getAvailableCacheTypes(serviceLoader));

        return null;
    }


    /**
     * Get list of available cache types for debugging.
     */
    private String getAvailableCacheTypes(ServiceLoader<SecretCache> serviceLoader) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (SecretCache cache : serviceLoader) {
            if (!first) sb.append(", ");
            sb.append(cache.getType());
            first = false;
        }
        sb.append("]");
        return sb.toString();
    }
}

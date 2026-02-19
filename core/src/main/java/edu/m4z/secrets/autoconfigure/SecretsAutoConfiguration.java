package edu.m4z.secrets.autoconfigure;

import edu.m4z.secrets.cache.SecretCache;
import edu.m4z.secrets.config.SecretsProperties;
import edu.m4z.secrets.encryption.SecretEncryptionService;
import edu.m4z.secrets.processor.SecretAnnotationProcessor;
import edu.m4z.secrets.provider.SecretVaultProvider;
import edu.m4z.secrets.rotation.SecretRotationDetector;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Auto-configuration for Secret Manager.
 * 
 * <p>Automatically configures all necessary beans when the library
 * is present on the classpath.
 * 
 * @author Mehrez Ben Salem
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(SecretsProperties.class)
public class SecretsAutoConfiguration {

    /**
     * Create annotation processor for @Secret annotations.
     */
    @Bean
    @ConditionalOnMissingBean
    public SecretAnnotationProcessor secretAnnotationProcessor(
            SecretCache cache,
            SecretVaultProvider provider,
            SecretEncryptionService encryptionService,
            SecretsProperties properties) {
        log.info("Creating SecretAnnotationProcessor bean!");
        return new SecretAnnotationProcessor(cache, provider, encryptionService, properties);
    }
    
    /**
     * Create rotation detector.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "secrets.rotation-detection.enabled", havingValue = "true", matchIfMissing = false)
    public SecretRotationDetector secretRotationDetector(
            SecretCache cache,
            SecretVaultProvider provider,
            ApplicationEventPublisher eventPublisher,
            SecretEncryptionService encryptionService) {

        log.info("Creating SecretRotationDetector bean");
        return new SecretRotationDetector(cache, provider, eventPublisher, encryptionService);
    }

}

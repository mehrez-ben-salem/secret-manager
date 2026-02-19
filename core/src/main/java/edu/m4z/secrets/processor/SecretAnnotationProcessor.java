package edu.m4z.secrets.processor;

import edu.m4z.secrets.annotation.Secret;
import edu.m4z.secrets.cache.EncryptedSecret;
import edu.m4z.secrets.cache.SecretCache;
import edu.m4z.secrets.config.SecretsProperties;
import edu.m4z.secrets.encryption.SecretEncryptionService;
import edu.m4z.secrets.exception.SecretInitializationException;
import edu.m4z.secrets.exception.SecretNotFoundException;
import edu.m4z.secrets.provider.SecretVaultProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;

/**
 * Processes @Secret annotations on bean fields during Spring initialization.
 * 
 * <p>Automatically injects secret values from the vault into annotated fields.
 * 
 * @author Mehrez Ben Salem
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecretAnnotationProcessor implements BeanPostProcessor {
    
    private final SecretCache cache;
    private final SecretVaultProvider vaultProvider;
    private final SecretEncryptionService encryptionService;
    private final SecretsProperties properties;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        
        Class<?> clazz = bean.getClass();
        
        // Process all fields with @Secret annotation
        ReflectionUtils.doWithFields(clazz, field -> {
            Secret secretAnnotation = field.getAnnotation(Secret.class);
            if (secretAnnotation != null) {
                injectSecret(bean, field, secretAnnotation);
            }
        }, field -> field.isAnnotationPresent(Secret.class));
        
        return bean;
    }
    
    /**
     * Inject secret value into the field.
     * 
     * @param bean the target bean
     * @param field the field to inject into
     * @param annotation the @Secret annotation
     */
    private void injectSecret(Object bean, Field field, Secret annotation) {
        String path = annotation.path();
        
        log.debug("Processing @Secret annotation for field '{}' in bean '{}', path: {}", 
            field.getName(), bean.getClass().getSimpleName(), path);
        
        // Try cache first
        String secretValue = cache.get(path).orElseGet(() -> {
            try {
                log.info("Vault provider : {}", vaultProvider.getProviderName());
                // Fetch from vault
                String value = vaultProvider.getSecret(path);
                
                log.debug("Secret retrieved from vault: {}", path);
                
                // Store in cache
                EncryptedSecret encrypted = encryptionService.encrypt(value);
                
                // Apply custom TTL if specified
                long ttl = annotation.cacheTtl();
                if (ttl > 0) {
                    encrypted.setTtl(ttl);
                } else if (ttl == -1) {
                    encrypted.setTtl(properties.getCache().getDefaultTtl());
                }
                
                cache.put(path, encrypted);
                
                return value;
                
            } catch (SecretNotFoundException e) {
                // Handle missing secret
                if (annotation.required() && annotation.defaultValue().isEmpty()) {
                    throw new SecretInitializationException(
                        "Required secret not found: " + path + " for field " + 
                        field.getName() + " in " + bean.getClass().getName(), e);
                }
                
                String defaultValue = annotation.defaultValue();
                if (!defaultValue.isEmpty()) {
                    log.warn("Secret not found '{}', using default value", path);
                    return defaultValue;
                }
                
                log.warn("Secret not found '{}', field will be null", path);
                return null;
            }
        });

        // Inject value using reflection
        try {
            ReflectionUtils.makeAccessible(field);
            ReflectionUtils.setField(field, bean, secretValue);
            log.debug("Injected secret into field '{}' in bean '{}'", 
                field.getName(), bean.getClass().getSimpleName());
        } catch (Exception e) {
            throw new SecretInitializationException(
                "Failed to inject secret into field " + field.getName() + 
                " in " + bean.getClass().getName(), e);
        }
    }
}

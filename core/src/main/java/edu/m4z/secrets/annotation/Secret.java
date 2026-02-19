package edu.m4z.secrets.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark fields that should be injected with secrets from a vault.
 * 
 * <p>Usage example:
 * <pre>
 * {@code
 * @Component
 * public class MyService {
 *     
 *     @Secret(path = "database/prod/password")
 *     private String dbPassword;
 *     
 *     @Secret(path = "api/key", defaultValue = "dev-key", cacheTtl = 600)
 *     private String apiKey;
 * }
 * }
 * </pre>
 * 
 * @author Mehrez Ben Salem
 * @since 1.0.0
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Secret {
    
    /**
     * Path to the secret in the vault.
     * 
     * @return the secret path
     */
    String path();
    
    /**
     * Default value to use if the secret is not found.
     * Only used when {@link #required()} is false.
     * 
     * @return the default value
     */
    String defaultValue() default "";
    
    /**
     * Whether the secret is required.
     * If true and secret is not found (and no default value), throws exception.
     * 
     * @return true if required, false otherwise
     */
    boolean required() default true;
    
    /**
     * Cache TTL in seconds for this specific secret.
     * Overrides the global cache TTL configuration.
     * Use -1 to use global configuration.
     * 
     * @return cache TTL in seconds, or -1 for global config
     */
    long cacheTtl() default -1;
}

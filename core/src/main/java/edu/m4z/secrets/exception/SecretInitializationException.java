package edu.m4z.secrets.exception;

/**
 * Exception thrown when secret initialization fails during application startup.
 * 
 * @author Mehrez Ben Salem
 * @since 1.0.0
 */
public class SecretInitializationException extends SecretManagerException {
    
    public SecretInitializationException(String message) {
        super(message);
    }
    
    public SecretInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}

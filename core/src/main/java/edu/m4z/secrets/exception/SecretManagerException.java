package edu.m4z.secrets.exception;

/**
 * Base exception for secret manager errors.
 * 
 * @author Mehrez Ben Salem
 * @since 1.0.0
 */
public class SecretManagerException extends RuntimeException {
    
    public SecretManagerException(String message) {
        super(message);
    }
    
    public SecretManagerException(String message, Throwable cause) {
        super(message, cause);
    }
}

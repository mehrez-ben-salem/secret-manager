package edu.m4z.secrets.exception;

/**
 * Exception thrown when secret encryption/decryption fails.
 * 
 * @author Mehrez Ben Salem
 * @since 1.0.0
 */
public class SecretEncryptionException extends SecretManagerException {
    
    public SecretEncryptionException(String message) {
        super(message);
    }
    
    public SecretEncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}

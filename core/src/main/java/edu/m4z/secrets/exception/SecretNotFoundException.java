package edu.m4z.secrets.exception;

/**
 * Exception thrown when a required secret is not found in the vault.
 * 
 * @author Mehrez Ben Salem
 * @since 1.0.0
 */
public class SecretNotFoundException extends SecretManagerException {
    
    private final String secretPath;
    
    public SecretNotFoundException(String secretPath) {
        super("Secret not found: " + secretPath);
        this.secretPath = secretPath;
    }
    
    public SecretNotFoundException(String secretPath, Throwable cause) {
        super("Secret not found: " + secretPath, cause);
        this.secretPath = secretPath;
    }
    
    public String getSecretPath() {
        return secretPath;
    }
}

package edu.m4z.secrets.conjur.exception;

import edu.m4z.secrets.exception.SecretManagerException;
/**
 * Exception thrown when Conjur operations fail.
 *
 * @author Mehrez Ben Salem
 * @since 1.0.0
 */

/**
 * Custom exception for Conjur client errors.
 */
public class ConjurException extends SecretManagerException {
    /**
     * Creates a ConjurException with a message.
     * @param message error message
     */
    public ConjurException(String message) {
        super(message);
    }

    /**
     * Creates a ConjurException with a message and root cause.
     * @param message error message
     * @param cause root cause
     */
    public ConjurException(String message, Throwable cause) {
        super(message, cause);
    }
}

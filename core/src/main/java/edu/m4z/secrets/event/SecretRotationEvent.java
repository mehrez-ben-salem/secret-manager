package edu.m4z.secrets.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a secret rotation is detected.
 * 
 * <p>Applications can listen to this event to react to secret changes,
 * such as reconnecting database connections or updating API clients.
 * 
 * @author Mehrez Ben Salem
 * @since 1.0.0
 */
@Getter
public class SecretRotationEvent extends ApplicationEvent {
    
    /**
     * The path of the rotated secret.
     */
    private final String secretPath;
    
    /**
     * The new secret value.
     */
    private final String newValue;

    
    /**
     * Create a new secret rotation event.
     * 
     * @param source the source of the event
     * @param secretPath the path of the rotated secret
     * @param newValue the new secret value
     * @param oldHash hash of the old value
     * @param newHash hash of the new value
     */
    public SecretRotationEvent(Object source, String secretPath, String newValue) {
        super(source);
        this.secretPath = secretPath;
        this.newValue = newValue;
    }
}

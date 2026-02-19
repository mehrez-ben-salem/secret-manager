package edu.m4z.secrets.demo.listener;

import com.zaxxer.hikari.HikariDataSource;
import edu.m4z.secrets.demo.service.PaymentService;
import edu.m4z.secrets.event.SecretRotationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Listener for secret rotation events.
 * 
 * <p>Demonstrates how to react to secret rotation in the application.
 * 
 * @author Mehrez Ben Salem
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecretRotationListener {
    
    private final DataSource dataSource;
    private final PaymentService paymentService;
    
    /**
     * Handle secret rotation events.
     */
    @EventListener
    public void handleSecretRotation(SecretRotationEvent event) {
        log.info("=".repeat(80));
        log.info("SECRET ROTATION DETECTED!");
        log.info("Path: {}", event.getSecretPath());
        log.info("=".repeat(80));
        
        // Handle specific secret rotations
        if (event.getSecretPath().equals("database/prod/password")) {
            handleDatabasePasswordRotation(event);
        } else if (event.getSecretPath().equals("api/external/key")) {
            handleApiKeyRotation(event);
        } else if (event.getSecretPath().equals("api/external/secret")) {
            handleApiSecretRotation(event);
        } else {
            log.info("No specific handler for secret: {}", event.getSecretPath());
        }
    }
    
    /**
     * Handle database password rotation.
     */
    private void handleDatabasePasswordRotation(SecretRotationEvent event) {
        log.info("Handling database password rotation...");
        
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikari = (HikariDataSource) dataSource;
            
            // Update password
            hikari.setPassword(event.getNewValue());
            
            // Soft evict connections to force reconnection with new password
            hikari.getHikariPoolMXBean().softEvictConnections();
            
            log.info("Database password updated and connections evicted");
        } else {
            log.warn("DataSource is not HikariDataSource, cannot update password dynamically");
        }
    }
    
    /**
     * Handle API key rotation.
     */
    private void handleApiKeyRotation(SecretRotationEvent event) {
        log.info("Handling API key rotation...");
        
        // Update PaymentService with new API key
        // Note: In real scenario, you might need to reinitialize HTTP clients
        
        log.info("API key rotation handled");
    }
    
    /**
     * Handle API secret rotation.
     */
    private void handleApiSecretRotation(SecretRotationEvent event) {
        log.info("Handling API secret rotation...");
        
        log.info("API secret rotation handled");
    }
}

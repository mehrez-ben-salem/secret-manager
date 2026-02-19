package edu.m4z.secrets.demo.service;

import edu.m4z.secrets.cache.SecretCache;
import edu.m4z.secrets.provider.MockVaultProvider;
import edu.m4z.secrets.provider.SecretVaultProvider;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for demo operations.
 * 
 * @author Mehrez Ben Salem
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
public class DemoController {

    private final SecretCache secretCache;

    private final SecretVaultProvider vaultProvider;

    private final PaymentService paymentService;

    /**
     * Get cache statistics.
     */
    @GetMapping("/cache/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("size", secretCache.size());
        stats.put("keys", secretCache.getAllKeys());
        stats.put("provider", vaultProvider.getProviderName());

        return ResponseEntity.ok(stats);
    }

    /**
     * Test payment processing.
     */
    @PostMapping("/payment")
    public ResponseEntity<String> processPayment(@RequestParam String amount) {
        paymentService.processPayment(amount);
        return ResponseEntity.ok("Payment processed: " + amount);
    }
    
    /**
     * Get API configuration.
     */
    @GetMapping("/config")
    public ResponseEntity<String> getConfiguration() {
        return ResponseEntity.ok(paymentService.getApiConfiguration());
    }
    
    /**
     * Simulate secret rotation (only for MockVaultProvider).
     */
    @PostMapping("/rotate-secret")
    @ConditionalOnBean(MockVaultProvider.class)
    public ResponseEntity<String> rotateSecret(@RequestBody RotateSecretRequest request) {
        if (!(vaultProvider instanceof MockVaultProvider)) {
            return ResponseEntity.badRequest()
                .body("Secret rotation simulation only available with MockVaultProvider");
        }
        
        MockVaultProvider mockProvider = (MockVaultProvider) vaultProvider;
        
        log.info("Simulating secret rotation: {}", request.getPath());
        
        mockProvider.updateSecret(request.getPath(), request.getNewValue());
        
        return ResponseEntity.ok("Secret rotation simulated. " +
            "Rotation will be detected in next rotation check cycle (default: 5 minutes). " +
            "Or you can trigger immediately by restarting the app.");
    }

    /**
     * Clear cache.
     */
    @DeleteMapping("/clear-cache")
    public ResponseEntity<String> clearCache() {
        int size = secretCache.size();
        secretCache.clear();
        return ResponseEntity.ok("Cache cleared. Removed " + size + " secrets.");
    }

    /**
     * Health check.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("vaultProvider", vaultProvider.getProviderName());
        health.put("vaultAvailable", vaultProvider.isAvailable());
        health.put("cacheSize", secretCache.size());

        return ResponseEntity.ok(health);
    }
    
    @Data
    public static class RotateSecretRequest {
        private String path;
        private String newValue;
    }
}

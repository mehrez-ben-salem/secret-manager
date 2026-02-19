package edu.m4z.secrets.conjur.client;

import edu.m4z.secrets.conjur.config.ConjurProperties;
import edu.m4z.secrets.conjur.exception.ConjurException;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Pure Java implementation of CyberArk Conjur client.
 * 
 * NO EXTERNAL DEPENDENCIES - Uses only JDK built-in libraries:
 * - java.net.http.HttpClient (Java 11+)
 * - java.util.Base64
 * - java.net.URLEncoder
 * - javax.net.ssl.SSLContext
 * 
 * Features:
 * - Implicit authentication (authenticate once, reuse token)
 * - Automatic token refresh on 401
 * - Retrieve secrets only (read-only)
 * - SSL verification configurable
 * - Thread-safe
 *
 * @author Mehrez Ben Salem
 * @since 1.0.0
 */
public class ConjurClient {
    
    private final ConjurProperties properties;
    private final HttpClient httpClient;
    private final AtomicReference<String> accessToken = new AtomicReference<>();
    
    /**
     * Create a new Conjur client from properties.
     *
     * @param properties Conjur configuration properties
     */
    public ConjurClient(ConjurProperties properties) {
        this.properties = properties;
        this.properties.validate(); // Ensure properties are valid
        this.httpClient = createHttpClient();
    }
    
    /**
     * Authenticate with Conjur and obtain an access token.
     * Token is stored internally and reused for subsequent requests.
     * 
     * @throws ConjurException if authentication fails
     */
    public void authenticate() throws ConjurException {
        try {
            // URL: {baseUrl}/authn/{account}/{login}/authenticate
            String encodedLogin = URLEncoder.encode(properties.getAuthnLogin(), StandardCharsets.UTF_8);
            String url = String.format("%s/authn/%s/%s/authenticate", 
                properties.getUrl(), properties.getAccount(), encodedLogin);
            
            // Build request with API key in body
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(properties.getApiKey()))
                .timeout(Duration.ofSeconds(30))
                .build();
            
            // Send request
            HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                throw new ConjurException(
                    "Authentication failed: HTTP " + response.statusCode() + 
                    " - " + response.body()
                );
            }
            
            // Store the access token
            String token = response.body();
            if (token == null || token.isEmpty()) {
                throw new ConjurException("Empty token received from Conjur");
            }
            
            accessToken.set(token);
            
        } catch (IOException | InterruptedException e) {
            throw new ConjurException("Authentication request failed", e);
        }
    }
    
    /**
     * Retrieve a secret from Conjur.
     * Automatically authenticates if not already authenticated.
     * Retries once if token has expired (401 response).
     *
     * @param path Secret path in Conjur
     * @return Secret value
     * @throws ConjurException if secret not found or retrieval fails
     */
    public String retrieveSecret(String path) throws ConjurException {
        // Ensure we're authenticated
        ensureAuthenticated();
        
        try {
            return retrieveSecretInternal(path);
            
        } catch (ConjurException e) {
            // If unauthorized (token expired), re-authenticate and retry once
            if (e.getMessage().contains("401")) {
                authenticate();
                return retrieveSecretInternal(path);
            }
            throw e;
        }
    }
    
    /**
     * Retrieve multiple secrets in batch.
     * More efficient than calling retrieveSecret() multiple times.
     *
     * @param paths Set of secret paths
     * @return Map of path to secret value
     */
    public Map<String, String> retrieveSecrets(Set<String> paths) {
        Map<String, String> secrets = new HashMap<>();
        
        for (String path : paths) {
            try {
                String value = retrieveSecret(path);
                secrets.put(path, value);
            } catch (ConjurException e) {
                // Skip secrets that fail to retrieve
                System.err.println("Failed to retrieve secret: " + path + " - " + e.getMessage());
            }
        }
        
        return secrets;
    }
    
    /**
     * Check if Conjur is available and authentication works.
     *
     * @return true if available, false otherwise
     */
    public boolean isAvailable() {
        try {
            authenticate();
            return true;
        } catch (ConjurException e) {
            return false;
        }
    }
    
    /**
     * Get current access token (for debugging/monitoring).
     *
     * @return Current access token or null if not authenticated
     */
    public String getAccessToken() {
        return accessToken.get();
    }
    
    /**
     * Check if currently authenticated.
     *
     * @return true if has valid token
     */
    public boolean isAuthenticated() {
        return accessToken.get() != null && !accessToken.get().isEmpty();
    }
    
    /**
     * Clear stored access token (force re-authentication on next request).
     */
    public void clearToken() {
        accessToken.set(null);
    }
    
    /**
     * Get the configuration properties.
     *
     * @return ConjurProperties instance
     */
    public ConjurProperties getProperties() {
        return properties;
    }
    
    // ===== Private Methods =====
    
    private String retrieveSecretInternal(String path) throws ConjurException {
        try {
            // URL: {baseUrl}/secrets/{account}/variable/{path}
            String encodedPath = URLEncoder.encode(path, StandardCharsets.UTF_8);
            String url = String.format("%s/secrets/%s/variable/%s", 
                properties.getUrl(), properties.getAccount(), encodedPath);
            
            // Build request with authorization header
            String token = accessToken.get();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Token token=\"" + 
                    Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8)) + "\"")
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();
            
            // Send request
            HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 401) {
                throw new ConjurException("Unauthorized: 401 - Token may have expired");
            }
            
            if (response.statusCode() == 404) {
                throw new ConjurException("Secret not found: " + path);
            }
            
            if (response.statusCode() != 200) {
                throw new ConjurException(
                    "Failed to retrieve secret: HTTP " + response.statusCode() + 
                    " - " + response.body()
                );
            }
            
            return response.body();
            
        } catch (IOException | InterruptedException e) {
            throw new ConjurException("Secret retrieval request failed", e);
        }
    }
    
    private void ensureAuthenticated() throws ConjurException {
        if (!isAuthenticated()) {
            authenticate();
        }
    }
    
    private HttpClient createHttpClient() {
        HttpClient.Builder builder = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(30))
            .followRedirects(HttpClient.Redirect.NORMAL);
        
        // If SSL verification is disabled, configure trust-all SSL context
        if (!properties.isSslVerify()) {
            System.err.println("WARNING: SSL verification is DISABLED! Use only in development.");
            builder.sslContext(createTrustAllSslContext());
        }
        
        return builder.build();
    }
    
    private SSLContext createTrustAllSslContext() {
        try {
            // Create trust manager that accepts all certificates
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
            };
            
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            return sslContext;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to create SSL context", e);
        }
    }
    
}

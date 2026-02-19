package edu.m4z.secrets.encryption;

import edu.m4z.secrets.config.SecretsProperties;
import edu.m4z.secrets.cache.EncryptedSecret;
import edu.m4z.secrets.exception.SecretEncryptionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.ConfigurableEnvironment;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

/**
 * Service for encrypting and decrypting secrets using AES-GCM.
 * 
 * <p>Uses AES-256 in GCM mode for authenticated encryption.
 * Provides integrity and confidentiality for cached secrets.
 * 
 * @author Mehrez Ben Salem
 * @since 1.0.0
 */
@Slf4j
public class SecretEncryptionService {
    
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    private static final int AES_KEY_SIZE = 256;
    
    private SecretKey masterKey;

    public SecretEncryptionService(){
        log.info("Secret encryption service initialized with AES-{}", AES_KEY_SIZE);
    }

    public void initialize(ConfigurableEnvironment environment){
        SecretsProperties properties = Binder.get(environment)
                .bind("secrets", SecretsProperties.class)
                .orElseGet(SecretsProperties::new);

        initialize(properties);
    }

    public void initialize(SecretsProperties properties){
        this.masterKey = initializeMasterKey(properties.getEncryption().getKey());
    }

    /**
     * Encrypt a plaintext secret.
     * 
     * @param plaintext the secret value to encrypt
     * @return encrypted secret with metadata
     * @throws SecretEncryptionException if encryption fails
     */
    public EncryptedSecret encrypt(String plaintext) {
        try {
            byte[] iv = generateIV();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, masterKey, parameterSpec);
            
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            String hash = computeHash(plaintext);
            
            return EncryptedSecret.builder()
                .encryptedValue(encrypted)
                .iv(iv)
                .hash(hash)
                .cachedAt(Instant.now())
                .ttl(-1) // Default: no expiration
                .build();
                
        } catch (Exception e) {
            throw new SecretEncryptionException("Failed to encrypt secret", e);
        }
    }
    
    /**
     * Decrypt an encrypted secret.
     * 
     * @param encrypted the encrypted secret
     * @return the plaintext value
     * @throws SecretEncryptionException if decryption fails
     */
    public String decrypt(EncryptedSecret encrypted) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, encrypted.getIv());
            cipher.init(Cipher.DECRYPT_MODE, masterKey, parameterSpec);
            
            byte[] decrypted = cipher.doFinal(encrypted.getEncryptedValue());
            return new String(decrypted, StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            throw new SecretEncryptionException("Failed to decrypt secret", e);
        }
    }
    
    /**
     * Compute SHA-256 hash of a value.
     * Used for rotation detection without decryption.
     * 
     * @param value the value to hash
     * @return Base64-encoded hash
     */
    public String computeHash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new SecretEncryptionException("Failed to compute hash", e);
        }
    }
    
    /**
     * Initialize the master encryption key.
     * 
     * @param masterKeyBase64 Base64-encoded key, or null to generate
     * @return the master key
     */
    private SecretKey initializeMasterKey(String masterKeyBase64) {
        try {
            if (masterKeyBase64 != null && !masterKeyBase64.isEmpty()) {
                log.info("Using provided master encryption key");
                byte[] decodedKey = Base64.getDecoder().decode(masterKeyBase64);
                return new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
            } else {
                log.warn("No master encryption key provided, generating random key (NOT FOR PRODUCTION!)");
                KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
                keyGenerator.init(AES_KEY_SIZE, new SecureRandom());
                return keyGenerator.generateKey();
            }
        } catch (Exception e) {
            throw new SecretEncryptionException("Failed to initialize master key", e);
        }
    }
    
    /**
     * Generate a random initialization vector for GCM mode.
     * 
     * @return random IV bytes
     */
    private byte[] generateIV() {
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        return iv;
    }
    
    /**
     * Get the Base64-encoded current master key.
     * Useful for backup/export (use with extreme caution!).
     * 
     * @return Base64-encoded master key
     */
    public String exportMasterKey() {
        return Base64.getEncoder().encodeToString(masterKey.getEncoded());
    }
}

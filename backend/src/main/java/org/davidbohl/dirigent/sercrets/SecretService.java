package org.davidbohl.dirigent.sercrets;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
public class SecretService {

    private static final String ALGORITHM = "AES";

    private final String encryptionKey;
    private final SecretRepository secretRepository;

    public SecretService(@Value("${dirigent.secrets.encryption.key}") String encryptionKey, SecretRepository secretRepository) {

        if (encryptionKey == null || encryptionKey.length() != 16) {
            throw new IllegalArgumentException("SECRET_ENCRYPTION_KEY muss 16 Zeichen lang sein!<" + encryptionKey + ">");
        }

        this.encryptionKey = encryptionKey;
        this.secretRepository = secretRepository;
    }

    public void saveSecret(String environmentVariable, String value, List<String> deployments) {
        try {
            String encrypted = encrypt(value);
            Secret secret = new Secret(null, environmentVariable, encrypted, deployments);
            secretRepository.save(secret);
        } catch (Exception e) {
            throw new RuntimeException("Saving Secret failed", e);
        }
    }

    public Map<String, String> getAllSecretsAsEnvironmentVariableMapByDeployment(String deployment) { 
        List<Secret> secrets = secretRepository.findByDeploymentsContaining(deployment);
        Map<String, String> result = new HashMap<>();

        for (Secret secret : secrets) {
            result.put(secret.getEnvironmentVariable(), getSecret(secret.getEncryptedValue()));
        }

        return result;
    }

    public List<SecretDto> getAllSecretsWithoutValues() {
        return secretRepository.findAll().stream().map(
                s -> new SecretDto(s.getEnvironmentVariable(), null, s.getDeployments())
            ).toList();
    }

    private String getSecret(String key) {
        try {
            Secret secret = secretRepository.findByKey(key).orElseThrow();
            return decrypt(secret.getEncryptedValue());
        } catch (Exception e) {
            throw new RuntimeException("Reading Secret failed", e);
        }
    }

    private String encrypt(String value) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(encryptionKey.getBytes(), ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        return Base64.getEncoder().encodeToString(cipher.doFinal(value.getBytes()));
    }

    private String decrypt(String encrypted) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(encryptionKey.getBytes(), ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        return new String(cipher.doFinal(Base64.getDecoder().decode(encrypted)));
    }

}


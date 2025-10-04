package org.davidbohl.dirigent.sercrets;

import lombok.extern.slf4j.Slf4j;
import org.davidbohl.dirigent.deployments.events.MultipleNamedDeploymentsStartRequestedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.*;


@Service
@Slf4j
public class SecretService {

    private static final String ALGORITHM = "AES";

    private final String encryptionKey;
    private final SecretRepository secretRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public SecretService(@Value("${dirigent.secrets.encryption.key}") String encryptionKey, SecretRepository secretRepository, ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;

        if (encryptionKey == null || encryptionKey.length() != 16) {
            throw new IllegalArgumentException("SECRET_ENCRYPTION_KEY must have a length of 16 characters!<" + encryptionKey + ">");
        }

        this.encryptionKey = encryptionKey;
        this.secretRepository = secretRepository;
    }

    public void saveSecret(String key, String environmentVariable, String value, List<String> deployments) {
        try {

            Secret secret = secretRepository.findById(key).orElseGet(() -> new Secret(key, environmentVariable, value, deployments));

            secret.setDeployments(deployments);
            secret.setEnvironmentVariable(environmentVariable);

            if(value != null )
                secret.setEncryptedValue(encrypt(value));

            secretRepository.save(secret);

            applicationEventPublisher.publishEvent(new MultipleNamedDeploymentsStartRequestedEvent(this, secret.getDeployments(), true));

        } catch (Exception e) {
            throw new RuntimeException("Saving Secret failed", e);
        }
    }

    public Map<String, String> getAllSecretsAsEnvironmentVariableMapByDeployment(String deployment) { 
        List<Secret> secrets = secretRepository.findAllByDeploymentsContaining(deployment);
        Map<String, String> result = new HashMap<>();

        for (Secret secret : secrets) {
            try {
                result.put(secret.getEnvironmentVariable(), decrypt(secret.getEncryptedValue()));
            } catch(Exception ex) {
                log.error("Failed to decrypt secret <{}> for Env Var <{}> and Deployment <{}>.", secret.getKey(), secret.getEnvironmentVariable(), deployment);
                throw new RuntimeException(ex);
            }
        }

        return result;
    }

    public List<SecretDto> getAllSecretsWithoutValues() {
        return secretRepository.findAll().stream().map(
                s -> new SecretDto(s.getKey(), s.getEnvironmentVariable(), null, s.getDeployments())
            ).toList();
    }

    public void deleteSecret(String key) {
        Optional<Secret> byId = this.secretRepository.findById(key);

        if(byId.isEmpty()) return;

        Secret secret = byId.get();
        secretRepository.deleteById(key);
        applicationEventPublisher.publishEvent(new MultipleNamedDeploymentsStartRequestedEvent(this, secret.getDeployments(), true));
    }

    private String encrypt(String value) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(encryptionKey.getBytes(), ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        return Base64.getEncoder().encodeToString(cipher.doFinal(value.getBytes()));
    }

    private String decrypt(String encrypted) throws Exception {

        if(encrypted == null)
            return null;

        SecretKeySpec keySpec = new SecretKeySpec(encryptionKey.getBytes(), ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        return new String(cipher.doFinal(Base64.getDecoder().decode(encrypted)));
    }

}


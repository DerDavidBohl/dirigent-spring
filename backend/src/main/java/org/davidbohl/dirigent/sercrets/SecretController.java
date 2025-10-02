package org.davidbohl.dirigent.sercrets;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping(path = "/api/v1/secrets")
public class SecretController {

    private final SecretService secretService;

    
    public SecretController(SecretService secretService) {
        this.secretService = secretService;
    }

    @PutMapping
    public void saveSecret(SecretDto secret) {
        this.secretService.saveSecret(secret.environmentVariable(), secret.value(), secret.deployments());
    }

    @GetMapping
    public List<SecretDto> getSecrets() {
        return this.secretService.getAllSecretsWithoutValues();
    }
    
    
}
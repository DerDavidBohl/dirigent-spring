package org.davidbohl.dirigent.sercrets;

import java.util.List;

import org.davidbohl.dirigent.sercrets.model.SecretDto;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@RestController()
@RequestMapping(path = "/api/v1/secrets")
@Slf4j
public class SecretController {

    private final SecretService secretService;

    
    public SecretController(SecretService secretService) {
        this.secretService = secretService;
    }

    @PutMapping("{key}")
    public void saveSecret(@RequestBody SecretDto secret, @PathVariable String key, @RequestParam(required = false, defaultValue = "false") boolean restartDeployments) {
        this.secretService.saveSecret(key, secret.environmentVariable(), secret.value(), secret.deployments(), restartDeployments);
    }

    @DeleteMapping("{key}")
    public void deleteSecret(@PathVariable String key, @RequestParam(required = false, defaultValue = "false") boolean restartDeployments) {
        this.secretService.deleteSecret(key, restartDeployments);
    }

    @GetMapping
    public List<SecretDto> getSecrets() {
        return this.secretService.getAllSecretsWithoutValues();
    }
    
    
}
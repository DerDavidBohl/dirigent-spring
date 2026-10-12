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
import org.springframework.security.access.annotation.Secured;

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
    @Secured({"ROLE_ADMIN", "SCOPE_secrets:write"})
    public void saveSecret(@RequestBody SecretDto secret, @PathVariable String key, @RequestParam(required = false, defaultValue = "false") boolean restartDeployments) {
        this.secretService.saveSecret(key, secret.environmentVariable(), secret.value(), secret.deployments(), restartDeployments);
    }

    @DeleteMapping("{key}")
    @Secured({"ROLE_ADMIN", "SCOPE_secrets:write"})
    public void deleteSecret(@PathVariable String key, @RequestParam(required = false, defaultValue = "false") boolean restartDeployments) {
        this.secretService.deleteSecret(key, restartDeployments);
    }

    @GetMapping
    @Secured({"ROLE_ADMIN", "SCOPE_secrets:read"})
    public List<SecretDto> getSecrets() {
        return this.secretService.getAllSecretsWithoutValues();
    }
    
    
}
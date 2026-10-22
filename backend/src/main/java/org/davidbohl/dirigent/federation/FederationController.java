package org.davidbohl.dirigent.federation;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/federation")
@Secured("ROLE_ADMIN")
public class FederationController {

    private static final Logger log = LoggerFactory.getLogger(FederationController.class);

    private final FederationService federationService;

    public FederationController(FederationService federationService) {
        this.federationService = federationService;
    }

    @GetMapping("/instances")
    public List<FederatedInstance> getInstances() {
        return federationService.getInstances();
    }

    @GetMapping("/instances/{id}/system-information")
    public ResponseEntity<String> getSystemInformation(@PathVariable String id) {
        return forward(id, HttpMethod.GET, "/api/v1/system-information", null, null);
    }

    @GetMapping("/instances/{id}/deployments")
    public ResponseEntity<String> getDeployments(@PathVariable String id) {
        return forward(id, HttpMethod.GET, "/api/v1/deployments", null, null);
    }

    @PostMapping("/instances/{id}/deployments/all/start")
    public ResponseEntity<String> startAll(@PathVariable String id,
            @RequestParam(required = false, defaultValue = "false") boolean forceRecreate) {
        return forward(id, HttpMethod.POST, "/api/v1/deployments/all/start",
            "forceRecreate=" + forceRecreate, null, "start-all");
    }

    @PostMapping("/instances/{id}/deployments/{name}/start")
    public ResponseEntity<String> start(@PathVariable String id, @PathVariable String name,
            @RequestParam(required = false, defaultValue = "false") boolean forceRecreate) {
        return forward(id, HttpMethod.POST, "/api/v1/deployments/" + encode(name) + "/start",
                "forceRecreate=" + forceRecreate, null, "start-deployment");
    }

    @PostMapping("/instances/{id}/deployments/{name}/stop")
    public ResponseEntity<String> stop(@PathVariable String id, @PathVariable String name) {
        return forward(id, HttpMethod.POST, "/api/v1/deployments/" + encode(name) + "/stop",
            null, null, "stop-deployment");
    }

    @GetMapping("/instances/{id}/deployment-updates")
    public ResponseEntity<String> getUpdates(@PathVariable String id) {
        return forward(id, HttpMethod.GET, "/api/v1/deployment-updates", null, null);
    }

    @PostMapping("/instances/{id}/deployment-updates/check")
    public ResponseEntity<String> checkUpdates(@PathVariable String id) {
        return forward(id, HttpMethod.POST, "/api/v1/deployment-updates/check", null, null, "check-updates");
    }

    @PostMapping("/instances/{id}/deployment-updates/run")
    public ResponseEntity<String> runUpdate(@PathVariable String id, @RequestBody Object update) {
        return forward(id, HttpMethod.POST, "/api/v1/deployment-updates/run", null, update, "apply-update");
    }

    @GetMapping("/instances/{id}/secrets")
    public ResponseEntity<String> getSecrets(@PathVariable String id) {
        return forward(id, HttpMethod.GET, "/api/v1/secrets", null, null);
    }

    @PutMapping("/instances/{id}/secrets/{key}")
    public ResponseEntity<String> saveSecret(@PathVariable String id, @PathVariable String key,
            @RequestParam(required = false, defaultValue = "false") boolean restartDeployments,
            @RequestBody Object secret) {
        return forward(id, HttpMethod.PUT, "/api/v1/secrets/" + encode(key),
                "restartDeployments=" + restartDeployments, secret, "write-secret");
    }

    @DeleteMapping("/instances/{id}/secrets/{key}")
    public ResponseEntity<String> deleteSecret(@PathVariable String id, @PathVariable String key,
            @RequestParam(required = false, defaultValue = "false") boolean restartDeployments) {
        return forward(id, HttpMethod.DELETE, "/api/v1/secrets/" + encode(key),
                "restartDeployments=" + restartDeployments, null, "delete-secret");
    }

    private ResponseEntity<String> forward(String id, HttpMethod method, String path, String query, Object body) {
        return forward(id, method, path, query, body, null);
    }

    private ResponseEntity<String> forward(String id, HttpMethod method, String path, String query, Object body,
            String auditOperation) {
        try {
            FederationRemoteClient.RemoteResponse response = federationService.forward(id, method, path, query, body);
            if (auditOperation != null) {
                audit(id, auditOperation, response.status() >= 200 && response.status() < 300 ? "accepted" : "rejected",
                        response.status());
            }
                if (response.status() == HttpStatus.UNAUTHORIZED.value()
                    || response.status() == HttpStatus.FORBIDDEN.value()) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"Remote instance authorization failed\"}");
                }
            ResponseEntity.BodyBuilder result = ResponseEntity.status(response.status());
            if (response.body() == null || response.body().isBlank()) {
                return result.build();
            }
            return result.contentType(MediaType.APPLICATION_JSON).body(response.body());
        } catch (FederationService.UnknownFederatedInstanceException exception) {
            if (auditOperation != null) {
                audit(id, auditOperation, "failed", 404);
            }
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            if (auditOperation != null) {
                audit(id, auditOperation, "failed", 502);
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Remote instance request failed");
        } catch (Exception exception) {
            if (auditOperation != null) {
                audit(id, auditOperation, "failed", 502);
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Remote instance request failed");
        }
    }

    private void audit(String id, String operation, String outcome, int status) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String actor = authentication == null ? "unknown"
            : String.valueOf(authentication.getName()).replaceAll("[\\p{Cntrl}]", "_");
        String target = id != null && id.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,63}") ? id : "invalid";
        log.info("Federation write actor={} instance={} operation={} outcome={} status={}",
                actor, target, operation, outcome, status);
    }

    private static String encode(String value) {
        if (value == null || value.isBlank() || value.equals(".") || value.equals("..")
                || value.contains("/") || value.contains("\\") || value.chars().anyMatch(Character::isISOControl)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
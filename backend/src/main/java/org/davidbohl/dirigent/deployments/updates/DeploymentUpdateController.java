package org.davidbohl.dirigent.deployments.updates;

import java.util.List;

import org.davidbohl.dirigent.deployments.updates.dto.DeploymentUpdateDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.annotation.Secured;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/deployment-updates")
@RequiredArgsConstructor
public class DeploymentUpdateController {

    private final DeploymentUpdateService deploymentUpdateService;
    @GetMapping
    @Secured({"ROLE_ADMIN", "SCOPE_updates:read"})
    public List<DeploymentUpdateDto> getDeploymentUpdates() {
        return deploymentUpdateService.getDeploymentUpdates();
    }

    @PostMapping("run")
    @Secured({"ROLE_ADMIN", "SCOPE_updates:write"})
    public void postDeploymentUpdate(@RequestBody DeploymentUpdateDto deploymentUpdate) {
        deploymentUpdateService.runDeploymentUpdate(deploymentUpdate);
    }

    @PostMapping("check") 
    @Secured({"ROLE_ADMIN", "SCOPE_updates:write"})
    public void podtDeploymentUpdateCheck() {
        deploymentUpdateService.checkAllDeploymentForUpdates();
    }

}

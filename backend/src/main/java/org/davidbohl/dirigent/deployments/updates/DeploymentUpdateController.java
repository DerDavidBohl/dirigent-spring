package org.davidbohl.dirigent.deployments.updates;

import java.util.List;

import org.davidbohl.dirigent.deployments.updates.dto.DeploymentUpdateDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/deployment-updates")
@RequiredArgsConstructor
public class DeploymentUpdateController {

    private final DeploymentUpdateService deploymentUpdateService;
    @GetMapping
    public List<DeploymentUpdateDto> getDeploymentUpdates() {
        return deploymentUpdateService.getDeploymentUpdates();
    }

    @PostMapping("run")
    public void postDeploymentUpdate(@RequestBody DeploymentUpdateDto deploymentUpdate) {
        deploymentUpdateService.updateDeployment(deploymentUpdate);
    }

    @PostMapping("check") 
    public void podtDeploymentUpdateCheck() {
        deploymentUpdateService.checkAllDeploymentForUpdates();
    }

}

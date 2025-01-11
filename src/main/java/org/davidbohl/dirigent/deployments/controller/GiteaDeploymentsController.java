package org.davidbohl.dirigent.deployments.controller;

import org.davidbohl.dirigent.deployments.models.GiteaRequestBody;
import org.davidbohl.dirigent.deployments.service.DeploymentsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping("/api/v1/gitea")
public class GiteaDeploymentsController {

    private final DeploymentsService deploymentsService;

    public GiteaDeploymentsController(DeploymentsService deploymentsService) {
        this.deploymentsService = deploymentsService;
    }

    @PostMapping()
    public void webHook(@RequestBody GiteaRequestBody body) {
        deploymentsService.startSingleDeploymentBySource(body.repository().url());
    }

}

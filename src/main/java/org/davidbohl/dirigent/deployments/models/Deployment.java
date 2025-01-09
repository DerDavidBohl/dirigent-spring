package org.davidbohl.dirigent.deployments.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record Deployment (
        String name, // ToDo: Validate name is not "all", cause its used in controller
        String source,
        @JsonIgnore int order,
        @JsonIgnore boolean restartOnDeployment) {
}

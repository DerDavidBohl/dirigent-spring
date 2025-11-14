package org.davidbohl.dirigent.deployments.models;

public record Deployment (
        String name, // ToDo: Validate name is not "all", cause its used in controller
        String source,
        int order,
        String ref
) {
}

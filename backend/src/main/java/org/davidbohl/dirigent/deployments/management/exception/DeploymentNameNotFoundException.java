package org.davidbohl.dirigent.deployments.management.exception;

public class DeploymentNameNotFoundException extends RuntimeException {
    public DeploymentNameNotFoundException(String name) {
        super(String.format("Could not find Deployment with name '%s'", name));
    }
}

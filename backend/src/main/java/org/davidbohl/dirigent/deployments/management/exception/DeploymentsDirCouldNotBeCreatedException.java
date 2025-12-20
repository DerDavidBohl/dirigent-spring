package org.davidbohl.dirigent.deployments.management.exception;

public class DeploymentsDirCouldNotBeCreatedException extends RuntimeException {

    public DeploymentsDirCouldNotBeCreatedException() {
        super("Deployments directory could not be created");
    }
}

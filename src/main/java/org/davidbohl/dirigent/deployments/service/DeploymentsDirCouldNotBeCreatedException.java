package org.davidbohl.dirigent.deployments.service;

public class DeploymentsDirCouldNotBeCreatedException extends RuntimeException {

    public DeploymentsDirCouldNotBeCreatedException() {
        super("Deployments directory could not be created");
    }
}

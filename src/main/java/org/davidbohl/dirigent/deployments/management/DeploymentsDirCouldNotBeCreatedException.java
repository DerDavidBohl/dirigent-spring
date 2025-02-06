package org.davidbohl.dirigent.deployments.management;

public class DeploymentsDirCouldNotBeCreatedException extends RuntimeException {

    public DeploymentsDirCouldNotBeCreatedException() {
        super("Deployments directory could not be created");
    }
}

package org.davidbohl.dirigent.deployments.config;

public class DeploymentsConfigurationReadFailed extends RuntimeException {
    public DeploymentsConfigurationReadFailed(Throwable cause) {

        super("Reading of configuration failed", cause);
    }
}

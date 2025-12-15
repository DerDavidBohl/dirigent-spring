package org.davidbohl.dirigent.deployments.config.exception;

public class DeploymentsConfigurationReadFailedException extends RuntimeException {
    public DeploymentsConfigurationReadFailedException(Throwable cause) {

        super("Reading of configuration failed", cause);
    }
}

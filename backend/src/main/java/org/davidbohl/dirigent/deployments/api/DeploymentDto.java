package org.davidbohl.dirigent.deployments.api;

import org.davidbohl.dirigent.deployments.state.DeploymentState;

public record DeploymentDto(String name,
                            String source,
                            int order, DeploymentState.State state) {
}

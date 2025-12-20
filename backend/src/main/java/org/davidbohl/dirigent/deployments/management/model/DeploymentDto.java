package org.davidbohl.dirigent.deployments.management.model;

import org.davidbohl.dirigent.deployments.state.entity.DeploymentStateEntity;

public record DeploymentDto(String name,
                            String source,
                            int order, DeploymentStateEntity.State state, String message) {
}

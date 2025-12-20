package org.davidbohl.dirigent.deployments.state;

import org.davidbohl.dirigent.deployments.state.entity.DeploymentStateEntity;
import org.springframework.data.repository.CrudRepository;

public interface DeploymentStateRepository extends CrudRepository<DeploymentStateEntity, String> {
}

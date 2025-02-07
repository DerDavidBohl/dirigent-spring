package org.davidbohl.dirigent.deployments.state;

import org.springframework.data.repository.CrudRepository;

interface DeploymentStateRepository extends CrudRepository<DeploymentState, String> {
}

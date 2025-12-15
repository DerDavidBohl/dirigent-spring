package org.davidbohl.dirigent.deployments.updates;

import java.util.List;
import java.util.UUID;
import org.davidbohl.dirigent.deployments.updates.entity.DeploymentUpdateEntity;
import org.springframework.data.repository.CrudRepository;
public interface DeploymentUpdateRepository extends CrudRepository<DeploymentUpdateEntity, UUID> {
    List<DeploymentUpdateEntity> findAllByDeploymentName(String deploymentName);
    List<DeploymentUpdateEntity> findAllByDeploymentNameAndServiceAndImage(String deploymentName, String service, String image);
    void deleteAllByDeploymentName(String deploymentName);
    
}
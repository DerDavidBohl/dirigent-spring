package org.davidbohl.dirigent.sercrets;

import java.util.List;

import org.davidbohl.dirigent.sercrets.entity.SecretEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SecretRepository extends JpaRepository<SecretEntity, String> {

    List<SecretEntity> findAllByDeploymentsContaining(String deployment);
    List<SecretEntity> findAllByEnvironmentVariableAndDeploymentsContaining(String environmentVariable, String deployment);

}
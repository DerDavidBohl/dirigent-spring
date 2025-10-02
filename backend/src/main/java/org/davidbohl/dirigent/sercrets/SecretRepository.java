package org.davidbohl.dirigent.sercrets;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SecretRepository extends JpaRepository<Secret, String> {

    List<Secret> findAllByDeploymentsContaining(String deployment);
    List<Secret> findAllByEnvironmentVariableAndDeploymentsContaining(String environmentVariable, String deployment);

}
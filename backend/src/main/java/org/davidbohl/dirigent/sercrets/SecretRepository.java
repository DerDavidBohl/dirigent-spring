package org.davidbohl.dirigent.sercrets;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SecretRepository extends JpaRepository<Secret, Long> {

    Optional<Secret> findByKey(String key);

    List<Secret> findByDeploymentsContaining(String deployment);

}
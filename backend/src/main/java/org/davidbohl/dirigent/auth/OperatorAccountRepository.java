package org.davidbohl.dirigent.auth;

import java.util.Optional;

import org.davidbohl.dirigent.auth.entity.OperatorAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperatorAccountRepository extends JpaRepository<OperatorAccountEntity, Long> {

    Optional<OperatorAccountEntity> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    long countByEnabledTrueAndPasswordHashIsNotNull();

}
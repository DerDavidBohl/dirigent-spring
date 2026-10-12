package org.davidbohl.dirigent.auth;

import java.util.List;
import java.util.Optional;

import org.davidbohl.dirigent.auth.entity.MachineTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MachineTokenRepository extends JpaRepository<MachineTokenEntity, Long> {

    Optional<MachineTokenEntity> findByTokenHashAndRevokedAtIsNull(String tokenHash);

    List<MachineTokenEntity> findAllByOrderByCreatedAtDesc();
}
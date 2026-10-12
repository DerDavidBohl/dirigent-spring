package org.davidbohl.dirigent.auth.dto;

import java.time.Instant;
import java.util.Set;

public record MachineTokenCreatedResponse(Long id, String name, String token, Set<String> permissions,
                                          Instant createdAt, Instant expiresAt) {
}
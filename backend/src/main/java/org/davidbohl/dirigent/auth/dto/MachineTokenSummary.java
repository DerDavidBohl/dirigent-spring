package org.davidbohl.dirigent.auth.dto;

import java.time.Instant;
import java.util.Set;

public record MachineTokenSummary(Long id, String name, Set<String> permissions, String createdBy,
                                 Instant createdAt, Instant expiresAt, Instant revokedAt, Instant lastUsedAt) {
}
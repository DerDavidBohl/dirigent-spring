package org.davidbohl.dirigent.auth.dto;

import java.time.Instant;
import java.util.Set;

public record MachineTokenCreateRequest(String name, Set<String> permissions, Instant expiresAt) {
}
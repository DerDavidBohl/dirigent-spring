package org.davidbohl.dirigent.auth.dto;

public record AuthenticatedUserResponse(String username, boolean apiToken) {
}
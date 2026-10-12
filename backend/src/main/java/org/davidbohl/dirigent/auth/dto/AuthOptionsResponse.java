package org.davidbohl.dirigent.auth.dto;

public record AuthOptionsResponse(boolean oidcEnabled, String oidcLoginPath) {
}
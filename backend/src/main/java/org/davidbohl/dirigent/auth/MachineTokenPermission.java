package org.davidbohl.dirigent.auth;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum MachineTokenPermission {
    DEPLOYMENTS_READ("deployments:read"),
    DEPLOYMENTS_WRITE("deployments:write"),
    SECRETS_READ("secrets:read"),
    SECRETS_WRITE("secrets:write"),
    UPDATES_READ("updates:read"),
    UPDATES_WRITE("updates:write"),
    SYSTEM_READ("system:read"),
    WEBHOOKS_TRIGGER("webhooks:trigger");

    private final String value;

    MachineTokenPermission(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static Set<String> valuesAsSet() {
        return Arrays.stream(values()).map(permission -> permission.value()).collect(Collectors.toUnmodifiableSet());
    }
}
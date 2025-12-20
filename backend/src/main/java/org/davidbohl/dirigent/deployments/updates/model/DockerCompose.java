package org.davidbohl.dirigent.deployments.updates.model;

import java.util.Map;

public record DockerCompose(Map<String, ComposeService> services) {

}

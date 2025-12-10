package org.davidbohl.dirigent.deployments.updates;

import java.util.Map;

public record DockerCompose(Map<String, ComposeService> services) {

}

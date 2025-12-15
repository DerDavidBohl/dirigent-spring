package org.davidbohl.dirigent.deployments.updates.model;

import java.util.Map;

import org.davidbohl.dirigent.deployments.updates.ComposeService;

public record DockerCompose(Map<String, ComposeService> services) {

}

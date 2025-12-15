package org.davidbohl.dirigent.deployments.updates.model;

public record DockerImage(String registryEndpoint, String image, String tag) {

}

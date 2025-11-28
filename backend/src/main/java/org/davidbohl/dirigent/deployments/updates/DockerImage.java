package org.davidbohl.dirigent.deployments.updates;

public record DockerImage(String registryEndpoint, String image, String tag) {

}

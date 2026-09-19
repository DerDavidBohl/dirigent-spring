package org.davidbohl.dirigent.deployments.updates;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.davidbohl.dirigent.deployments.updates.model.DockerImage;
import org.junit.jupiter.api.Test;

class DeploymentUpdateServiceParserTest {

    private final DeploymentUpdateService service = new DeploymentUpdateService(null, null, null, null, null);

    @Test
    void parsesDockerHubOfficialImageWithTag() {
        assertEquals(
                new DockerImage("https://registry-1.docker.io/v2/", "library/traefik", "v3"),
                service.parseDockerImage("traefik:v3"));
    }

    @Test
    void parsesRegistryWithPort() {
        assertEquals(
                new DockerImage("https://localhost:5000/v2/", "traefik", "v3"),
                service.parseDockerImage("localhost:5000/traefik:v3"));
    }

    @Test
    void parsesDigestReference() {
        assertEquals(
                new DockerImage("https://ghcr.io/v2/", "owner/traefik", "sha256:abc"),
                service.parseDockerImage("ghcr.io/owner/traefik@sha256:abc"));
    }

    @Test
    void parsesTagAndDigestReference() {
        assertEquals(
                new DockerImage("https://ghcr.io/v2/", "owner/traefik", "sha256:abc"),
                service.parseDockerImage("ghcr.io/owner/traefik:v3@sha256:abc"));
    }
}

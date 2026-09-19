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
    void defaultsDockerHubImageTagToLatest() {
        assertEquals(
                new DockerImage("https://registry-1.docker.io/v2/", "library/traefik", "latest"),
                service.parseDockerImage("traefik"));
    }

    @Test
    void parsesExplicitDockerHubImage() {
        assertEquals(
                new DockerImage("https://registry-1.docker.io/v2/", "library/traefik", "v3"),
                service.parseDockerImage("docker.io/library/traefik:v3"));
    }

    @Test
    void parsesRegistryImageWithoutTag() {
        assertEquals(
                new DockerImage("https://quay.io/v2/", "prometheus/node-exporter", "latest"),
                service.parseDockerImage("quay.io/prometheus/node-exporter"));
    }

    @Test
    void parsesLocalhostRegistryWithoutPort() {
        assertEquals(
                new DockerImage("https://localhost/v2/", "traefik", "v3"),
                service.parseDockerImage("localhost/traefik:v3"));
    }

    @Test
    void parsesNestedRepository() {
        assertEquals(
                new DockerImage("https://registry.example.com/v2/", "team/platform/traefik", "v3"),
                service.parseDockerImage("registry.example.com/team/platform/traefik:v3"));
    }

    @Test
    void parsesIpv6Registry() {
        assertEquals(
                new DockerImage("https://[::1]:5000/v2/", "traefik", "v3"),
                service.parseDockerImage("[::1]:5000/traefik:v3"));
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

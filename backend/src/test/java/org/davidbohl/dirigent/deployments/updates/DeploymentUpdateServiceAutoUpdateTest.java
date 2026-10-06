package org.davidbohl.dirigent.deployments.updates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.davidbohl.dirigent.deployments.config.model.Deployment;
import org.davidbohl.dirigent.deployments.config.model.DeploynentConfiguration;
import org.davidbohl.dirigent.deployments.updates.dto.DeploymentUpdateDto;
import org.davidbohl.dirigent.deployments.updates.entity.DeploymentUpdateEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

// REQ-014
class DeploymentUpdateServiceAutoUpdateTest {

    private List<DeploymentUpdateEntity> existing;
    private List<DeploymentUpdateEntity> saved;
    private List<DeploymentUpdateDto> applied;
    private DeploymentUpdateService service;

    @BeforeEach
    void setUp() {
        existing = new ArrayList<>();
        saved = new ArrayList<>();
        applied = new ArrayList<>();

        DeploymentUpdateRepository repository = (DeploymentUpdateRepository) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[] { DeploymentUpdateRepository.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "findAllByDeploymentNameAndServiceAndImage" -> existing;
                    case "save" -> {
                        saved.add((DeploymentUpdateEntity) args[0]);
                        yield args[0];
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                });

        service = new DeploymentUpdateService(null, null, null, repository, null, null) {
            @Override
            public void runDeploymentUpdate(DeploymentUpdateDto deploymentUpdate) {
                applied.add(deploymentUpdate);
            }
        };
    }

    private static Deployment deployment(boolean autoUpdate) {
        return new Deployment("app", "https://git/app.git", 0, null, autoUpdate);
    }

    @Test
    void appliesDiscoveredUpdateWhenAutoUpdateEnabled() {
        service.handleDiscoveredUpdate(deployment(true), "web", "img:1");

        assertEquals(1, saved.size());
        assertEquals(List.of(new DeploymentUpdateDto("app", "web", "img:1", false)), applied);
    }

    @Test
    void doesNotApplyDiscoveredUpdateWhenAutoUpdateDisabled() {
        service.handleDiscoveredUpdate(deployment(false), "web", "img:1");

        assertEquals(1, saved.size());
        assertTrue(applied.isEmpty());
    }

    @Test
    void doesNotApplyTwiceWhileUpdateIsRunning() {
        existing.add(new DeploymentUpdateEntity(null, "app", "web", "img:1", true));

        service.handleDiscoveredUpdate(deployment(true), "web", "img:1");

        assertTrue(saved.isEmpty());
        assertTrue(applied.isEmpty());
    }

    @Test
    void appliesExistingPendingUpdateWhenAutoUpdateEnabled() {
        existing.add(new DeploymentUpdateEntity(null, "app", "web", "img:1", false));

        service.handleDiscoveredUpdate(deployment(true), "web", "img:1");

        assertTrue(saved.isEmpty());
        assertEquals(List.of(new DeploymentUpdateDto("app", "web", "img:1", false)), applied);
    }

    @Test
    void autoUpdateDefaultsToFalseInYamlConfiguration() throws Exception {
        String yaml = """
                deployments:
                  - name: a
                    source: s
                  - name: b
                    source: s
                    autoUpdate: true
                """;

        DeploynentConfiguration configuration = new ObjectMapper(new YAMLFactory())
                .readValue(yaml, DeploynentConfiguration.class);

        assertFalse(configuration.deployments().get(0).autoUpdate());
        assertTrue(configuration.deployments().get(1).autoUpdate());
    }
}

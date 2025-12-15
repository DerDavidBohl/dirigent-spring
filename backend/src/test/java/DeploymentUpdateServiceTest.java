import org.davidbohl.dirigent.deployments.config.model.Deployment;
import org.davidbohl.dirigent.deployments.updates.ContainerRegistryClient;
import org.davidbohl.dirigent.deployments.updates.DeploymentUpdateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {ContainerRegistryClient.class, DeploymentUpdateService.class})
public class DeploymentUpdateServiceTest {

    @Autowired
    DeploymentUpdateService updateService;

    @Test
    void testUpdates() {

        updateService.checkIfImageUpdatesExistForDeployment(new Deployment("test1", "", 0, ""));
    }

}
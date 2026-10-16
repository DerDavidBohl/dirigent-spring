import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.davidbohl.dirigent.deployments.updates.DeploymentUpdateRepository;
import org.davidbohl.dirigent.deployments.updates.DeploymentUpdateService;
import org.davidbohl.dirigent.deployments.updates.dto.DeploymentUpdateDto;
import org.davidbohl.dirigent.deployments.updates.entity.DeploymentUpdateEntity;
import org.junit.jupiter.api.Test;

class DeploymentUpdateServiceTest {

    @Test
    void mapsStoredUpdatesToDtos() {
        DeploymentUpdateRepository repository = mock(DeploymentUpdateRepository.class);
        DeploymentUpdateEntity update = new DeploymentUpdateEntity(
                null, "test1", "web", "example/image:latest", false);
        when(repository.findAll()).thenReturn(List.of(update));
        DeploymentUpdateService service = new DeploymentUpdateService(null, null, null, repository, null, null);

        List<DeploymentUpdateDto> updates = service.getDeploymentUpdates();

        assertEquals(List.of(new DeploymentUpdateDto("test1", "web", "example/image:latest", false)), updates);
        verify(repository).findAll();
    }

}
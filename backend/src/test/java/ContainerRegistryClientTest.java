import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import org.davidbohl.dirigent.deployments.updates.ContainerRegistryClient;
import org.davidbohl.dirigent.deployments.updates.CouldNotGetManifestDigestFromRegistryFailedException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient.Request;
import com.github.dockerjava.transport.DockerHttpClient.Response;

@SpringBootTest(classes = ContainerRegistryClient.class)
public class ContainerRegistryClientTest {

@Autowired
private ContainerRegistryClient client;

    @Test
    void checkGhcrIoRegistry() throws CouldNotGetManifestDigestFromRegistryFailedException  {
        client.getRegistryDigest("ghcr.io", "derdavidbohl/dirigent-spring", "latest");
    }
    
    @Test
    void checkDockerImage() throws CouldNotGetManifestDigestFromRegistryFailedException {
        client.getRegistryDigest("https://registry-1.docker.io", "mcp/slack", "latest");
    }

    @Test
    void checkOs() {
        String arch = System.getProperty("os.arch");
        String os = System.getProperty("os.name");

        System.err.println(os + "/" + arch);
    }
    

    @Test
    void dockerClient() {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();

        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();


        Request request = Request.builder()
            .method(Request.Method.GET)
            .path("/_ping")
            .build();

        Response response = httpClient.execute(request);

        assertEquals(200, response.getStatusCode());
        
    }

}

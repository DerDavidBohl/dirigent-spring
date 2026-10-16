import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.davidbohl.dirigent.deployments.updates.ContainerRegistryClient;
import org.davidbohl.dirigent.deployments.updates.RegistryAuthProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

class ContainerRegistryClientTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void retrievesManifestDigestFromLocalRegistry() throws Exception {
        List<String> requestedPaths = new CopyOnWriteArrayList<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            requestedPaths.add(path);
            byte[] body = path.endsWith("/manifests/latest")
                    ? "{\"config\":{\"digest\":\"sha256:local\"}}".getBytes(StandardCharsets.UTF_8)
                    : new byte[0];
            exchange.sendResponseHeaders(200, body.length == 0 ? -1 : body.length);
            if (body.length > 0) {
                try (OutputStream responseBody = exchange.getResponseBody()) {
                    responseBody.write(body);
                }
            }
            exchange.close();
        });
        server.start();

        ContainerRegistryClient client = new ContainerRegistryClient(new RegistryAuthProperties());
        String digest = client.getRegistryDigest(
                "http://127.0.0.1:" + server.getAddress().getPort(), "team/image", "latest");

        assertEquals("sha256:local", digest);
        assertEquals(List.of("/v2/", "/v2/team/image/manifests/latest"), requestedPaths);
    }
}

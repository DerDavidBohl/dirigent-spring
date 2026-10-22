package org.davidbohl.dirigent.federation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.net.http.HttpTimeoutException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.davidbohl.dirigent.config.TlsBootstrapEnvironmentPostProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.mock.env.MockEnvironment;

class FederationTlsIntegrationTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void enforcesPinnedCertificateHostnameAndDefaultTrustDuringRealTlsHandshake() throws Exception {
        ConfigurableEnvironment environment = new MockEnvironment()
                .withProperty("dirigent.tls.storage-directory", temporaryDirectory.resolve("tls").toString())
                .withProperty("dirigent.tls.hostnames", "localhost");
        new TlsBootstrapEnvironmentPostProcessor().postProcessEnvironment(environment, new SpringApplication());
        String password = environment.getProperty("server.ssl.key-store-password");
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (var input = Path.of(URI.create(environment.getProperty("server.ssl.key-store"))).toUri().toURL().openStream()) {
            keyStore.load(input, password.toCharArray());
        }
        byte[] certificate = keyStore.getCertificate("dirigent").getEncoded();
        String validPin = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(certificate));

        KeyManagerFactory keyManagers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagers.init(keyStore, password.toCharArray());
        SSLContext serverContext = SSLContext.getInstance("TLS");
        serverContext.init(keyManagers.getKeyManagers(), null, null);

        HttpsServer server = HttpsServer.create(new InetSocketAddress(InetAddress.getByName("0.0.0.0"), 0), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(serverContext));
        server.createContext("/api/v1/system-information", exchange -> {
            byte[] body = "{\"instanceName\":\"Pinned\",\"gitUrl\":\"\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();

        AtomicInteger redirectTargetHits = new AtomicInteger();
        HttpsServer redirectServer = HttpsServer.create(
                new InetSocketAddress(InetAddress.getByName("0.0.0.0"), 0), 0);
        redirectServer.setHttpsConfigurator(new HttpsConfigurator(serverContext));
        redirectServer.createContext("/api/v1/system-information", exchange -> {
            exchange.getResponseHeaders().set("Location", "/redirect-target");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        redirectServer.createContext("/redirect-target", exchange -> {
            redirectTargetHits.incrementAndGet();
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        redirectServer.start();

        CountDownLatch releaseSlowResponse = new CountDownLatch(1);
        HttpsServer slowServer = HttpsServer.create(
                new InetSocketAddress(InetAddress.getByName("0.0.0.0"), 0), 0);
        slowServer.setHttpsConfigurator(new HttpsConfigurator(serverContext));
        slowServer.createContext("/api/v1/system-information", exchange -> {
            try {
                releaseSlowResponse.await(10, TimeUnit.SECONDS);
                byte[] body = "{}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                try (var output = exchange.getResponseBody()) {
                    output.write(body);
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                exchange.close();
            }
        });
        slowServer.start();
        try {
            String origin = "https://localhost:" + server.getAddress().getPort();
            FederationRemoteClient client = new FederationRemoteClient();
            FederationProperties.RemoteInstance pinned = remote(origin, validPin);
            assertEquals("Pinned", client.getSystemInformation(pinned).instanceName());

            FederationProperties.RemoteInstance wrongPin = remote(origin, "0".repeat(64));
            assertThrows(Exception.class, () -> client.getSystemInformation(wrongPin));

            FederationProperties.RemoteInstance wrongHostname = remote(
                    "https://127.0.0.2:" + server.getAddress().getPort(), validPin);
            assertThrows(Exception.class, () -> client.getSystemInformation(wrongHostname));

            FederationProperties.RemoteInstance unpinned = remote(origin, "");
            assertThrows(Exception.class, () -> client.getSystemInformation(unpinned));

            FederationProperties.RemoteInstance redirect = remote(
                    "https://localhost:" + redirectServer.getAddress().getPort(), validPin);
            assertThrows(Exception.class, () -> client.getSystemInformation(redirect));
            assertEquals(0, redirectTargetHits.get());

            FederationProperties.RemoteInstance slow = remote(
                    "https://localhost:" + slowServer.getAddress().getPort(), validPin);
            assertThrows(HttpTimeoutException.class, () -> client.getSystemInformation(slow));
        } finally {
            releaseSlowResponse.countDown();
            slowServer.stop(0);
            redirectServer.stop(0);
            server.stop(0);
        }
    }

    private FederationProperties.RemoteInstance remote(String baseUrl, String fingerprint) {
        FederationProperties.RemoteInstance instance = new FederationProperties.RemoteInstance();
        instance.setBaseUrl(baseUrl);
        instance.setToken("integration-test-token");
        instance.setCertificateSha256(fingerprint);
        return instance;
    }
}

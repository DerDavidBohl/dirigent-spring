package org.davidbohl.dirigent.federation;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.HttpMethod;

@SuppressWarnings("unchecked")
class FederationRemoteClientTest {

    @Test
    void createsComponentWithSpringWithoutAnObjectMapperBean() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(FederationRemoteClient.class);
            context.refresh();

            Assertions.assertNotNull(context.getBean(FederationRemoteClient.class));
        }
    }

    @Test
    void sendsBearerTokenToValidatedHttpsEndpoint() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("{\"instanceName\":\"Remote\",\"gitUrl\":\"\"}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        FederationProperties.RemoteInstance instance = new FederationProperties.RemoteInstance();
        instance.setBaseUrl("https://remote.example/prefix/");
        instance.setToken("secret-token");

        FederationRemoteClient client = new FederationRemoteClient(httpClient, new ObjectMapper());
        client.getSystemInformation(instance);

        var request = org.mockito.ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(request.capture(), any(HttpResponse.BodyHandler.class));
        assertEquals(URI.create("https://remote.example/prefix/api/v1/system-information"), request.getValue().uri());
        assertEquals("Bearer secret-token", request.getValue().headers().firstValue("Authorization").orElseThrow());
        assertEquals("GET", request.getValue().method());
    }

    @Test
    void rejectsMalformedResponsesAndRemoteHttpErrors() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> response = mock(HttpResponse.class);
        FederationProperties.RemoteInstance instance = new FederationProperties.RemoteInstance();
        instance.setBaseUrl("https://remote.example");
        instance.setToken("secret-token");
        FederationRemoteClient client = new FederationRemoteClient(httpClient, new ObjectMapper());

        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("not-json");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        assertThrows(IOException.class, () -> client.getSystemInformation(instance));

        when(response.statusCode()).thenReturn(503);
        when(response.body()).thenReturn("remote-error-detail");
        assertThrows(IOException.class, () -> client.getSystemInformation(instance));
    }

    @Test
    void rejectsNonHttpsAndCredentialBearingUrls() {
        assertThrows(IllegalArgumentException.class, () -> FederationRemoteClient.endpoint("http://remote.example", "/api"));
        assertThrows(IllegalArgumentException.class, () -> FederationRemoteClient.endpoint("https://user@remote.example", "/api"));
        assertThrows(IllegalArgumentException.class, () -> FederationRemoteClient.endpoint("https://remote.example?token=x", "/api"));
    }

    @Test
    void keepsHttpsHostnameVerificationEnabledForPinnedClients() {
        assertEquals("HTTPS", FederationRemoteClient.httpsSslParameters().getEndpointIdentificationAlgorithm());
        HttpClient client = FederationRemoteClient.newHttpClient(null);
        assertEquals(HttpClient.Redirect.NEVER, client.followRedirects());
        assertEquals(Duration.ofSeconds(3), client.connectTimeout().orElseThrow());
    }

    @Test
    void rejectsNonAllowlistedPathsBeforeSending() {
        FederationRemoteClient client = new FederationRemoteClient(mock(HttpClient.class), new ObjectMapper());
        FederationProperties.RemoteInstance instance = new FederationProperties.RemoteInstance();
        instance.setBaseUrl("https://remote.example");
        instance.setToken("secret-token");

        assertThrows(IllegalArgumentException.class,
                () -> client.forward(instance, HttpMethod.GET, "/api/v1/auth/tokens", null, null));
    }

        @Test
        void rejectsUnallowlistedMethodAndPathCombinations() {
        FederationRemoteClient client = new FederationRemoteClient(mock(HttpClient.class), new ObjectMapper());
        FederationProperties.RemoteInstance instance = new FederationProperties.RemoteInstance();
        instance.setBaseUrl("https://remote.example");
        instance.setToken("secret-token");

        assertThrows(IllegalArgumentException.class,
            () -> client.forward(instance, HttpMethod.GET, "/api/v1/secrets/db", null, null));
        assertThrows(IllegalArgumentException.class,
            () -> client.forward(instance, HttpMethod.DELETE, "/api/v1/deployments/all/start", null, null));
        assertThrows(IllegalArgumentException.class,
            () -> client.forward(instance, HttpMethod.POST, "/api/v1/secrets/key", null, null));
        assertThrows(IllegalArgumentException.class,
            () -> client.forward(instance, HttpMethod.PUT, "/api/v1/deployments/api/start", null, null));
        }

    @Test
    void allowsOnlyDocumentedDeploymentWritesWithFiniteRequestTimeout() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(202);
        when(response.body()).thenReturn("");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        FederationProperties.RemoteInstance instance = new FederationProperties.RemoteInstance();
        instance.setBaseUrl("https://remote.example");
        instance.setToken("write-token");

        FederationRemoteClient client = new FederationRemoteClient(httpClient, new ObjectMapper());
        client.forward(instance, HttpMethod.POST, "/api/v1/deployments/all/start", "forceRecreate=false", null);

        var request = org.mockito.ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(request.capture(), any(HttpResponse.BodyHandler.class));
        assertEquals(Duration.ofSeconds(5), request.getValue().timeout().orElseThrow());
        assertEquals(URI.create("https://remote.example/api/v1/deployments/all/start?forceRecreate=false"),
                request.getValue().uri());
        assertEquals(0, request.getValue().bodyPublisher().orElseThrow().contentLength());
    }

    @Test
    void discardsRemoteErrorBodyAndNeverChangesConfiguredOrigin() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(500);
        when(response.body()).thenReturn("sensitive-remote-error");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        FederationProperties.RemoteInstance instance = new FederationProperties.RemoteInstance();
        instance.setBaseUrl("https://remote.example/dirigent");
        instance.setToken("remote-token");

        FederationRemoteClient.RemoteResponse forwarded = new FederationRemoteClient(httpClient, new ObjectMapper())
                .forward(instance, HttpMethod.GET, "/api/v1/secrets", null, null);

        var request = org.mockito.ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(request.capture(), any(HttpResponse.BodyHandler.class));
        assertEquals("https", request.getValue().uri().getScheme());
        assertEquals("remote.example", request.getValue().uri().getHost());
        assertEquals("Bearer remote-token",
                request.getValue().headers().firstValue("Authorization").orElseThrow());
        assertEquals(500, forwarded.status());
        Assertions.assertEquals("", forwarded.body());
        Assertions.assertFalse(forwarded.body().contains("sensitive-remote-error"));
    }
}
package org.davidbohl.dirigent.federation;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.davidbohl.dirigent.deployments.management.model.DeploymentDto;
import org.davidbohl.dirigent.system.SystemInformation;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

@Component
public class FederationRemoteClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final Set<String> STATIC_READS = Set.of(
            "GET /api/v1/system-information", "GET /api/v1/deployments",
            "GET /api/v1/deployment-updates", "GET /api/v1/secrets");
    private static final Pattern DYNAMIC_OPERATION = Pattern.compile(
            "(?:POST /api/v1/deployments/(?:all/start|[^/]+/(?:start|stop))"
                    + "|POST /api/v1/deployment-updates/(?:check|run)"
                    + "|(?:PUT|DELETE) /api/v1/secrets/[^/]+)");

    private final HttpClient defaultHttpClient;
    private final ConcurrentMap<String, HttpClient> pinnedHttpClients = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public FederationRemoteClient() {
        this(newHttpClient(null), new ObjectMapper());
    }

    FederationRemoteClient(HttpClient httpClient, ObjectMapper objectMapper) {
        this.defaultHttpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public SystemInformation getSystemInformation(FederationProperties.RemoteInstance instance)
            throws IOException, InterruptedException {
        return get(instance, "/api/v1/system-information", SystemInformation.class);
    }

    public List<DeploymentDto> getDeployments(FederationProperties.RemoteInstance instance)
            throws IOException, InterruptedException {
        return get(instance, "/api/v1/deployments", new TypeReference<>() {});
    }

    public RemoteResponse forward(FederationProperties.RemoteInstance instance, HttpMethod method,
                                  String apiPath, String query, Object body)
            throws IOException, InterruptedException {
        String operation = method.name() + " " + apiPath;
        if (!STATIC_READS.contains(operation) && !DYNAMIC_OPERATION.matcher(operation).matches()) {
            throw new IllegalArgumentException("Federation operation is not allowed");
        }
        URI uri = endpoint(instance.getBaseUrl(), apiPath + (query == null || query.isBlank() ? "" : "?" + query));
        HttpRequest.Builder request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .header("Authorization", bearerToken(instance));
        if (body == null) {
            request.method(method.name(), HttpRequest.BodyPublishers.noBody());
        } else {
            request.header("Content-Type", "application/json")
                    .method(method.name(), HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
        }
        HttpResponse<String> response = httpClientFor(instance).send(request.build(), HttpResponse.BodyHandlers.ofString());
        return new RemoteResponse(response.statusCode(), response.statusCode() < 300 ? response.body() : "");
    }

    private <T> T get(FederationProperties.RemoteInstance instance, String apiPath, Class<T> responseType)
            throws IOException, InterruptedException {
        HttpResponse<String> response = send(instance, endpoint(instance.getBaseUrl(), apiPath));
        requireSuccessful(response);
        return objectMapper.readValue(response.body(), responseType);
    }

    private <T> T get(FederationProperties.RemoteInstance instance, String apiPath, TypeReference<T> responseType)
            throws IOException, InterruptedException {
        HttpResponse<String> response = send(instance, endpoint(instance.getBaseUrl(), apiPath));
        requireSuccessful(response);
        return objectMapper.readValue(response.body(), responseType);
    }

    private HttpResponse<String> send(FederationProperties.RemoteInstance instance, URI uri)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .header("Authorization", bearerToken(instance))
                .GET()
                .build();
        return httpClientFor(instance).send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpClient httpClientFor(FederationProperties.RemoteInstance instance) {
        String configuredPin = instance.getCertificateSha256();
        if (configuredPin == null || configuredPin.isBlank()) {
            return defaultHttpClient;
        }
        String fingerprint = PinnedCertificateTrustManager.normalizeFingerprint(configuredPin);
        return pinnedHttpClients.computeIfAbsent(fingerprint, this::newPinnedHttpClient);
    }

    private HttpClient newPinnedHttpClient(String fingerprint) {
        try {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new TrustManager[]{new PinnedCertificateTrustManager(fingerprint)}, null);
            return newHttpClient(context);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not configure federation TLS trust", exception);
        }
    }

    static HttpClient newHttpClient(SSLContext context) {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NEVER)
                .sslParameters(httpsSslParameters());
        if (context != null) {
            builder.sslContext(context);
        }
        return builder.build();
    }

    static SSLParameters httpsSslParameters() {
        SSLParameters parameters = new SSLParameters();
        parameters.setEndpointIdentificationAlgorithm("HTTPS");
        return parameters;
    }

    static URI endpoint(String baseUrl, String pathAndQuery) {
        URI base;
        try {
            base = URI.create(baseUrl);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid federation URL");
        }
        if (!base.isAbsolute() || !"https".equalsIgnoreCase(base.getScheme()) || base.getHost() == null
                || base.getUserInfo() != null || base.getQuery() != null || base.getFragment() != null) {
            throw new IllegalArgumentException("Federation URLs must be absolute HTTPS origins");
        }
        return URI.create(baseUrl.replaceAll("/+$", "") + pathAndQuery);
    }

    private static String bearerToken(FederationProperties.RemoteInstance instance) {
        return bearerToken(instance.getToken());
    }

    private static String bearerToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Federation token is required");
        }
        return "Bearer " + token;
    }

    private static void requireSuccessful(HttpResponse<String> response) throws IOException {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RemoteStatusException(response.statusCode());
        }
    }

    public static class RemoteStatusException extends IOException {
        private static final long serialVersionUID = 1L;
        private final int status;

        public RemoteStatusException(int status) {
            super("Federated instance returned an unsuccessful status");
            this.status = status;
        }

        public int getStatus() {
            return status;
        }
    }

    public record RemoteResponse(int status, String body) {
    }
}
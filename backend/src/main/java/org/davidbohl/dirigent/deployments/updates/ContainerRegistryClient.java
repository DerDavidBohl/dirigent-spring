package org.davidbohl.dirigent.deployments.updates;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.davidbohl.dirigent.deployments.updates.exception.CouldNotGetManifestDigestFromRegistryFailedException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class ContainerRegistryClient {

    private final RegistryAuthProperties registryAuthProperties;

    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public String getRegistryDigest(String registryEndpoint, String name, String tag) throws CouldNotGetManifestDigestFromRegistryFailedException {
        
        if(!registryEndpoint.startsWith("https://") && !registryEndpoint.startsWith("http://"))
            registryEndpoint = "https://" + registryEndpoint;

        if(!registryEndpoint.endsWith("/v2") && !registryEndpoint.endsWith("/v2/"))
            registryEndpoint = registryEndpoint + "/v2";

        Optional<RegistryAuthProperties.Registry> credentials = registryAuthProperties
            .findByHost(URI.create(registryEndpoint).getAuthority());

        String authorizationHeader = getAuthorizationHeader(registryEndpoint, name, credentials);
        try {
            return getManifestDigest(registryEndpoint, name, tag, authorizationHeader);
        } catch (Throwable e) {
            log.warn("Could not Get Manifest Digest from Registry: {}:{}", name, tag);
            throw new CouldNotGetManifestDigestFromRegistryFailedException(e);
        }
    }

    private String getAuthorizationHeader(String registryEndpoint, String name, Optional<RegistryAuthProperties.Registry> credentials) {

        try {

            HttpHeaders h = new HttpHeaders();
            credentials.ifPresent(c -> h.setBasicAuth(c.getUsername(), c.getPassword()));
            rest.exchange(registryEndpoint + "/", HttpMethod.GET, new HttpEntity<>(h), String.class);

        } catch (HttpClientErrorException rce) {

            if (rce.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                String authHeader = rce.getResponseHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE);

                if (authHeader != null && authHeader.toLowerCase().startsWith("basic"))
                    return credentials.map(this::basicAuthHeader).orElse(null);

                return authenticate(authHeader, name, credentials);
            }
        }

        return credentials.map(this::basicAuthHeader).orElse(null); // no bearer challenge, fall back to basic auth if configured
    }

    private String basicAuthHeader(RegistryAuthProperties.Registry credentials) {
        String raw = credentials.getUsername() + ":" + credentials.getPassword();
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private String authenticate(String header, String name, Optional<RegistryAuthProperties.Registry> credentials) {
        String realm = extractValueFromRegistryHeader(header, "realm");
        String service = extractValueFromRegistryHeader(header, "service");
        String scope = "repository:" + name + ":pull";

        String url = "%s?service=%s&scope=%s".formatted(realm, service, scope);
        HttpHeaders h = new HttpHeaders();
        credentials.ifPresent(c -> h.setBasicAuth(c.getUsername(), c.getPassword()));
        HttpEntity<Void> req = new HttpEntity<>(h);
        TokenResponse token = rest.exchange(url, HttpMethod.GET, req, TokenResponse.class).getBody();
        return "Bearer " + token.token();
    }

    private String getManifestDigest(String registryEndpoint, String name, String tag, String authorizationHeader)
            throws JsonMappingException, JsonProcessingException {
        HttpHeaders h = new HttpHeaders();
        if (authorizationHeader != null)
            h.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
        h.setAccept(MediaType.parseMediaTypes(List.of(
                "application/vnd.docker.distribution.manifest.v2+json",
                "application/vnd.oci.image.manifest.v1+json",
                "application/vnd.oci.image.index.v1+json"
            )));

        HttpEntity<Void> req = new HttpEntity<>(h);

        String uri =registryEndpoint + "/" + name + "/manifests/" + tag;

        String body = rest.exchange(
                uri,
                HttpMethod.GET,
                req,
                String.class)
                .getBody();

        JsonNode root = mapper.readTree(body);

        if (root.has("manifests")) { // manifest list – pick first
            Iterator<JsonNode> elements = root.get("manifests").elements();

            String arch = System.getProperty("os.arch").toLowerCase();
            String os = System.getProperty("os.name").toLowerCase();

            while (elements.hasNext()) {
                JsonNode dataset = elements.next();

                String archInManifest = dataset.get("platform").get("architecture").asText();
                String osInManifest = dataset.get("platform").get("os").asText();

                if (arch.equals(archInManifest) && os.equals(osInManifest)) {
                    return getManifestDigest(registryEndpoint, name, dataset.get("digest").asText(), authorizationHeader);
                }

            }

            throw new RuntimeException("Manifest for the following arch/os not found: " + arch + "/" + os);
        }

        return root.get("config").get("digest").asText();
    }

    private record TokenResponse(String token) {
    }

    private static String extractValueFromRegistryHeader(String header, String key) {

        Map<String, String> map = new HashMap<>();
        // Regex to match key="value" patterns (handles whitespace and quotes)
        Pattern pattern = Pattern.compile("(\\w+)=\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(header);

        while (matcher.find()) {
            String headerKey = matcher.group(1);
            String value = matcher.group(2);
            map.put(headerKey, value);
        }

        return map.get(key);

    }
}

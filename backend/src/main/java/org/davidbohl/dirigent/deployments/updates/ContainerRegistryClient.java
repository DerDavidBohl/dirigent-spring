package org.davidbohl.dirigent.deployments.updates;

import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ContainerRegistryClient {

    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public String getRegistryDigest(String host, String name, String tag) throws CouldNotGetManifestDigestFromRegistryFailedException {
        String token = getToken(host, name);
        try {
            return getManifestDigest(host, name, tag, token);
        } catch (Throwable e) {
            log.warn("Could not Get Manifest Digest from Registry: {name}: {tag}", name, tag);
            throw new CouldNotGetManifestDigestFromRegistryFailedException(e);
        }
    }

    private String getToken(String host, String name) {

        try {

            rest.exchange(
                    "https://{host}/v2/", HttpMethod.GET, null, String.class, host);

        } catch (HttpClientErrorException rce) {

            if (rce.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                String authHeader = rce.getResponseHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE);
                return authenticate(host, authHeader, name);
            }
        }

        return ""; // no auth required
    }

    private String authenticate(String host, String header, String name) {
        String realm = extractValueFromRegistryHeader(header, "realm");
        String service = extractValueFromRegistryHeader(header, "service");
        String scope = "repository:" + name + ":pull";

        String url = "%s?service=%s&scope=%s".formatted(realm, service, scope);
        HttpHeaders h = new HttpHeaders();
        HttpEntity<Void> req = new HttpEntity<>(h);
        TokenResponse token = rest.exchange(url, HttpMethod.GET, req, TokenResponse.class).getBody();
        return token.token();
    }

    private String getManifestDigest(String host, String name, String tag, String token)
            throws JsonMappingException, JsonProcessingException {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setAccept(MediaType.parseMediaTypes(List.of(
                "application/vnd.docker.distribution.manifest.v2+json")
            ));

        HttpEntity<Void> req = new HttpEntity<>(h);

        URI uri = URI.create(host + name + "/manifests/" + tag);

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
                    return getManifestDigest(host, name, dataset.get("digest").asText(), token);
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

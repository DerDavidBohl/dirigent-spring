package org.davidbohl.dirigent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

class SpaWebConfigurationTest {

    private final SpaWebConfiguration.SpaFallbackResourceResolver resolver =
            new SpaWebConfiguration.SpaFallbackResourceResolver();
    private final Resource location = new ClassPathResource("static/");

    @Test
    void servesIndexForClientSideRoutes() throws IOException {
        Resource resource = resolver.getResource("deployments", location);
        assertEquals("index.html", resource.getFilename());
        resource = resolver.getResource("settings/tokens", location);
        assertEquals("index.html", resource.getFilename());
    }

    @Test
    void doesNotServeIndexForApiOrMissingFiles() throws IOException {
        assertNull(resolver.getResource("api/v1/unknown", location));
        assertNull(resolver.getResource("missing.js", location));
    }
}

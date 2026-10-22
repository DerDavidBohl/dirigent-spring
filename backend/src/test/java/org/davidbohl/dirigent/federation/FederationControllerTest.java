package org.davidbohl.dirigent.federation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.slf4j.LoggerFactory;

class FederationControllerTest {

    @Test
    void remoteAuthorizationFailureDoesNotBecomeHubUnauthorizedResponse() throws Exception {
        FederationService service = mock(FederationService.class);
        when(service.forward("office", HttpMethod.GET, "/api/v1/secrets", null, null))
                .thenReturn(new FederationRemoteClient.RemoteResponse(HttpStatus.UNAUTHORIZED.value(), ""));

        var response = new FederationController(service).getSecrets("office");

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertEquals("{\"error\":\"Remote instance authorization failed\"}", response.getBody());
    }

    @Test
    void secretWriteAuditAndFailureResponseDoNotExposeSecretOrToken() throws Exception {
        String secretValue = "sensitive-secret-value";
        String token = "sensitive-machine-token";
        FederationService service = mock(FederationService.class);
        when(service.forward("office", HttpMethod.PUT, "/api/v1/secrets/database-password",
                "restartDeployments=false", java.util.Map.of("value", secretValue)))
            .thenReturn(new FederationRemoteClient.RemoteResponse(HttpStatus.BAD_GATEWAY.value(), ""));
        Logger logger = (Logger) LoggerFactory.getLogger(FederationController.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("operator", token));
        try {
            FederationController controller = new FederationController(service);

            var response = controller.saveSecret("office", "database-password", false,
                    java.util.Map.of("value", secretValue));

            assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
            assertFalse(String.valueOf(response.getBody()).contains(secretValue));
            assertFalse(String.valueOf(response.getBody()).contains(token));
            String auditEvents = appender.list.stream().map(event -> String.valueOf(event.getFormattedMessage()))
                    .reduce("", (left, right) -> left + "\n" + right);
            assertFalse(auditEvents.contains(secretValue));
            assertFalse(auditEvents.contains(token));
            assertEquals(1, appender.list.size());
        } finally {
            SecurityContextHolder.clearContext();
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
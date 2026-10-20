package org.davidbohl.dirigent.federation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;

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
}
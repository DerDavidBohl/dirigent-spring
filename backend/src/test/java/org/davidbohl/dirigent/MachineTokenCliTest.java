package org.davidbohl.dirigent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Set;

import org.davidbohl.dirigent.auth.MachineTokenService;
import org.davidbohl.dirigent.auth.dto.MachineTokenCreateRequest;
import org.davidbohl.dirigent.auth.dto.MachineTokenCreatedResponse;
import org.junit.jupiter.api.Test;

class MachineTokenCliTest {

    @Test
    void parsesExplicitPermissionsAndExpiry() {
        MachineTokenCreateRequest request = DirigentApplication.parseMachineTokenRequest(new String[] {
                "--create-machine-token", "Federation hub", "--token-permissions",
                " deployments:read, secrets:read ", "--token-expires-at", "2027-01-01T00:00:00Z"
        }, 0);

        assertEquals("Federation hub", request.name());
        assertEquals(Set.of("deployments:read", "secrets:read"), request.permissions());
        assertEquals(Instant.parse("2027-01-01T00:00:00Z"), request.expiresAt());
    }

    @Test
    void rejectsMissingArgumentsAndInvalidExpiry() {
        assertThrows(IllegalArgumentException.class, () -> DirigentApplication.parseMachineTokenRequest(
                new String[] {"--create-machine-token", "federation"}, 0));
        assertThrows(IllegalArgumentException.class, () -> DirigentApplication.parseMachineTokenRequest(
                new String[] {"--create-machine-token", "federation", "--token-permissions", "system:read",
                        "--token-expires-at", "tomorrow"}, 0));
    }

    @Test
    void delegatesIssuanceAndPrintsTheTokenOnce() {
        MachineTokenService tokenService = org.mockito.Mockito.mock(MachineTokenService.class);
        MachineTokenCreateRequest request = new MachineTokenCreateRequest("Federation hub",
                Set.of("system:read"), Instant.parse("2027-01-01T00:00:00Z"));
        MachineTokenCreatedResponse created = new MachineTokenCreatedResponse(1L, "Federation hub", "dgt_once",
                Set.of("system:read"), Instant.parse("2026-10-22T00:00:00Z"), request.expiresAt());
        when(tokenService.create(request, "server-cli")).thenReturn(created);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        DirigentApplication.createMachineToken(request, tokenService,
                new PrintStream(bytes, true, StandardCharsets.UTF_8));

        String output = bytes.toString(StandardCharsets.UTF_8);
        assertEquals(1, output.split("dgt_once", -1).length - 1);
        assertEquals(true, output.contains("Token: dgt_once"));
        verify(tokenService).create(request, "server-cli");
    }
}
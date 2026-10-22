package org.davidbohl.dirigent.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.davidbohl.dirigent.auth.dto.MachineTokenCreateRequest;
import org.davidbohl.dirigent.auth.entity.MachineTokenEntity;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class MachineTokenServiceTest {

    private final MachineTokenRepository repository = Mockito.mock(MachineTokenRepository.class);
    private final MachineTokenService service = new MachineTokenService(repository);

    @Test
    void createsOpaqueTokenAndPersistsOnlyItsVerifier() {
        when(repository.save(any(MachineTokenEntity.class)))
                                .thenAnswer(invocation -> {
                                        MachineTokenEntity entity = invocation.getArgument(0);
                                        ReflectionTestUtils.setField(entity, "id", 3L);
                                        return entity;
                                });

        var created = service.create(new MachineTokenCreateRequest(
                "CI deploy", Set.of("deployments:read", "deployments:write"), futureExpiry()), "operator");

        ArgumentCaptor<MachineTokenEntity> entityCaptor = ArgumentCaptor.forClass(MachineTokenEntity.class);
        Mockito.verify(repository).save(entityCaptor.capture());
        MachineTokenEntity stored = entityCaptor.getValue();
        assertTrue(created.token().startsWith("dgt_"));
        assertNotEquals(created.token(), stored.getTokenHash());
        assertEquals(Set.of("deployments:read", "deployments:write"), created.permissions());
    }

    @Test
    void authenticatesAnUnexpiredTokenUsingItsVerifier() {
        when(repository.save(any(MachineTokenEntity.class)))
                                .thenAnswer(invocation -> {
                                        MachineTokenEntity entity = invocation.getArgument(0);
                                        ReflectionTestUtils.setField(entity, "id", 4L);
                                        return entity;
                                });
        var created = service.create(new MachineTokenCreateRequest(
                "Read-only", Set.of("deployments:read"), futureExpiry()), "operator");
        ArgumentCaptor<MachineTokenEntity> entityCaptor = ArgumentCaptor.forClass(MachineTokenEntity.class);
        Mockito.verify(repository).save(entityCaptor.capture());
        MachineTokenEntity stored = entityCaptor.getValue();
        when(repository.findByTokenHashAndRevokedAtIsNull(stored.getTokenHash())).thenReturn(Optional.of(stored));

        var identity = service.authenticate(created.token()).orElseThrow();

        assertEquals("operator", identity.username());
        assertEquals(Set.of("deployments:read"), identity.permissions());
    }

    @Test
    void rejectsUnsupportedOrEmptyPermissions() {
        assertThrows(IllegalArgumentException.class, () -> service.create(
                new MachineTokenCreateRequest("Invalid", Set.of("admin"), null), "operator"));
        assertThrows(IllegalArgumentException.class, () -> service.create(
                new MachineTokenCreateRequest("Empty", Set.of(), null), "operator"));
        assertThrows(IllegalArgumentException.class, () -> service.create(
                new MachineTokenCreateRequest("No expiry", Set.of("deployments:read"), null), "operator"));
    }

    @Test
    void allowsDedicatedWebhookTriggerPermission() {
        when(repository.save(any(MachineTokenEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var created = service.create(new MachineTokenCreateRequest(
                "Gitea webhook", Set.of("webhooks:trigger"), futureExpiry()), "operator");

        assertEquals(Set.of("webhooks:trigger"), created.permissions());
    }

    @Test
    void rejectsExpiredTokensAndExpiryBeyondOneYear() throws Exception {
        String expiredToken = "dgt_expired-test-token";
        String verifier = java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(expiredToken.getBytes(StandardCharsets.UTF_8)));
        MachineTokenEntity expired = new MachineTokenEntity("expired", verifier, "deployments:read",
                "operator", Instant.now().minusSeconds(3600), Instant.now().minusSeconds(1));
        when(repository.findByTokenHashAndRevokedAtIsNull(verifier)).thenReturn(Optional.of(expired));

        assertTrue(service.authenticate(expiredToken).isEmpty());
        String nonExpiringToken = "dgt_legacy-token";
        String nonExpiringVerifier = java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(nonExpiringToken.getBytes(StandardCharsets.UTF_8)));
        MachineTokenEntity legacy = new MachineTokenEntity("legacy", nonExpiringVerifier, "deployments:read",
                "operator", Instant.now().minusSeconds(3600), null);
        when(repository.findByTokenHashAndRevokedAtIsNull(nonExpiringVerifier)).thenReturn(Optional.of(legacy));
        assertTrue(service.authenticate(nonExpiringToken).isEmpty());

        assertThrows(IllegalArgumentException.class, () -> service.create(
                new MachineTokenCreateRequest("Too long", Set.of("deployments:read"),
                        Instant.now().plusSeconds(367L * 24 * 60 * 60)), "operator"));
    }

    @Test
    void revokesTokenWithoutDeletingAuditMetadata() throws Exception {
        String token = "dgt-token-to-revoke";
        String tokenHash = java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8)));
        MachineTokenEntity entity = new MachineTokenEntity("deploy", tokenHash, "deployments:read",
                "operator", java.time.Instant.now(), futureExpiry());
        when(repository.findById(7L)).thenReturn(Optional.of(entity));
        when(repository.findByTokenHashAndRevokedAtIsNull(tokenHash)).thenReturn(Optional.empty());

        service.revoke(7L);

        assertFalse(entity.getRevokedAt() == null);
        assertTrue(service.authenticate(token).isEmpty());
        Mockito.verify(repository, Mockito.never()).delete(any(MachineTokenEntity.class));
    }

    @Test
    void refreshesLastUseOnceItIsOlderThanAMinute() {
        MachineTokenEntity stored = new MachineTokenEntity("ci", "hash", "deployments:read",
                "operator", Instant.now(), futureExpiry());
        ReflectionTestUtils.setField(stored, "id", 6L);
        Instant stale = Instant.now().minusSeconds(120);
        stored.markUsed(stale);
        when(repository.findByTokenHashAndRevokedAtIsNull(any())).thenReturn(Optional.of(stored));

        service.authenticate("dgt_x");

        assertTrue(stored.getLastUsedAt().isAfter(stale));
    }

    @Test
    void doesNotRecordUseForExpiredTokens() {
        MachineTokenEntity expired = new MachineTokenEntity("old", "hash", "deployments:read",
                "operator", Instant.now().minusSeconds(3600), Instant.now().minusSeconds(1));
        when(repository.findByTokenHashAndRevokedAtIsNull(any())).thenReturn(Optional.of(expired));

        assertTrue(service.authenticate("dgt_x").isEmpty());

        assertEquals(null, expired.getLastUsedAt());
    }

    @Test
    void listExposesLastUse() {
        MachineTokenEntity stored = new MachineTokenEntity("ci", "hash", "deployments:read",
                "operator", Instant.now(), futureExpiry());
        Instant used = Instant.now().minusSeconds(30);
        stored.markUsed(used);
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(java.util.List.of(stored));

        assertEquals(used, service.list().get(0).lastUsedAt());
    }

    @Test
    void findsExpiredUnrevokedTokensThroughTheRepository() {
        MachineTokenEntity expired = new MachineTokenEntity("old", "hash", "deployments:read",
                "operator", Instant.now().minusSeconds(3600), Instant.now().minusSeconds(1));
        when(repository.findAllByRevokedAtIsNullAndExpiresAtBeforeOrderByExpiresAtAsc(any()))
                .thenReturn(java.util.List.of(expired));

        assertEquals(java.util.List.of(expired), service.findExpiredUnrevoked());
    }

    @Test
    void recordsLastUseAtMostOncePerMinute() {
        MachineTokenEntity stored = new MachineTokenEntity("ci", "hash", "deployments:read",
                "operator", Instant.now(), futureExpiry());
        ReflectionTestUtils.setField(stored, "id", 5L);
        when(repository.findByTokenHashAndRevokedAtIsNull(any())).thenReturn(Optional.of(stored));

        service.authenticate("dgt_x");
        Instant first = stored.getLastUsedAt();
        service.authenticate("dgt_x");

        assertTrue(first != null);
        assertEquals(first, stored.getLastUsedAt());
    }

        private Instant futureExpiry() {
                return Instant.now().plus(30, ChronoUnit.DAYS);
        }
}
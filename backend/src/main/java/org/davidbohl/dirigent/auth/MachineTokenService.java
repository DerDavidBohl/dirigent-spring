package org.davidbohl.dirigent.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Set;
import java.util.stream.Collectors;

import org.davidbohl.dirigent.auth.dto.MachineTokenCreateRequest;
import org.davidbohl.dirigent.auth.dto.MachineTokenCreatedResponse;
import org.davidbohl.dirigent.auth.dto.MachineTokenSummary;
import org.davidbohl.dirigent.auth.entity.MachineTokenEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MachineTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final long MAX_TOKEN_LIFETIME_DAYS = 366;
    private static final java.time.Duration LAST_USED_RESOLUTION = java.time.Duration.ofMinutes(1);

    private final MachineTokenRepository tokenRepository;

    public MachineTokenService(MachineTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    @Transactional
    public MachineTokenCreatedResponse create(MachineTokenCreateRequest request, String createdBy) {
        if (request == null || request.name() == null || request.name().isBlank() || request.name().length() > 96) {
            throw new IllegalArgumentException("Token name must contain between 1 and 96 characters");
        }
        Set<String> requestedPermissions = request.permissions() == null ? Set.of() : request.permissions();
        if (requestedPermissions.isEmpty() || !MachineTokenPermission.valuesAsSet().containsAll(requestedPermissions)) {
            throw new IllegalArgumentException("Select one or more supported token permissions");
        }

        Instant now = Instant.now();
        Instant expiresAt = request.expiresAt();
        if (expiresAt == null || !expiresAt.isAfter(now)
            || expiresAt.isAfter(now.plus(MAX_TOKEN_LIFETIME_DAYS, ChronoUnit.DAYS))) {
            throw new IllegalArgumentException("Token expiry must be in the future and within 366 days");
        }

        String token = generateToken();
        String permissions = requestedPermissions.stream().sorted().collect(Collectors.joining(" "));
        MachineTokenEntity entity = tokenRepository.save(new MachineTokenEntity(
                request.name().trim(), hashToken(token), permissions, createdBy, now, expiresAt));

        return new MachineTokenCreatedResponse(entity.getId(), entity.getName(), token,
                requestedPermissions, entity.getCreatedAt(), entity.getExpiresAt());
    }

    @Transactional(readOnly = true)
    public java.util.List<MachineTokenSummary> list() {
        return tokenRepository.findAllByOrderByCreatedAtDesc().stream().map(this::summary).toList();
    }

    @Transactional
    public void revoke(long id) {
        MachineTokenEntity token = tokenRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Token not found"));
        if (token.getRevokedAt() == null) {
            token.revoke(Instant.now());
        }
    }

    @Transactional(readOnly = true)
    public java.util.List<MachineTokenEntity> findExpiredUnrevoked() {
        return tokenRepository.findAllByRevokedAtIsNullAndExpiresAtBeforeOrderByExpiresAtAsc(Instant.now());
    }

    @Transactional
    public java.util.Optional<MachineTokenIdentity> authenticate(String token) {
        if (token == null || token.isBlank()) {
            return java.util.Optional.empty();
        }
        return tokenRepository.findByTokenHashAndRevokedAtIsNull(hashToken(token))
            .filter(entity -> entity.getExpiresAt() != null && entity.getExpiresAt().isAfter(Instant.now()))
                .map(entity -> {
                    Instant now = Instant.now();
                    if (entity.getLastUsedAt() == null
                            || entity.getLastUsedAt().isBefore(now.minus(LAST_USED_RESOLUTION))) {
                        entity.markUsed(now);
                    }
                    return new MachineTokenIdentity(entity.getId(), entity.getCreatedBy(),
                            Set.of(entity.getPermissions().split(" ")));
                });
    }

    private MachineTokenSummary summary(MachineTokenEntity entity) {
        Set<String> permissions = Set.of(entity.getPermissions().split(" "));
        return new MachineTokenSummary(entity.getId(), entity.getName(), permissions, entity.getCreatedBy(),
                entity.getCreatedAt(), entity.getExpiresAt(), entity.getRevokedAt(), entity.getLastUsedAt());
    }

    private String generateToken() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return "dgt_" + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public record MachineTokenIdentity(long id, String username, Set<String> permissions) {
    }
}
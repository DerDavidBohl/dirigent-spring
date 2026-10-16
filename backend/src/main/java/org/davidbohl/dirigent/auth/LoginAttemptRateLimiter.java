package org.davidbohl.dirigent.auth;

import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

@Component
public class LoginAttemptRateLimiter {

    private static final int MAX_FAILURES_PER_CLIENT = 50;
    private static final int MAX_FAILURES_PER_USERNAME = 10;
    private static final long LOCKOUT_SECONDS = Duration.ofMinutes(15).toSeconds();

    private final Cache<String, AtomicInteger> failuresByClient = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(15))
            .maximumSize(10_000)
            .build();
        private final Cache<String, AtomicInteger> failuresByClientAndUsername = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(15))
            .maximumSize(10_000)
            .build();

    public boolean isBlocked(String clientAddress, String username) {
        String clientKey = clientKey(clientAddress);
        AtomicInteger clientFailures = failuresByClient.getIfPresent(clientKey);
        AtomicInteger scopedFailures = failuresByClientAndUsername.getIfPresent(scopedKey(clientKey, username));
        return (clientFailures != null && clientFailures.get() >= MAX_FAILURES_PER_CLIENT)
            || (scopedFailures != null && scopedFailures.get() >= MAX_FAILURES_PER_USERNAME);
    }

    public void recordFailure(String clientAddress, String username) {
        String clientKey = clientKey(clientAddress);
        failuresByClient.get(clientKey, ignored -> new AtomicInteger()).incrementAndGet();
        failuresByClientAndUsername.get(scopedKey(clientKey, username), ignored -> new AtomicInteger())
            .incrementAndGet();
    }

    public void recordSuccess(String clientAddress, String username) {
        String clientKey = clientKey(clientAddress);
        failuresByClient.invalidate(clientKey);
        failuresByClientAndUsername.invalidate(scopedKey(clientKey, username));
    }

    public long retryAfterSeconds() {
        return LOCKOUT_SECONDS;
    }

    private String clientKey(String clientAddress) {
        return clientAddress == null || clientAddress.isBlank() ? "unknown" : clientAddress;
    }

    private String usernameKey(String username) {
        return username == null || username.isBlank() ? "unknown" : username.trim().toLowerCase(Locale.ROOT);
    }

    private String scopedKey(String clientKey, String username) {
        return clientKey + "\u0000" + usernameKey(username);
    }
}
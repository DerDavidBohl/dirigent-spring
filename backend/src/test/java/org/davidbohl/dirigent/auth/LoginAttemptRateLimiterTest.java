package org.davidbohl.dirigent.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LoginAttemptRateLimiterTest {

    private final LoginAttemptRateLimiter rateLimiter = new LoginAttemptRateLimiter();

    @Test
    void blocksAfterTenFailuresForTheSameNormalizedUsername() {
        for (int attempt = 0; attempt < 10; attempt++) {
            rateLimiter.recordFailure("192.0.2.10", "Operator");
        }

        assertTrue(rateLimiter.isBlocked("192.0.2.10", "operator"));
        assertFalse(rateLimiter.isBlocked("192.0.2.11", "operator"));
        assertFalse(rateLimiter.isBlocked("192.0.2.11", "another-user"));
    }

    @Test
    void blocksAfterFiftyFailuresFromOneClient() {
        for (int attempt = 0; attempt < 50; attempt++) {
            rateLimiter.recordFailure("192.0.2.10", "user-" + attempt);
        }

        assertTrue(rateLimiter.isBlocked("192.0.2.10", "another-user"));
        assertFalse(rateLimiter.isBlocked("192.0.2.11", "another-user"));
    }

    @Test
    void successfulLoginClearsThatClientAndAccountCounters() {
        for (int attempt = 0; attempt < 10; attempt++) {
            rateLimiter.recordFailure("192.0.2.10", "operator");
        }
        assertTrue(rateLimiter.isBlocked("192.0.2.10", "operator"));

        rateLimiter.recordSuccess("192.0.2.10", "operator");

        assertFalse(rateLimiter.isBlocked("192.0.2.10", "operator"));
    }
}
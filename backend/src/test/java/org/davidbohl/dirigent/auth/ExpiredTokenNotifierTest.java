package org.davidbohl.dirigent.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.davidbohl.dirigent.auth.entity.MachineTokenEntity;
import org.davidbohl.dirigent.deployments.notification.NotificationService;
import org.junit.jupiter.api.Test;

class ExpiredTokenNotifierTest {

    private final MachineTokenService tokenService = mock(MachineTokenService.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final ExpiredTokenNotifier notifier = new ExpiredTokenNotifier(tokenService, notificationService);

    @Test
    void sendsNoticeNamingExpiredTokens() {
        var expired = new MachineTokenEntity("ci", "hash", "deployments:read", "op",
                Instant.now().minusSeconds(7200), Instant.now().minusSeconds(60));
        when(tokenService.findExpiredUnrevoked()).thenReturn(List.of(expired));

        notifier.notifyExpiredTokens();

        verify(notificationService).sendNotice(any(), contains("ci"));
    }

    @Test
    void runsHourly() throws Exception {
        var scheduled = ExpiredTokenNotifier.class.getMethod("notifyExpiredTokens")
                .getAnnotation(org.springframework.scheduling.annotation.Scheduled.class);

        org.junit.jupiter.api.Assertions.assertEquals("0 0 * * * *", scheduled.cron());
    }

    @Test
    void staysSilentWithoutExpiredTokens() {
        when(tokenService.findExpiredUnrevoked()).thenReturn(List.of());

        notifier.notifyExpiredTokens();

        verify(notificationService, never()).sendNotice(any(), any());
    }
}

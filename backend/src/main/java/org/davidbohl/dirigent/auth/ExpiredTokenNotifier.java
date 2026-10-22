package org.davidbohl.dirigent.auth;

import java.util.stream.Collectors;

import org.davidbohl.dirigent.deployments.notification.NotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExpiredTokenNotifier {

    private final MachineTokenService tokenService;
    private final NotificationService notificationService;

    public ExpiredTokenNotifier(MachineTokenService tokenService, NotificationService notificationService) {
        this.tokenService = tokenService;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void notifyExpiredTokens() {
        var expired = tokenService.findExpiredUnrevoked();
        if (expired.isEmpty()) {
            return;
        }
        String message = expired.stream()
                .map(token -> "%s (expired %s)".formatted(token.getName(), token.getExpiresAt()))
                .collect(Collectors.joining("\n"));
        notificationService.sendNotice("Machine tokens expired", message);
    }
}

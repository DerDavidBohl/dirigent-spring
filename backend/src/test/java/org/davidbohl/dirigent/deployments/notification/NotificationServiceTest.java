package org.davidbohl.dirigent.deployments.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class NotificationServiceTest {

    @Test
    void prefixesTitleWithInstanceName() {
        NotificationService service = new NotificationService();
        ReflectionTestUtils.setField(service, "instanceName", "Prod");

        assertEquals("[Prod] Image updated", service.withInstanceName("Image updated"));
    }
}

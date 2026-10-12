package org.davidbohl.dirigent.deployments.management;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.SystemEnvironmentPropertySource;

class DeploymentSchedulerConfigurationTest {

    @Test
    void bindsDocumentedScheduleEnvironmentVariables() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new SystemEnvironmentPropertySource(
                "test-environment", Map.of(
                        "DIRIGENT_DEPLOYMENTS_SCHEDULE_ENABLED", "false",
                        "DIRIGENT_DEPLOYMENTS_SCHEDULE_CRON", "0 */5 * * * *",
                        "DIRIGENT_START_ALL_ON_STARTUP", "false")));
        context.register(DeploymentScheduler.class);

        assertDoesNotThrow(context::refresh);
        context.close();
    }
}

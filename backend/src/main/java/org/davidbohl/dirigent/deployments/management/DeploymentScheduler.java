package org.davidbohl.dirigent.deployments.management;

import org.davidbohl.dirigent.deployments.management.event.RecreateAllDeploymentStatesEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class DeploymentScheduler {

    private final Logger logger = LoggerFactory.getLogger(DeploymentScheduler.class);

    private final ApplicationEventPublisher applicationEventPublisher;

    @Value("${dirigent.delpoyments.schedule.enabled}")
    boolean enabled;

    @Value("${dirigent.start.all.on.startup}")
    private boolean startAllDeploymentsOnStartup;

    public DeploymentScheduler(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Scheduled(cron = "${dirigent.delpoyments.schedule.cron}")
    void runScheduledDeployments() {
        if(enabled) {
            logger.info("Starting all deployments scheduled");
            this.applicationEventPublisher.publishEvent(new RecreateAllDeploymentStatesEvent(this));
        }
    }

    @EventListener(ContextRefreshedEvent.class)
    public void onContextRefreshed() {
        if(startAllDeploymentsOnStartup)
            applicationEventPublisher.publishEvent(new RecreateAllDeploymentStatesEvent(this));
    }

}

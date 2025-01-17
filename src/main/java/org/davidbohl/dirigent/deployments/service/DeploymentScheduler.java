package org.davidbohl.dirigent.deployments.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class DeploymentScheduler {

    private final Logger logger = LoggerFactory.getLogger(DeploymentScheduler.class);

    @Value("${dirigent.delpoyments.schedule.enabled}")
    boolean enabled;

    private final DeploymentsService deploymentsService;

    public DeploymentScheduler(@Autowired DeploymentsService deploymentsService) {
        this.deploymentsService = deploymentsService;
    }

    @Scheduled(cron = "${dirigent.delpoyments.schedule.cron}")
    void runScheduledDeployments() {
        if(enabled) {
            logger.info("Starting all deployments scheduled");
            deploymentsService.startAllDeployments();
        }
    }

}

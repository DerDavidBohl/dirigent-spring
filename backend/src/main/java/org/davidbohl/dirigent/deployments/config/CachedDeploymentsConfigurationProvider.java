package org.davidbohl.dirigent.deployments.config;

import org.davidbohl.dirigent.deployments.models.DeploynentConfiguration;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class CachedDeploymentsConfigurationProvider {

    private final DeploymentsConfigurationProvider deploymentsConfigurationProvider;

    public CachedDeploymentsConfigurationProvider(DeploymentsConfigurationProvider deploymentsConfigurationProvider) {
        this.deploymentsConfigurationProvider = deploymentsConfigurationProvider;
    }


    @Cacheable("deploymentsConfiguration")
    @Scheduled(fixedDelay = 5000)
    public DeploynentConfiguration getCachedConfiguration() {
        return this.deploymentsConfigurationProvider.getConfiguration();
    }

}

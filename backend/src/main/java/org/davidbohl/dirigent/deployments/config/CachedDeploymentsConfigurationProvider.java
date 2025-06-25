package org.davidbohl.dirigent.deployments.config;

import lombok.extern.slf4j.Slf4j;
import org.davidbohl.dirigent.deployments.models.DeploynentConfiguration;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CachedDeploymentsConfigurationProvider {

    private final DeploymentsConfigurationProvider deploymentsConfigurationProvider;

    public CachedDeploymentsConfigurationProvider(DeploymentsConfigurationProvider deploymentsConfigurationProvider) {
        this.deploymentsConfigurationProvider = deploymentsConfigurationProvider;
    }


    @Cacheable("deploymentsConfiguration")
    public DeploynentConfiguration getCachedConfiguration() {
        return this.deploymentsConfigurationProvider.getConfiguration();
    }

    @CacheEvict(value = "deploymentsConfiguration", allEntries = true)
    @Scheduled(fixedDelay = 60000)
    public void evictCachedConfiguration() {
        log.info("Evicting cached deployments configuration");
    }


}

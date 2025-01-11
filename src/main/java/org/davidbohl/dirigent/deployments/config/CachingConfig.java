package org.davidbohl.dirigent.deployments.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;


@Configuration
@EnableCaching
@EnableScheduling
public class CachingConfig {

    private final Logger logger = LoggerFactory.getLogger(CachingConfig.class);

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("deployments");
    }

    @CacheEvict(value = "deployments", allEntries = true)
    @Scheduled(fixedRateString = "${dirigent.deployments.cache.evict.interval}")
    public void emptyDeploymentsCache() {
        logger.info("emptying deployments cache");
    }

}

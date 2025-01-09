package org.davidbohl.dirigent.deployments;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.davidbohl.dirigent.deployments.models.DeploynentConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizer;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

@Service
public class DeploymentsConfigurationProvider implements CacheManagerCustomizer<ConcurrentMapCacheManager> {

    private final GitService gitService;

    @Value("${dirigent.deployments.git.url}")
    private String gitUrl;

    public DeploymentsConfigurationProvider(GitService gitService) {
        this.gitService = gitService;
    }

    @Cacheable("deployments")
    public DeploynentConfiguration getConfiguration() throws IOException, InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

        gitService.cloneOrPull(gitUrl, "config");

        File configFile = new File("config/deployments.yml");

        try {
            return objectMapper.readValue(configFile, DeploynentConfiguration.class);
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public void customize(ConcurrentMapCacheManager cacheManager) {
        cacheManager.setCacheNames(Arrays.asList("deployments"));
    }
}


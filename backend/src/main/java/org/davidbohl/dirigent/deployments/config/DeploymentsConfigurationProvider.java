package org.davidbohl.dirigent.deployments.config;

import java.io.File;

import org.davidbohl.dirigent.deployments.config.exception.DeploymentsConfigurationReadFailedException;
import org.davidbohl.dirigent.deployments.config.model.DeploynentConfiguration;
import org.davidbohl.dirigent.utility.git.GitService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

@Service
public class DeploymentsConfigurationProvider {

    private final GitService gitService;

    @Value("${dirigent.deployments.git.url:}")
    private String gitUrl;

    public DeploymentsConfigurationProvider(GitService gitService) {
        this.gitService = gitService;
    }

    public DeploynentConfiguration getConfiguration() {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

        try {
        if (gitUrl != null)
            gitService.updateRepo(gitUrl, "config", "HEAD");

        File configFile = new File("config/deployments.yml");

            return objectMapper.readValue(configFile, DeploynentConfiguration.class);
        } catch (Throwable e) {
            throw new DeploymentsConfigurationReadFailedException(e);
        }
    }
}


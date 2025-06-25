package org.davidbohl.dirigent.deployments.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.davidbohl.dirigent.deployments.models.DeploynentConfiguration;
import org.davidbohl.dirigent.utility.GitService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class DeploymentsConfigurationProvider {

    private final GitService gitService;

    @Value("${dirigent.deployments.git.url}")
    private String gitUrl;

    public DeploymentsConfigurationProvider(GitService gitService) {
        this.gitService = gitService;
    }

    public DeploynentConfiguration getConfiguration() {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

        try {
        if (gitUrl != null)
            gitService.updateRepo(gitUrl, "config");

        File configFile = new File("config/deployments.yml");

            return objectMapper.readValue(configFile, DeploynentConfiguration.class);
        } catch (Throwable e) {
            throw new DeploymentsConfigurationReadFailed(e);
        }
    }
}


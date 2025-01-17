package org.davidbohl.dirigent.deployments.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.davidbohl.dirigent.deployments.service.GitService;
import org.davidbohl.dirigent.deployments.models.DeploynentConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
public class DeploymentsConfigurationProvider {

    private final GitService gitService;

    @Value("${dirigent.deployments.git.url}")
    private String gitUrl;

    public DeploymentsConfigurationProvider(GitService gitService) {
        this.gitService = gitService;
    }

    public DeploynentConfiguration getConfiguration() throws IOException, InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

        if (gitUrl != null)
            gitService.cloneOrPull(gitUrl, "config");

        File configFile = new File("config/deployments.yml");

        try {
            return objectMapper.readValue(configFile, DeploynentConfiguration.class);
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
    }
}


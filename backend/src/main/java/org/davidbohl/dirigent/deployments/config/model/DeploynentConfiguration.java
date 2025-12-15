package org.davidbohl.dirigent.deployments.config.model;

import java.util.List;

public record DeploynentConfiguration(List<Deployment> deployments, boolean startAllOnStartup) {
}


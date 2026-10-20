package org.davidbohl.dirigent.federation;

import java.util.List;

public record FederatedInstance(String id, String instanceName, boolean available,
                                List<FederatedDeployment> deployments, String error) {
}
package org.davidbohl.dirigent.deployments.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GiteaRepository(String url, @JsonProperty("ssh_url") String sshUrl) {
}

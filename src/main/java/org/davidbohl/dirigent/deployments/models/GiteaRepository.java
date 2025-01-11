package org.davidbohl.dirigent.deployments.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GiteaRepository(@JsonProperty("clone_url") String cloneUrl, @JsonProperty("ssh_url") String sshUrl) {
}

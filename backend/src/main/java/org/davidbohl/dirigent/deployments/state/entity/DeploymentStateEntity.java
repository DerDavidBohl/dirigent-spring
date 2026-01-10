package org.davidbohl.dirigent.deployments.state.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity(name = "deployment_state")
public class DeploymentStateEntity {

    @Id
    private String name;

    @Enumerated(EnumType.STRING)
    private State state;

    // length is required, because messages can be longer than 255 chars.
    @Column(length = 65535)
    private String message;

    public enum State {
        RUNNING, STOPPED, FAILED, UPDATED, UNKNOWN, REMOVED, STARTING, STOPPING
    }
}


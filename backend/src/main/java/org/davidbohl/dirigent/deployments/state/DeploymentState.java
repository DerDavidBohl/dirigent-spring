package org.davidbohl.dirigent.deployments.state;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
public class DeploymentState {

    @Id
    private String name;

    @Enumerated(EnumType.STRING)
    private State state;

    @Column(length = 65535)
    private String message;

    public enum State {
        RUNNING, STOPPED, FAILED, UPDATED, UNKNOWN, REMOVED, STARTING, STOPPING
    }
}

package org.davidbohl.dirigent.deployments.updates.entity;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity(name = "deployment_update")
public class DeploymentUpdateEntity {
    
    @Id
    @GeneratedValue
    private UUID id;
    private String deploymentName;
    private String service;
    private String image;

}
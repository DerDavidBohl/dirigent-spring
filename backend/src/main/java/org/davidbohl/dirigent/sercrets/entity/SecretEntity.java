package org.davidbohl.dirigent.sercrets.entity;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "secret")
public class SecretEntity {
    @Id
    private String key;
    
    private String environmentVariable;

    private String encryptedValue;

    @ElementCollection(fetch = FetchType.EAGER)
    @Column(length = Integer.MAX_VALUE)
    private List<String> deployments;

}
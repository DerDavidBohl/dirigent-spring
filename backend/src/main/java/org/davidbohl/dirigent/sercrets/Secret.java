package org.davidbohl.dirigent.sercrets;

import java.util.List;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Secret {
    @Id
    private String key;
    
    private String environmentVariable;

    private String encryptedValue;

    @ElementCollection
    private List<String> deployments;

}
package org.davidbohl.dirigent.federation;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "dirigent.federation")
public class FederationProperties {

    private List<RemoteInstance> instances = new ArrayList<>();

    @Getter
    @Setter
    public static class RemoteInstance {
        private String id = "";
        private String baseUrl = "";
        private String token = "";
        private String certificateSha256 = "";
    }
}
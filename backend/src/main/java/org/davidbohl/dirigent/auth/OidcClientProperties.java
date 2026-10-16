package org.davidbohl.dirigent.auth;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "dirigent.oidc")
public class OidcClientProperties {

    private boolean enabled;
    private String clientId = "";
    private String clientSecret = "";
    private String issuerUri = "";
    private List<String> scope = new ArrayList<>(List.of("openid", "profile", "email"));
    private String requiredGroup = "";
    private String groupsClaim = "groups";
}
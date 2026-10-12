package org.davidbohl.dirigent.auth;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
public class AuthenticatedOidcUserService {

    private final OidcUserService delegate;
    private final OidcClientProperties properties;

    public AuthenticatedOidcUserService(OidcUserService delegate, OidcClientProperties properties) {
        this.delegate = delegate;
        this.properties = properties;
    }

    public OidcUser loadUser(OidcUserRequest request) throws OAuth2AuthenticationException {
        OidcUser oidcUser = delegate.loadUser(request);
        String requiredGroup = properties.getRequiredGroup();
        if (requiredGroup != null && !requiredGroup.isBlank()
                && !containsRequiredGroup(oidcUser.getClaims().get(properties.getGroupsClaim()), requiredGroup)) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("insufficient_group", "This identity is not in the required OIDC group", null));
        }

        return new DefaultOidcUser(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")),
                oidcUser.getIdToken(), oidcUser.getUserInfo());
    }

    private boolean containsRequiredGroup(Object claim, String requiredGroup) {
        if (claim instanceof Collection<?> groups) {
            return groups.stream().anyMatch(group -> requiredGroup.equals(group));
        }
        return claim instanceof String group && requiredGroup.equals(group);
    }
}
package org.davidbohl.dirigent.auth;

import java.util.Map;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.oauth2.server.resource.introspection.OAuth2IntrospectionException;
import org.springframework.stereotype.Component;
import org.springframework.security.oauth2.server.resource.introspection.OAuth2IntrospectionAuthenticatedPrincipal;

@Component
public class MachineTokenIntrospector implements OpaqueTokenIntrospector {

    private final MachineTokenService tokenService;

    public MachineTokenIntrospector(MachineTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public OAuth2AuthenticatedPrincipal introspect(String token) {
        MachineTokenService.MachineTokenIdentity identity = tokenService.authenticate(token)
                .orElseThrow(() -> new OAuth2IntrospectionException("Invalid bearer token"));
        List<GrantedAuthority> authorities = identity.permissions().stream()
            .<GrantedAuthority>map(permission -> new SimpleGrantedAuthority("SCOPE_" + permission))
                .toList();
        return new OAuth2IntrospectionAuthenticatedPrincipal(identity.username(),
            Map.<String, Object>of("token_id", identity.id()), authorities);
    }
}
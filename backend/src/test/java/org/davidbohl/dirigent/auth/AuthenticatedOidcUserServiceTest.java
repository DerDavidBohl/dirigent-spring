package org.davidbohl.dirigent.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

class AuthenticatedOidcUserServiceTest {

    private final OidcUserService delegate = mock(OidcUserService.class);
    private final OidcUserRequest request = mock(OidcUserRequest.class);

    @Test
    void authorizesAnyAuthenticatedIssuerUserWhenNoGroupIsConfigured() {
        OidcClientProperties properties = new OidcClientProperties();
        when(delegate.loadUser(request)).thenReturn(oidcUser(Map.of("groups", List.of("other"))));

        OidcUser result = new AuthenticatedOidcUserService(delegate, properties).loadUser(request);

        assertEquals("operator-123", result.getSubject());
        assertEquals(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")), List.copyOf(result.getAuthorities()));
    }

    @Test
    void acceptsUserWhoHasConfiguredGroup() {
        OidcClientProperties properties = new OidcClientProperties();
        properties.setRequiredGroup("dirigent-users");
        when(delegate.loadUser(request)).thenReturn(oidcUser(Map.of("groups", List.of("other", "dirigent-users"))));

        OidcUser result = new AuthenticatedOidcUserService(delegate, properties).loadUser(request);

        assertEquals("operator-123", result.getSubject());
    }

    @Test
    void rejectsUserMissingConfiguredGroup() {
        OidcClientProperties properties = new OidcClientProperties();
        properties.setRequiredGroup("dirigent-users");
        when(delegate.loadUser(request)).thenReturn(oidcUser(Map.of("groups", List.of("other"))));

        assertThrows(OAuth2AuthenticationException.class,
                () -> new AuthenticatedOidcUserService(delegate, properties).loadUser(request));
    }

    @Test
    void supportsConfiguringTheGroupClaimName() {
        OidcClientProperties properties = new OidcClientProperties();
        properties.setRequiredGroup("dirigent-users");
        properties.setGroupsClaim("roles");
        when(delegate.loadUser(request)).thenReturn(oidcUser(Map.of("roles", "dirigent-users")));

        OidcUser result = new AuthenticatedOidcUserService(delegate, properties).loadUser(request);

        assertEquals("operator-123", result.getSubject());
    }

    private OidcUser oidcUser(Map<String, Object> additionalClaims) {
        Instant issuedAt = Instant.now();
        Map<String, Object> claims = new java.util.HashMap<>(additionalClaims);
        claims.put("sub", "operator-123");
        claims.put("iss", "https://identity.example.test");
        claims.put("aud", List.of("dirigent"));
        claims.put("iat", issuedAt);
        claims.put("exp", issuedAt.plusSeconds(600));
        return new DefaultOidcUser(List.of(), new OidcIdToken("test-token", issuedAt, issuedAt.plusSeconds(600), claims));
    }
}
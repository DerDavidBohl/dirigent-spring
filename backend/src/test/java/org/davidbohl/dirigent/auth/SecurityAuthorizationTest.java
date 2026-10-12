package org.davidbohl.dirigent.auth;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.davidbohl.dirigent.deployments.management.GiteaDeploymentsController;
import org.springframework.context.ApplicationEventPublisher;
import org.davidbohl.dirigent.sercrets.SecretController;
import org.davidbohl.dirigent.sercrets.SecretService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.introspection.OAuth2IntrospectionAuthenticatedPrincipal;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {AuthController.class, SecretController.class, GiteaDeploymentsController.class})
@Import(SecurityConfiguration.class)
class SecurityAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

        @Autowired
        private PasswordEncoder passwordEncoder;

    @MockitoBean
    private MachineTokenService tokenService;

    @MockitoBean
    private MachineTokenIntrospector tokenIntrospector;

    @MockitoBean
        private AuthenticatedOidcUserService oidcUserService;

        @MockitoBean
        private LocalAccountUserDetailsService localAccountUserDetailsService;

        @MockitoBean
        private LoginAttemptRateLimiter loginAttemptRateLimiter;

        @MockitoBean
        private SecretService secretService;

        @MockitoBean
        private ApplicationEventPublisher applicationEventPublisher;

    @Test
    void rejectsAnonymousApiRequests() throws Exception {
        mockMvc.perform(get("/api/v1/deployments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatesLocalAccountsThroughSpringFormLogin() throws Exception {
        when(localAccountUserDetailsService.loadUserByUsername("operator"))
                .thenReturn(User.withUsername("operator")
                        .password(passwordEncoder.encode("long-enough-password"))
                        .roles("ADMIN")
                        .build());

        mockMvc.perform(post("/api/v1/auth/login")
                        .param("username", "operator")
                        .param("password", "long-enough-password")
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

        @Test
        void rateLimitsBlockedLoginPeerAndAccount() throws Exception {
                when(loginAttemptRateLimiter.isBlocked("127.0.0.1", "operator")).thenReturn(true);
                when(loginAttemptRateLimiter.retryAfterSeconds()).thenReturn(900L);

                mockMvc.perform(post("/api/v1/auth/login")
                                                .param("username", "operator")
                                                .param("password", "attempted-password")
                                                .with(csrf()))
                                .andExpect(status().isTooManyRequests())
                                .andExpect(header().string("Retry-After", "900"));
        }

    @Test
    void permitsMachineTokenOnlyForItsGrantedScope() throws Exception {
        when(tokenIntrospector.introspect("deployments-token")).thenReturn(principal("deployments:read"));
                when(tokenIntrospector.introspect("secrets-token")).thenReturn(principal("secrets:read"));
                when(tokenIntrospector.introspect("webhooks-token")).thenReturn(principal("webhooks:trigger"));
                when(secretService.getAllSecretsWithoutValues()).thenReturn(new ArrayList<>());

        mockMvc.perform(get("/api/v1/deployments")
                        .header("Authorization", "Bearer deployments-token"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/secrets")
                        .header("Authorization", "Bearer deployments-token"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/secrets")
                        .header("Authorization", "Bearer secrets-token"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/auth/tokens")
                        .header("Authorization", "Bearer secrets-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void permitsGiteaWebhookOnlyForDedicatedWebhookScope() throws Exception {
                when(tokenIntrospector.introspect("deployments-token")).thenReturn(principal("deployments:write"));
                when(tokenIntrospector.introspect("webhooks-token")).thenReturn(principal("webhooks:trigger"));
        String payload = """
                {"ref":"refs/heads/main","repository":{"clone_url":"https://git.example/repo.git","ssh_url":""}}
                """;

        mockMvc.perform(post("/api/v1/gitea")
                        .with(csrf())
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/gitea")
                        .header("Authorization", "Bearer deployments-token")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/gitea")
                        .header("Authorization", "Bearer webhooks-token")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isOk());
    }

    @Test
    void requiresCsrfForSessionAuthenticatedMutations() throws Exception {
        mockMvc.perform(post("/api/v1/deployments/example/stop")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("operator").roles("ADMIN")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/deployments/example/stop")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("operator").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    private OAuth2IntrospectionAuthenticatedPrincipal principal(String... permissions) {
        List<GrantedAuthority> authorities = java.util.Arrays.stream(permissions)
                .<GrantedAuthority>map(permission -> new SimpleGrantedAuthority("SCOPE_" + permission))
                .toList();
        return new OAuth2IntrospectionAuthenticatedPrincipal("automation",
                Map.<String, Object>of("sub", "automation"), authorities);
    }
}
package org.davidbohl.dirigent.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.function.Supplier;

import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.util.StringUtils;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
@EnableConfigurationProperties(OidcClientProperties.class)
public class SecurityConfiguration {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    CsrfTokenRepository csrfTokenRepository(@Value("${dirigent.auth.cookie-secure:true}") boolean secureCookies) {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieCustomizer(cookie -> cookie.secure(secureCookies).sameSite("Lax"));
        return repository;
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    OidcUserService oidcUserService() {
        return new OidcUserService();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            MachineTokenIntrospector tokenIntrospector,
                                            AuthenticatedOidcUserService oidcUserService,
                                            OidcClientProperties oidcClientProperties,
                                            LoginAttemptRateLimiter loginAttemptRateLimiter,
                                            CsrfTokenRepository csrfTokenRepository,
                                            SecurityContextRepository securityContextRepository) throws Exception {
        http
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                        .ignoringRequestMatchers((RequestMatcher) request -> {
                            String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
                            return authorization != null && authorization.startsWith("Bearer ");
                        }))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .securityContext(context -> context.securityContextRepository(securityContextRepository))
                .authorizeHttpRequests(authorize -> authorize
                    .requestMatchers("/api/v1/auth/csrf", "/api/v1/auth/options", "/api/v1/auth/login",
                        "/oauth2/**", "/login/oauth2/**").permitAll()
                    .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().permitAll())
                .exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        request -> request.getRequestURI().startsWith("/api/")))
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/api/v1/auth/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                    .successHandler((request, response, authentication) ->
                            {
                                loginAttemptRateLimiter.recordSuccess(request.getRemoteAddr(), request.getParameter("username"));
                                response.setStatus(HttpStatus.NO_CONTENT.value());
                            })
                    .failureHandler((request, response, exception) ->
                            {
                                loginAttemptRateLimiter.recordFailure(request.getRemoteAddr(), request.getParameter("username"));
                                response.sendError(HttpStatus.UNAUTHORIZED.value());
                            }))
                .logout(logout -> logout
                        .logoutUrl("/api/v1/auth/logout")
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)));

        if (oidcClientProperties.isEnabled()) {
            http.oauth2Login(oauth2 -> oauth2
                    .loginPage("/login")
                    .userInfoEndpoint(userInfo -> userInfo.oidcUserService(oidcUserService::loadUser))
                    .defaultSuccessUrl("/", true));
        }

        http.oauth2ResourceServer(resourceServer -> resourceServer.opaqueToken(opaque -> opaque.introspector(tokenIntrospector)));
        http.addFilterBefore(new LoginRateLimitFilter(loginAttemptRateLimiter), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private static final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {
        private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
        private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response,
                           Supplier<CsrfToken> csrfToken) {
            xor.handle(request, response, csrfToken);
        }

        @Override
        public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
            String headerValue = request.getHeader(csrfToken.getHeaderName());
            return StringUtils.hasText(headerValue)
                    ? plain.resolveCsrfTokenValue(request, csrfToken)
                    : xor.resolveCsrfTokenValue(request, csrfToken);
        }
    }
}
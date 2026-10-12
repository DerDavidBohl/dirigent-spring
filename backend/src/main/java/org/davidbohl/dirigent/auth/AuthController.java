package org.davidbohl.dirigent.auth;

import java.util.List;

import org.davidbohl.dirigent.auth.dto.AuthOptionsResponse;
import org.davidbohl.dirigent.auth.dto.AuthenticatedUserResponse;
import org.davidbohl.dirigent.auth.dto.MachineTokenCreateRequest;
import org.davidbohl.dirigent.auth.dto.MachineTokenCreatedResponse;
import org.davidbohl.dirigent.auth.dto.MachineTokenSummary;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final MachineTokenService tokenService;
    private final boolean oidcEnabled;

    public AuthController(MachineTokenService tokenService, OidcClientProperties oidcClientProperties) {
        this.tokenService = tokenService;
        this.oidcEnabled = oidcClientProperties.isEnabled();
    }

    @GetMapping("/options")
    public AuthOptionsResponse getOptions() {
        return new AuthOptionsResponse(oidcEnabled, oidcEnabled ? "/oauth2/authorization/dirigent" : null);
    }

    @GetMapping("/csrf")
    public String getCsrfToken(CsrfToken csrfToken) {
        return csrfToken.getToken();
    }

    @GetMapping("/me")
    public AuthenticatedUserResponse getCurrentUser(Authentication authentication) {
        return new AuthenticatedUserResponse(authentication.getName(), authentication instanceof BearerTokenAuthentication);
    }

    @GetMapping("/tokens")
    @Secured("ROLE_ADMIN")
    public List<MachineTokenSummary> listTokens() {
        return tokenService.list();
    }

    @PostMapping("/tokens")
    @Secured("ROLE_ADMIN")
    public MachineTokenCreatedResponse createToken(@RequestBody MachineTokenCreateRequest request,
                                                   Authentication authentication) {
        try {
            return tokenService.create(request, authentication.getName());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
    }

    @DeleteMapping("/tokens/{id}")
    @Secured("ROLE_ADMIN")
    public void revokeToken(@PathVariable long id) {
        try {
            tokenService.revoke(id);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage());
        }
    }
}
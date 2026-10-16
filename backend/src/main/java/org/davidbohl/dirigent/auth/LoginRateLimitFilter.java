package org.davidbohl.dirigent.auth;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/v1/auth/login";

    private final LoginAttemptRateLimiter rateLimiter;

    public LoginRateLimitFilter(LoginAttemptRateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String clientAddress = request.getRemoteAddr();
        String username = request.getParameter("username");
        String loginRequestPath = request.getContextPath() + LOGIN_PATH;
        if ("POST".equals(request.getMethod()) && loginRequestPath.equals(request.getRequestURI())
            && rateLimiter.isBlocked(clientAddress, username)) {
            response.setHeader("Retry-After", Long.toString(rateLimiter.retryAfterSeconds()));
            response.sendError(HttpStatus.TOO_MANY_REQUESTS.value());
            return;
        }
        filterChain.doFilter(request, response);
    }
}
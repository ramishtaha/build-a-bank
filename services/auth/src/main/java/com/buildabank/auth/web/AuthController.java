// services/auth/src/main/java/com/buildabank/auth/web/AuthController.java
package com.buildabank.auth.web;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.buildabank.auth.security.JwtService;
import com.buildabank.auth.user.UserService;
import com.buildabank.auth.web.AuthDtos.LoginRequest;
import com.buildabank.auth.web.AuthDtos.MeResponse;
import com.buildabank.auth.web.AuthDtos.TokenResponse;

/**
 * The auth API. {@code /login} is public (it issues tokens); {@code /me} requires a valid token
 * (authentication); {@code /admin} additionally requires the ADMIN role (authorization) — the security
 * filter chain enforces the last two before this controller is ever reached.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService users;
    private final JwtService jwt;

    public AuthController(UserService users, JwtService jwt) {
        this.users = users;
        this.jwt = jwt;
    }

    /** Authenticate (BCrypt) and issue a JWT → 200 with the token, or 401 if the credentials are wrong. */
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return users.authenticate(request.username(), request.password())
                .map(user -> ResponseEntity.ok(
                        new TokenResponse(jwt.issue(user.username(), user.roles()), jwt.ttlSeconds())))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    /** Who am I? Reads the identity from the validated JWT (the filter chain populated the Authentication). */
    @GetMapping("/me")
    public MeResponse me(Authentication authentication) {
        // Report only roles — Spring Security 7 also grants authentication-factor authorities (e.g.
        // FACTOR_BEARER) which are an internal detail, not part of this API's role contract.
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .sorted().toList();
        return new MeResponse(authentication.getName(), roles);
    }

    /** ADMIN-only via URL rule (the filter chain's `requestMatchers("/api/auth/admin").hasRole("ADMIN")`). */
    @GetMapping("/admin")
    public Map<String, String> admin() {
        return Map.of("message", "admin access granted");
    }

    /**
     * ADMIN-only via <strong>method security</strong> ({@code @PreAuthorize}) instead of a URL rule — the
     * authorization lives on the method, closer to the domain and reusable from any caller. (There's no URL
     * rule for this path beyond `anyRequest().authenticated()`, so the @PreAuthorize is what enforces ADMIN.)
     */
    @GetMapping("/admin-method")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> adminViaMethodSecurity() {
        return Map.of("message", "admin (method security) access granted");
    }
}

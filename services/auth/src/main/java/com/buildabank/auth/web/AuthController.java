// services/auth/src/main/java/com/buildabank/auth/web/AuthController.java
package com.buildabank.auth.web;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.buildabank.auth.security.JwtService;
import com.buildabank.auth.security.RefreshTokenService;
import com.buildabank.auth.user.UserService;
import com.buildabank.auth.web.AuthDtos.LoginRequest;
import com.buildabank.auth.web.AuthDtos.MeResponse;
import com.buildabank.auth.web.AuthDtos.TokenResponse;

/**
 * The auth API. {@code /login} is public (it issues tokens); {@code /me} requires a valid token
 * (authentication); {@code /admin} additionally requires the ADMIN role (authorization) — the security
 * filter chain enforces the last two before this controller is ever reached.
 *
 * <p>Step 32: login now ALSO plants a refresh token in an <strong>httpOnly cookie</strong> (JS can't read
 * it), and {@code /refresh} + {@code /logout} manage the session it represents. The access JWT stays in the
 * response body (the SPA keeps it in memory only). Honest scope: this defeats credential
 * <em>theft/persistence</em> (nothing long-lived sits in JS-readable storage) — an XSS payload running in
 * the live page can still <em>use</em> the session while it's open. The defenses against that are CSP,
 * output encoding, and dependency hygiene, not storage choice.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /** The refresh cookie's name — 'host-bound' by convention; the browser scopes it to the gateway origin. */
    static final String REFRESH_COOKIE = "bab_refresh";

    private final UserService users;
    private final JwtService jwt;
    private final RefreshTokenService refreshTokens;
    private final boolean cookieSecure;

    public AuthController(UserService users, JwtService jwt, RefreshTokenService refreshTokens,
                          @Value("${bank.auth.cookie-secure:false}") boolean cookieSecure) {
        this.users = users;
        this.jwt = jwt;
        this.refreshTokens = refreshTokens;
        this.cookieSecure = cookieSecure;   // false for local http; MUST be true behind TLS (prod)
    }

    /**
     * Authenticate (BCrypt) → 200 with a short-lived access JWT in the body + a refresh token in an
     * httpOnly cookie, or 401 if the credentials are wrong.
     */
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return users.authenticate(request.username(), request.password())
                .map(user -> ResponseEntity.ok()
                        .header(HttpHeaders.SET_COOKIE, refreshCookie(refreshTokens.issue(user.username()),
                                refreshTokens.ttlSeconds()).toString())
                        .body(new TokenResponse(jwt.issue(user.username(), user.roles()), jwt.ttlSeconds())))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    /**
     * Trade a valid refresh cookie for a fresh access JWT + a ROTATED refresh cookie. 401 when the cookie is
     * missing, unknown, expired, revoked, or replayed-after-grace (reuse revokes the whole family — see
     * {@link RefreshTokenService#rotate}); 409 for the benign two-tabs race (retry with the current cookie).
     * Authenticated by the cookie itself, so it's permitAll in the chain.
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // Pattern-switch over the sealed result (Step 2's sealed types earning their keep): the compiler
        // enforces that every outcome is handled — add a fourth result type and this stops compiling.
        return switch (refreshTokens.rotate(refreshToken)) {
            case RefreshTokenService.RotationResult.Rotated(String rawToken, String username) -> {
                var user = users.find(username).orElseThrow();   // seeded users never vanish
                yield ResponseEntity.ok()
                        .header(HttpHeaders.SET_COOKIE,
                                refreshCookie(rawToken, refreshTokens.ttlSeconds()).toString())
                        .body(new TokenResponse(jwt.issue(user.username(), user.roles()), jwt.ttlSeconds()));
            }
            case RefreshTokenService.RotationResult.ConcurrentRotation() ->
                    ResponseEntity.status(HttpStatus.CONFLICT).build();
            case RefreshTokenService.RotationResult.Invalid() ->
                    ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        };
    }

    /** Revoke the refresh family and clear the cookie (Max-Age=0 tells the browser to delete it) → 204. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        if (refreshToken != null) {
            refreshTokens.revoke(refreshToken);
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookie("", 0).toString())
                .build();
    }

    /**
     * The refresh cookie, locked down: httpOnly (no JS access), SameSite=Strict (not sent cross-site),
     * Path=/api/auth (sent ONLY to auth endpoints — never rides along on /bank calls), Secure per config.
     */
    private ResponseCookie refreshCookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(maxAgeSeconds)
                .build();
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

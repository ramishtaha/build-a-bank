// services/auth/src/main/java/com/buildabank/auth/security/SecurityConfig.java
package com.buildabank.auth.security;

import java.nio.charset.StandardCharsets;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * The heart of Step 16: the <strong>security filter chain</strong> + the JWT and password machinery.
 *
 * <p>This is a <strong>stateless</strong> API secured by JWTs (no server session, no cookies), so we disable
 * CSRF (there's no cookie/session for a CSRF attack to ride) and set the session policy to STATELESS. The
 * authorization rules decide, per request path, what's public vs. requires authentication vs. requires a role.
 * As an OAuth2 <em>resource server</em>, Spring validates the {@code Authorization: Bearer <jwt>} on every
 * protected request using the {@link JwtDecoder}; we issue those same tokens with the {@link JwtEncoder}.
 */
@Configuration
public class SecurityConfig {

    private final byte[] secret;
    private final MacAlgorithm macAlgorithm = MacAlgorithm.HS256;

    public SecurityConfig(@Value("${bank.jwt.secret}") String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);   // HS256 needs >= 32 bytes (256 bits)
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Stateless JWT API: no session, no cookies → CSRF is not applicable (and would block our clients).
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/auth/login", "/actuator/health").permitAll()   // public
                        .requestMatchers("/api/auth/admin").hasRole("ADMIN")                   // authZ: role required
                        .anyRequest().authenticated())                                         // everything else: authN
                // Validate incoming Bearer JWTs; map the "roles" claim to Spring authorities.
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter())));
        return http.build();
    }

    /** BCrypt for password hashing — slow-by-design + per-hash salt (never store or compare plaintext). */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Signs JWTs with the shared HMAC secret. */
    @Bean
    JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey()));
    }

    /** Validates JWTs (signature + expiry) with the same HMAC secret. */
    @Bean
    JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(secretKey()).macAlgorithm(macAlgorithm).build();
    }

    MacAlgorithm macAlgorithm() {
        return macAlgorithm;
    }

    SecretKeySpec secretKey() {
        return new SecretKeySpec(secret, "HmacSHA256");
    }

    /** Maps the token's {@code roles} claim (already like "ROLE_USER") straight to granted authorities. */
    private JwtAuthenticationConverter jwtAuthConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("");   // the claim already carries the ROLE_ prefix
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}

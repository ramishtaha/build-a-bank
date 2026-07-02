// services/auth/src/main/java/com/buildabank/auth/security/SecurityConfig.java
package com.buildabank.auth.security;

import java.util.UUID;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

/**
 * Step 17 hardens Step 16's security: <strong>asymmetric (RS256) signing</strong> with a published
 * <strong>JWKS</strong>, plus <strong>method security</strong> ({@code @EnableMethodSecurity}).
 *
 * <p>Why asymmetric? With the Step-16 HMAC secret, every validator also held the secret and could forge
 * tokens. Now the auth service signs with a <strong>private</strong> RSA key and publishes the matching
 * <strong>public</strong> key at {@code /oauth2/jwks} — so other services (demand-account, Step 17) can
 * validate tokens with the public key but cannot mint them (least privilege). The key pair is generated at
 * startup (ephemeral; validators fetch the current key from JWKS — production would persist/rotate it via
 * a keystore/Vault, Phase H).
 */
@Configuration
@EnableMethodSecurity   // turns on @PreAuthorize (method-level authorization), used in AuthController
public class SecurityConfig {

    private final RSAKey rsaKey = generateRsaKey();

    private static RSAKey generateRsaKey() {
        try {
            return new RSAKeyGenerator(2048).keyID(UUID.randomUUID().toString()).generate();
        } catch (Exception e) {
            throw new IllegalStateException("failed to generate RSA key", e);
        }
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())                                          // stateless token API
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(authorize -> authorize
                        // public: login, health, and the JWKS so resource servers can fetch the public key.
                        // Step 32: refresh + logout are 'public' to the FILTER CHAIN but authenticated by the
                        // httpOnly refresh cookie inside the controller (no Bearer token exists yet on refresh).
                        .requestMatchers("/api/auth/login", "/api/auth/refresh", "/api/auth/logout",
                                "/actuator/health", "/oauth2/jwks").permitAll()
                        .requestMatchers("/api/auth/admin").hasRole("ADMIN")           // URL-based authZ
                        .anyRequest().authenticated())                                 // everything else: authN
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter())));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** The signing key pair, as a JWK source (private key included — used only to SIGN). */
    @Bean
    JWKSource<SecurityContext> jwkSource() {
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    /** Signs JWTs with the RSA private key (RS256). */
    @Bean
    JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    /** Validates JWTs with the RSA public key (this service validates its own tokens too). */
    @Bean
    JwtDecoder jwtDecoder() {
        try {
            return NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build();
        } catch (Exception e) {
            throw new IllegalStateException("failed to build JwtDecoder", e);
        }
    }

    /** The PUBLIC half only — what we publish at /oauth2/jwks (no private key material). */
    @Bean
    JWKSet publicJwkSet() {
        return new JWKSet(rsaKey.toPublicJWK());
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

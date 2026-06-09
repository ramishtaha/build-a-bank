// services/demand-account/src/main/java/com/buildabank/account/web/SecurityConfig.java
package com.buildabank.account.web;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Step 17: demand-account becomes an OAuth2 <strong>resource server</strong> — every money endpoint now
 * requires a valid JWT issued by the auth service (validated with auth's public key via {@code jwk-set-uri},
 * so this service never holds a signing secret). Health and the API docs stay public; everything under
 * {@code /api/**} requires authentication, and {@code @EnableMethodSecurity} turns on {@code @PreAuthorize}
 * for fine-grained, domain-level rules (e.g. admin-only operations).
 *
 * <p><strong>Step 18 (secure-by-default hardening):</strong> the threat model
 * ({@code security/threat-model.md}) drove three explicit edge defaults here — security
 * <strong>response headers</strong> (anti-clickjacking, MIME-sniff lock, referrer suppression, HSTS),
 * a <strong>deny-by-default CORS</strong> policy (no browser origin is allowed unless explicitly
 * allow-listed via {@code app.security.cors.allowed-origins}), and the already-present stateless/CSRF
 * posture. These shrink the browser-facing attack surface (OWASP A05 Security Misconfiguration / A07).
 */
@Configuration
@EnableWebSecurity      // imports HttpSecurityConfiguration (provides the HttpSecurity bean — needed in @WebMvcTest slices too)
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())                                  // stateless token API (no cookies/session)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(Customizer.withDefaults())                               // resolves the corsConfigurationSource bean BY NAME (Step 18, deny-by-default)
                .headers(headers -> headers                                    // Step 18: explicit secure response headers
                        .frameOptions(frame -> frame.deny())                   // X-Frame-Options: DENY — no framing (anti-clickjacking)
                        .contentTypeOptions(Customizer.withDefaults())         // X-Content-Type-Options: nosniff — no MIME sniffing
                        .referrerPolicy(ref -> ref.policy(ReferrerPolicy.NO_REFERRER))  // Referrer-Policy: no-referrer — don't leak URLs
                        .httpStrictTransportSecurity(hsts -> hsts             // HSTS — force HTTPS (only emitted over TLS)
                                .includeSubDomains(true).maxAgeInSeconds(31_536_000)))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated())                         // all /api/** money endpoints: authN
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(rolesConverter())));
        return http.build();
    }

    /**
     * Deny-by-default CORS. With no configured origins (the default), <em>no</em> cross-origin browser
     * request is allowed — a preflight from an un-listed origin gets 403 and no
     * {@code Access-Control-Allow-Origin} header, so a malicious site cannot script our API from a
     * victim's browser. Allow specific front-ends by setting {@code app.security.cors.allowed-origins}
     * (e.g. the React app at {@code http://localhost:5173}, Step 29). Server-to-server callers send no
     * {@code Origin} header, so they are unaffected. Note: CORS is a browser guardrail, NOT authZ — the
     * JWT requirement and method security above are the real access controls.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.security.cors.allowed-origins:}") List<String> allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins.stream().filter(o -> !o.isBlank()).toList());  // empty ⇒ deny all
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key"));
        config.setMaxAge(Duration.ofHours(1));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /** Map the JWT's {@code roles} claim (already "ROLE_*") to Spring authorities — same scheme as the auth service. */
    private JwtAuthenticationConverter rolesConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}

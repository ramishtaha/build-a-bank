// services/demand-account/src/main/java/com/buildabank/account/web/SecurityConfig.java
package com.buildabank.account.web;

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

/**
 * Step 17: demand-account becomes an OAuth2 <strong>resource server</strong> — every money endpoint now
 * requires a valid JWT issued by the auth service (validated with auth's public key via {@code jwk-set-uri},
 * so this service never holds a signing secret). Health and the API docs stay public; everything under
 * {@code /api/**} requires authentication, and {@code @EnableMethodSecurity} turns on {@code @PreAuthorize}
 * for fine-grained, domain-level rules (e.g. admin-only operations).
 */
@Configuration
@EnableWebSecurity      // imports HttpSecurityConfiguration (provides the HttpSecurity bean — needed in @WebMvcTest slices too)
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())                                  // stateless token API
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated())                         // all /api/** money endpoints: authN
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(rolesConverter())));
        return http.build();
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

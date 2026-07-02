// gateway/src/main/java/com/buildabank/gateway/GatewayCorsConfig.java
package com.buildabank.gateway;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Step 29 · CORS at the gateway (the front door), so the browser-based React app (Vite dev server,
 * http://localhost:5173) can call it cross-origin. A standard servlet {@link CorsFilter} — it answers the
 * browser's OPTIONS preflight and adds {@code Access-Control-Allow-Origin} to responses.
 *
 * <p><strong>Deny-by-default</strong> (same posture as demand-account, Step 18): only the origins listed in
 * {@code app.security.cors.allowed-origins} are allowed. The dev default is the Vite origin; override
 * {@code APP_CORS_ALLOWED_ORIGINS} per environment and tighten for production.
 */
@Configuration
class GatewayCorsConfig {

    @Bean
    CorsFilter corsFilter(@Value("${app.security.cors.allowed-origins:}") List<String> allowedOrigins) {
        List<String> origins = allowedOrigins.stream().filter(o -> !o.isBlank()).toList();
        // Step 32 guard: with credentialed CORS, /api/auth/refresh answers a READABLE access token to any
        // allowed origin. A wildcard here would hand every website a signed-in user visits a valid bank
        // token (full account takeover, no XSS needed) — so a wildcard kills the app at startup, loudly.
        if (origins.contains("*")) {
            throw new IllegalStateException(
                    "app.security.cors.allowed-origins must list explicit origins — '*' is forbidden with "
                            + "credentialed CORS (the refresh flow would leak access tokens to any site)");
        }
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origins); // empty ⇒ deny all
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key"));
        // Step 32: the refresh flow authenticates with an httpOnly COOKIE. A cross-origin fetch only sends /
        // stores cookies when the request says credentials:'include' AND the server answers
        // Access-Control-Allow-Credentials: true. Only needed in dev (5173 → 8080); in the shipped topology
        // the SPA is served BY the gateway (same origin), and CORS doesn't apply at all.
        config.setAllowCredentials(true);   // NEVER together with a wildcard origin — ours is an exact list
        config.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}

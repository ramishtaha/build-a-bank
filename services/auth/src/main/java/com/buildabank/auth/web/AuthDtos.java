// services/auth/src/main/java/com/buildabank/auth/web/AuthDtos.java
package com.buildabank.auth.web;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

/** Request/response records for the auth API (grouped to keep the package tidy). */
public final class AuthDtos {

    private AuthDtos() {
    }

    /** Login credentials. */
    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    /** Issued token + how long it's valid (seconds). */
    public record TokenResponse(String token, long expiresInSeconds) {
    }

    /** The authenticated principal's identity, derived from the validated JWT. */
    public record MeResponse(String username, List<String> roles) {
    }
}

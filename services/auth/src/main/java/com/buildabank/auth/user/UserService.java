// services/auth/src/main/java/com/buildabank/auth/user/UserService.java
package com.buildabank.auth.user;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * A tiny in-memory user store with <strong>BCrypt-hashed</strong> passwords (a real DB-backed store, plus
 * OIDC, comes in Step 17+). Passwords are never stored or compared in plaintext: we keep only the BCrypt
 * hash and verify with a constant-time {@link PasswordEncoder#matches}.
 */
@Service
public class UserService {

    /** A stored user: username, the BCrypt hash (never the plaintext), and granted roles. */
    public record StoredUser(String username, String passwordHash, List<String> roles) {
    }

    private final PasswordEncoder passwordEncoder;
    private final Map<String, StoredUser> users = new ConcurrentHashMap<>();

    public UserService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        // Seed demo users (fake credentials only). Passwords are hashed at startup — never persisted in clear.
        register("alice", "password", List.of("ROLE_USER"));
        register("admin", "admin123", List.of("ROLE_USER", "ROLE_ADMIN"));
    }

    private void register(String username, String rawPassword, List<String> roles) {
        users.put(username, new StoredUser(username, passwordEncoder.encode(rawPassword), roles));
    }

    /** Look a user up by username — no password check (Step 32: the refresh flow re-loads roles). */
    public Optional<StoredUser> find(String username) {
        return Optional.ofNullable(users.get(username));
    }

    /** Verify credentials with BCrypt; returns the user only if the password matches. */
    public Optional<StoredUser> authenticate(String username, String rawPassword) {
        StoredUser user = users.get(username);
        if (user != null && passwordEncoder.matches(rawPassword, user.passwordHash())) {
            return Optional.of(user);
        }
        return Optional.empty();
    }
}

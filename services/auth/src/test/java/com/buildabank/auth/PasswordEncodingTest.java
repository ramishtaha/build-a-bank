// services/auth/src/test/java/com/buildabank/auth/PasswordEncodingTest.java
package com.buildabank.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * BCrypt fundamentals (pure unit): the stored value is a one-way hash (never the plaintext), the same
 * password hashes differently each time (a random per-hash salt), and verification is by {@code matches},
 * not equality.
 */
class PasswordEncodingTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    void hashIsNotThePlaintext_andVerifies() {
        String hash = encoder.encode("password");

        assertThat(hash).isNotEqualTo("password");      // never store plaintext
        assertThat(hash).startsWith("$2");              // BCrypt prefix
        assertThat(encoder.matches("password", hash)).isTrue();
        assertThat(encoder.matches("wrong", hash)).isFalse();
    }

    @Test
    void samePasswordHashesDifferently_dueToSalt() {
        String a = encoder.encode("password");
        String b = encoder.encode("password");

        assertThat(a).isNotEqualTo(b);                  // distinct salts → distinct hashes...
        assertThat(encoder.matches("password", a)).isTrue();   // ...yet both verify
        assertThat(encoder.matches("password", b)).isTrue();
    }
}

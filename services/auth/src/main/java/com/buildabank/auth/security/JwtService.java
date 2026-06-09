// services/auth/src/main/java/com/buildabank/auth/security/JwtService.java
package com.buildabank.auth.security;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Issues signed JWTs. A JWT is three base64url parts — header, claims, signature — where the signature
 * (HMAC-SHA256 here) lets any holder of the secret verify the token wasn't tampered with. We put the
 * username in {@code sub}, the roles in a {@code roles} claim, and an expiry in {@code exp} so the token is
 * short-lived.
 */
@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final long ttlMinutes;
    private final String issuer;

    public JwtService(JwtEncoder jwtEncoder,
                      @Value("${bank.jwt.ttl-minutes:30}") long ttlMinutes,
                      @Value("${bank.jwt.issuer:build-a-bank-auth}") String issuer) {
        this.jwtEncoder = jwtEncoder;
        this.ttlMinutes = ttlMinutes;
        this.issuer = issuer;
    }

    /** Mint a signed JWT for the user with their roles, valid for {@code ttlMinutes}. */
    public String issue(String username, List<String> roles) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plus(Duration.ofMinutes(ttlMinutes)))
                .subject(username)
                .claim("roles", roles)
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();   // asymmetric (Step 17)
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long ttlSeconds() {
        return ttlMinutes * 60;
    }
}

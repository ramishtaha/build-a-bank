// services/auth/src/main/java/com/buildabank/auth/web/JwksController.java
package com.buildabank.auth.web;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nimbusds.jose.jwk.JWKSet;

/**
 * Publishes the <strong>JWKS</strong> (JSON Web Key Set) — the auth service's <em>public</em> signing key(s)
 * — at {@code /oauth2/jwks}. Resource servers (e.g. demand-account in Step 17) point their
 * {@code jwk-set-uri} here to fetch the public key and validate tokens, without ever holding the private key.
 * Only public key material is exposed (never the private key).
 */
@RestController
public class JwksController {

    private final JWKSet publicJwkSet;

    public JwksController(JWKSet publicJwkSet) {
        this.publicJwkSet = publicJwkSet;
    }

    @GetMapping("/oauth2/jwks")
    public Map<String, Object> jwks() {
        return publicJwkSet.toJSONObject();   // { "keys": [ { "kty":"RSA", "kid":..., "n":..., "e":"AQAB" } ] }
    }
}

// services/demand-account/src/test/java/com/buildabank/account/DemandAccountIntegrationTest.java
package com.buildabank.account;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Date;
import java.util.List;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import com.buildabank.account.domain.AccountRepository;
import com.buildabank.account.domain.LedgerEntryRepository;

/**
 * End-to-end over a REAL HTTP socket against a REAL Postgres (Testcontainers). Since Step 17 the money
 * endpoints require a JWT, so we mint real RS256 tokens with a test key and validate them with a test
 * {@link JwtDecoder} (overriding the production {@code jwk-set-uri} — we don't run the auth service here; see
 * §12.8 in the lesson). Health and the API docs stay public.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({ContainersConfig.class, DemandAccountIntegrationTest.JwtTestConfig.class})
class DemandAccountIntegrationTest {

    // One test signing key for the whole class: the resource server validates with its public half,
    // and we mint tokens with its private half.
    private static final RSAKey TEST_KEY = generateKey();

    private static RSAKey generateKey() {
        try {
            return new RSAKeyGenerator(2048).keyID("test-key").generate();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Override the production JwtDecoder (jwk-set-uri) with one backed by our test public key. */
    @TestConfiguration
    static class JwtTestConfig {
        @Bean
        JwtDecoder jwtDecoder() {
            try {
                return NimbusJwtDecoder.withPublicKey(TEST_KEY.toRSAPublicKey()).build();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private static String mintToken(List<String> roles) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject("alice")
                    .claim("roles", roles)
                    .issueTime(new Date())
                    .expirationTime(new Date(System.currentTimeMillis() + 600_000))
                    .build();
            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(TEST_KEY.getKeyID()).build(), claims);
            jwt.sign(new RSASSASigner(TEST_KEY.toPrivateKey()));
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static final String USER_TOKEN = mintToken(List.of("ROLE_USER"));
    private static final String ADMIN_TOKEN = mintToken(List.of("ROLE_USER", "ROLE_ADMIN"));

    @LocalServerPort
    int port;

    @Autowired
    AccountRepository accounts;

    @Autowired
    LedgerEntryRepository ledger;

    @Autowired
    com.buildabank.account.domain.IdempotencyRecordRepository idempotencyKeys;

    private final HttpClient http = HttpClient.newHttpClient();
    private String base;

    @BeforeEach
    void setup() {
        idempotencyKeys.deleteAll();
        ledger.deleteAll();
        accounts.deleteAll();
        base = "http://localhost:" + port;
    }

    @Test
    void unauthenticatedMoneyRequestIs401() throws Exception {
        // No Authorization header → the resource server rejects before any controller runs (Step 17).
        HttpResponse<String> r = http.send(HttpRequest.newBuilder(URI.create(base + "/api/accounts/ACC-A"))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(r.statusCode()).isEqualTo(401);
    }

    @Test
    void openTransferQuery_andRefuseOverdraft_overHttp() throws Exception {
        assertThat(post("/api/accounts",
                "{\"accountNumber\":\"ACC-A\",\"currency\":\"USD\",\"openingBalance\":200.00}").statusCode())
                .isEqualTo(201);
        assertThat(post("/api/accounts",
                "{\"accountNumber\":\"ACC-B\",\"currency\":\"USD\",\"openingBalance\":0.00}").statusCode())
                .isEqualTo(201);

        HttpResponse<String> transfer = post("/api/transfers",
                "{\"from\":\"ACC-A\",\"to\":\"ACC-B\",\"amount\":50.00,\"description\":\"rent\"}");
        assertThat(transfer.statusCode()).isEqualTo(200);
        assertThat(transfer.body()).contains("transactionId");
        assertThat(transfer.headers().firstValue("X-Request-Id")).isPresent();
        assertThat(transfer.headers().firstValue("X-Timing-Enabled")).hasValue("true");

        HttpResponse<String> balanceA = get("/api/accounts/ACC-A");
        assertThat(balanceA.statusCode()).isEqualTo(200);
        assertThat(balanceA.body()).contains("150");   // 200 − 50

        HttpResponse<String> overdraft = post("/api/transfers",
                "{\"from\":\"ACC-A\",\"to\":\"ACC-B\",\"amount\":9999.00}");
        assertThat(overdraft.statusCode()).isEqualTo(422);
        assertThat(overdraft.headers().firstValue("Content-Type")).hasValueSatisfying(
                ct -> assertThat(ct).contains("application/problem+json"));
        assertThat(overdraft.body()).contains("\"title\":\"Insufficient funds\"").contains("\"status\":422");
    }

    @Test
    void openApiDocsAndSwaggerUiAreServed_withoutAuth() throws Exception {
        // Docs are public (permitAll) — no token needed.
        HttpResponse<String> apiDocs = http.send(HttpRequest.newBuilder(URI.create(base + "/v3/api-docs"))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(apiDocs.statusCode()).isEqualTo(200);
        assertThat(apiDocs.body()).contains("Demand Account API").contains("/api/transfers");

        HttpResponse<String> swagger = http.send(HttpRequest.newBuilder(
                URI.create(base + "/swagger-ui/index.html")).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(swagger.statusCode()).isEqualTo(200);
    }

    @Test
    void v1Idempotency_pagination_andDeprecation_overHttp() throws Exception {
        post("/api/accounts", "{\"accountNumber\":\"ACC-A\",\"currency\":\"USD\",\"openingBalance\":200.00}");
        post("/api/accounts", "{\"accountNumber\":\"ACC-B\",\"currency\":\"USD\",\"openingBalance\":0.00}");

        String body = "{\"from\":\"ACC-A\",\"to\":\"ACC-B\",\"amount\":50.00,\"description\":\"rent\"}";
        assertThat(postWithHeader("/api/v1/transfers", body, "Idempotency-Key", "K1").statusCode()).isEqualTo(200);
        assertThat(postWithHeader("/api/v1/transfers", body, "Idempotency-Key", "K1").statusCode()).isEqualTo(200);
        assertThat(get("/api/accounts/ACC-A").body()).contains("150");   // moved once

        postWithHeader("/api/v1/transfers", body, "Idempotency-Key", "K2");
        postWithHeader("/api/v1/transfers", body, "Idempotency-Key", "K3");

        HttpResponse<String> page = get("/api/v1/accounts/ACC-A/entries?page=0&size=2&sort=createdAt,desc");
        assertThat(page.statusCode()).isEqualTo(200);
        assertThat(page.body()).contains("\"content\":").contains("\"totalElements\":").contains("\"size\":2");

        HttpResponse<String> deprecated = post("/api/transfers",
                "{\"from\":\"ACC-A\",\"to\":\"ACC-B\",\"amount\":1.00}");
        assertThat(deprecated.statusCode()).isEqualTo(200);
        assertThat(deprecated.headers().firstValue("Deprecation")).hasValue("true");
    }

    @Test
    void adminPing_methodSecurity_overHttp() throws Exception {
        // @PreAuthorize("hasRole('ADMIN')"): a USER token is forbidden, an ADMIN token allowed.
        assertThat(getWithToken("/api/v1/admin/ping", USER_TOKEN).statusCode()).isEqualTo(403);
        HttpResponse<String> ok = getWithToken("/api/v1/admin/ping", ADMIN_TOKEN);
        assertThat(ok.statusCode()).isEqualTo(200);
        assertThat(ok.body()).contains("admin ok");
    }

    // ── helpers (default to a USER token) ──
    private HttpResponse<String> post(String path, String json) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create(base + path))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + USER_TOKEN)
                        .POST(HttpRequest.BodyPublishers.ofString(json)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postWithHeader(String path, String json, String name, String value)
            throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create(base + path))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + USER_TOKEN)
                        .header(name, value)
                        .POST(HttpRequest.BodyPublishers.ofString(json)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path) throws Exception {
        return getWithToken(path, USER_TOKEN);
    }

    private HttpResponse<String> getWithToken(String path, String token) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create(base + path))
                        .header("Authorization", "Bearer " + token).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }
}

// services/auth/src/test/java/com/buildabank/auth/AuthSecurityTest.java
package com.buildabank.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * End-to-end security over real HTTP: log in to get a JWT, then use it. Proves the filter chain enforces
 * <strong>authentication</strong> (no token → 401) and <strong>authorization</strong> (wrong role → 403),
 * that valid credentials mint a usable token, and that Spring Security's default security headers are set.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthSecurityTest {

    @LocalServerPort
    int port;

    private final HttpClient http = HttpClient.newHttpClient();
    private String base;

    @BeforeEach
    void setup() {
        base = "http://localhost:" + port;
    }

    @Test
    void login_withValidCredentials_returnsAToken() throws Exception {
        HttpResponse<String> response = login("alice", "password");
        assertThat(response.statusCode()).isEqualTo(200);
        String token = JsonPath.read(response.body(), "$.token");
        assertThat(token).isNotBlank().contains(".");   // a JWT has dot-separated parts
    }

    @Test
    void login_withWrongPassword_isRejected() throws Exception {
        assertThat(login("alice", "WRONG").statusCode()).isEqualTo(401);
    }

    @Test
    void me_withoutToken_is401() throws Exception {
        assertThat(get("/api/auth/me", null).statusCode()).isEqualTo(401);   // authentication required
    }

    @Test
    void me_withValidToken_returnsIdentity() throws Exception {
        String token = tokenFor("alice", "password");
        HttpResponse<String> me = get("/api/auth/me", token);
        assertThat(me.statusCode()).isEqualTo(200);
        assertThat((String) JsonPath.read(me.body(), "$.username")).isEqualTo("alice");
        assertThat(me.body()).contains("ROLE_USER");
    }

    @Test
    void admin_asNonAdmin_is403() throws Exception {
        String userToken = tokenFor("alice", "password");          // ROLE_USER only
        assertThat(get("/api/auth/admin", userToken).statusCode()).isEqualTo(403);   // authorization denied
    }

    @Test
    void admin_asAdmin_is200() throws Exception {
        String adminToken = tokenFor("admin", "admin123");         // ROLE_ADMIN
        assertThat(get("/api/auth/admin", adminToken).statusCode()).isEqualTo(200);
    }

    @Test
    void securityHeadersArePresent() throws Exception {
        HttpResponse<String> response = login("alice", "password");
        // Spring Security sets safe defaults on every response.
        assertThat(response.headers().firstValue("X-Content-Type-Options")).hasValue("nosniff");
    }

    // ── helpers ──
    private String tokenFor(String username, String password) throws Exception {
        return JsonPath.read(login(username, password).body(), "$.token");
    }

    private HttpResponse<String> login(String username, String password) throws Exception {
        return post("/api/auth/login",
                "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}");
    }

    private HttpResponse<String> post(String path, String json) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create(base + path))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path, String bearerToken) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(base + path)).GET();
        if (bearerToken != null) {
            builder.header("Authorization", "Bearer " + bearerToken);
        }
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}

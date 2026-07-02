// services/auth/src/test/java/com/buildabank/auth/RotationGraceTest.java
package com.buildabank.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * The benign-race half of reuse detection (default 3s grace). Two tabs share ONE refresh cookie; both fire
 * /refresh on session restore; the loser replays a just-consumed token. That must NOT nuke the session
 * (the strict half — replay-as-theft — is {@link RefreshFlowTest} with grace pinned to 0): the loser gets
 * 409 (retry with the rotated cookie you already hold), and the winner's successor keeps working.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RotationGraceTest {

    @LocalServerPort
    int port;

    private final HttpClient http = HttpClient.newHttpClient();
    private String base;

    @BeforeEach
    void setup() {
        base = "http://localhost:" + port;
    }

    @Test
    void replayWithinGrace_is409_andDoesNotRevokeTheFamily() throws Exception {
        String firstCookie = cookieValue(login());

        HttpResponse<String> winner = refresh(firstCookie);            // tab A rotates R0 → R1
        assertThat(winner.statusCode()).isEqualTo(200);
        String successor = cookieValue(winner);

        HttpResponse<String> loser = refresh(firstCookie);             // tab B replays R0 milliseconds later
        assertThat(loser.statusCode()).isEqualTo(409);                 // benign race — NOT theft

        assertThat(refresh(successor).statusCode()).isEqualTo(200);    // the family survived
    }

    // ── helpers (same wire-level style as RefreshFlowTest) ──
    private HttpResponse<String> login() throws Exception {
        return post("/api/auth/login", "{\"username\":\"alice\",\"password\":\"password\"}", null);
    }

    private HttpResponse<String> refresh(String refreshCookieValue) throws Exception {
        return post("/api/auth/refresh", "", refreshCookieValue);
    }

    private HttpResponse<String> post(String path, String body, String refreshCookieValue) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(base + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (refreshCookieValue != null) {
            builder.header("Cookie", "bab_refresh=" + refreshCookieValue);
        }
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String cookieValue(HttpResponse<String> response) {
        String header = response.headers().allValues("Set-Cookie").stream()
                .filter(c -> c.startsWith("bab_refresh=")).findFirst().orElseThrow();
        return header.substring("bab_refresh=".length(), header.indexOf(';'));
    }
}

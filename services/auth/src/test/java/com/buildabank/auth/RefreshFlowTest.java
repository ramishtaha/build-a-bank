// services/auth/src/test/java/com/buildabank/auth/RefreshFlowTest.java
package com.buildabank.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * The Step-32 session lifecycle over real HTTP: login plants a locked-down refresh cookie; /refresh rotates
 * it and mints a fresh access JWT; REUSING a consumed refresh token is detected and revokes the whole family
 * (both the replayed token AND its successor die); /logout revokes and clears the cookie. Cookies are parsed
 * by hand from Set-Cookie so every attribute we assert is the exact wire value.
 *
 * <p>Rotation grace is pinned to 0 here so "reuse" means STRICT reuse — the benign concurrent-tabs window
 * (409) is covered separately in {@link RotationGraceTest}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "bank.auth.rotation-grace-seconds=0")
class RefreshFlowTest {

    @LocalServerPort
    int port;

    private final HttpClient http = HttpClient.newHttpClient();
    private String base;

    @BeforeEach
    void setup() {
        base = "http://localhost:" + port;
    }

    @Test
    void login_setsHttpOnlyLockedDownRefreshCookie() throws Exception {
        HttpResponse<String> login = login("alice", "password");
        assertThat(login.statusCode()).isEqualTo(200);

        String setCookie = setCookieHeader(login).orElseThrow();
        assertThat(setCookie).startsWith("bab_refresh=");
        assertThat(setCookie).contains("HttpOnly");            // JS must never read this cookie
        assertThat(setCookie).contains("SameSite=Strict");     // never sent cross-site
        assertThat(setCookie).contains("Path=/api/auth");      // only rides to auth endpoints
    }

    @Test
    void refresh_rotatesTheCookie_andMintsAFreshAccessToken() throws Exception {
        HttpResponse<String> login = login("alice", "password");
        String firstCookie = cookieValue(login);

        HttpResponse<String> refreshed = refresh(firstCookie);
        assertThat(refreshed.statusCode()).isEqualTo(200);

        String newAccessToken = JsonPath.read(refreshed.body(), "$.token");
        assertThat(newAccessToken).isNotBlank().contains(".");         // a real JWT
        String secondCookie = cookieValue(refreshed);
        assertThat(secondCookie).isNotBlank().isNotEqualTo(firstCookie);   // ROTATED, not reissued
    }

    @Test
    void reusingAConsumedRefreshToken_revokesTheWholeFamily() throws Exception {
        String firstCookie = cookieValue(login("alice", "password"));
        String secondCookie = cookieValue(refresh(firstCookie));       // consumes firstCookie

        // Replay the consumed token (what a thief with a stolen old cookie does) → rejected…
        assertThat(refresh(firstCookie).statusCode()).isEqualTo(401);
        // …and the legitimate successor is dead too: the whole family was revoked (fail closed).
        assertThat(refresh(secondCookie).statusCode()).isEqualTo(401);
    }

    @Test
    void refresh_withoutACookie_is401() throws Exception {
        assertThat(refresh(null).statusCode()).isEqualTo(401);
    }

    @Test
    void logout_revokesTheSession_andClearsTheCookie() throws Exception {
        String cookie = cookieValue(login("alice", "password"));

        HttpResponse<String> logout = post("/api/auth/logout", "", cookie);
        assertThat(logout.statusCode()).isEqualTo(204);
        assertThat(setCookieHeader(logout).orElseThrow()).contains("Max-Age=0");   // browser deletes it

        assertThat(refresh(cookie).statusCode()).isEqualTo(401);       // and the server side is revoked
    }

    // ── helpers ──
    private HttpResponse<String> login(String username, String password) throws Exception {
        return post("/api/auth/login",
                "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}", null);
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

    private static Optional<String> setCookieHeader(HttpResponse<String> response) {
        List<String> cookies = response.headers().allValues("Set-Cookie");
        return cookies.stream().filter(c -> c.startsWith("bab_refresh=")).findFirst();
    }

    /** The bare cookie value from Set-Cookie: `bab_refresh=VALUE; Path=…; …` → `VALUE`. */
    private static String cookieValue(HttpResponse<String> response) {
        String header = setCookieHeader(response).orElseThrow();
        return header.substring("bab_refresh=".length(), header.indexOf(';'));
    }
}

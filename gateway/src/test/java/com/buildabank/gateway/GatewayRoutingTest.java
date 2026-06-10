// gateway/src/test/java/com/buildabank/gateway/GatewayRoutingTest.java
package com.buildabank.gateway;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Proves the gateway routes to a downstream service: it forwards a request, strips the route prefix, and
 * applies the response-header filter — verified against an in-test stub HTTP server (no real services
 * needed). The route's target URI is pointed at the stub via {@code @DynamicPropertySource}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutingTest {

    private static HttpServer stub;
    private static final AtomicReference<String> receivedPath = new AtomicReference<>();

    @LocalServerPort
    int gatewayPort;

    private final HttpClient http = HttpClient.newHttpClient();

    @DynamicPropertySource
    static void downstream(DynamicPropertyRegistry registry) {
        try {
            stub = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        stub.createContext("/", exchange -> {
            receivedPath.set(exchange.getRequestURI().getPath());   // record what the downstream actually got
            byte[] body = "{\"ok\":true}".getBytes(UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        stub.start();
        String stubUri = "http://localhost:" + stub.getAddress().getPort();
        registry.add("services.cif.uri", () -> stubUri);
        registry.add("services.demand-account.uri", () -> stubUri);
        registry.add("services.auth.uri", () -> stubUri);                                  // Step 29: auth route target
        registry.add("services.notification.uri", () -> stubUri);                          // Step 30: notification (SSE) target
        registry.add("app.security.cors.allowed-origins", () -> "http://localhost:5173");  // Step 29: allowed dev origin
    }

    @AfterAll
    static void stopStub() {
        if (stub != null) {
            stub.stop(0);
        }
    }

    @Test
    void routesToDownstream_stripsPrefix_andAddsGatewayHeader() throws Exception {
        HttpResponse<String> response = http.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + gatewayPort + "/cif/api/customers/1"))
                        .GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"ok\":true");                       // downstream's body returned
        assertThat(receivedPath.get()).isEqualTo("/api/customers/1");               // StripPrefix removed "/cif"
        assertThat(response.headers().firstValue("X-Gateway")).hasValue("build-a-bank");   // gateway filter ran
    }

    @Test
    void routesAuthWithoutStrippingPrefix() throws Exception {
        // Step 29: the React app calls the gateway for login; auth's paths already start with /api/auth,
        // so the auth route does NOT strip — the downstream receives the path unchanged.
        HttpResponse<String> response = http.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + gatewayPort + "/api/auth/me"))
                        .GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(receivedPath.get()).isEqualTo("/api/auth/me");                   // NOT stripped
        assertThat(response.headers().firstValue("X-Gateway")).hasValue("build-a-bank");
    }

    @Test
    void routesNotificationStreamStrippingPrefix() throws Exception {
        // Step 30: the SPA's SSE client subscribes through the gateway; /notifications is stripped so the
        // notification service receives its own /api/notifications/stream path.
        HttpResponse<String> response = http.send(
                HttpRequest.newBuilder(
                                URI.create("http://localhost:" + gatewayPort + "/notifications/api/notifications/stream"))
                        .GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(receivedPath.get()).isEqualTo("/api/notifications/stream");       // StripPrefix removed "/notifications"
        assertThat(response.headers().firstValue("X-Gateway")).hasValue("build-a-bank");
    }

    @Test
    void corsPreflightFromTheAllowedOriginIsAllowed() throws Exception {
        // A browser preflight: OPTIONS + Origin + Access-Control-Request-Method. The gateway's CorsFilter
        // answers it directly with the matching Access-Control-Allow-Origin (the route is never reached).
        HttpResponse<String> preflight = http.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + gatewayPort + "/api/auth/login"))
                        .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(preflight.statusCode()).isEqualTo(200);
        assertThat(preflight.headers().firstValue("Access-Control-Allow-Origin"))
                .hasValue("http://localhost:5173");
    }

    @Test
    void corsPreflightFromADisallowedOriginIsRejected() throws Exception {
        // deny-by-default: an origin not on the allow-list gets no Access-Control-Allow-Origin (browser blocks it).
        HttpResponse<String> preflight = http.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + gatewayPort + "/api/auth/login"))
                        .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                        .header("Origin", "http://evil.example")
                        .header("Access-Control-Request-Method", "POST")
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(preflight.statusCode()).isEqualTo(403);
        assertThat(preflight.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
    }
}

// playground/java-basics/src/test/java/com/buildabank/basics/net/LoopbackHttpTest.java
package com.buildabank.basics.net;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Exercises {@link HttpClientDemo} and {@link RawHttpDemo} against a real, self-contained loopback server
 * — the JDK's built-in {@link HttpServer} on an ephemeral port. No Docker, no external host: a deterministic
 * end-to-end HTTP round trip on localhost.
 */
class LoopbackHttpTest {

    private HttpServer server;
    private int port;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);  // port 0 → OS picks a free port
        server.createContext("/api/hello", exchange -> {
            byte[] body = "{\"message\":\"hi\",\"service\":\"hello\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
        port = server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void httpClientGetsTwoHundredAndJsonBody() throws Exception {
        var response = HttpClientDemo.get("http://localhost:" + port + "/api/hello");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("content-type")).contains("application/json");
        assertThat(response.body()).contains("\"service\":\"hello\"");
    }

    @Test
    void rawSocketSeesTheStatusLineAndBody() throws Exception {
        String raw = RawHttpDemo.fetch("localhost", port, "/api/hello");
        assertThat(raw).startsWith("HTTP/1.1 200");
        // HTTP header NAMES are case-insensitive (RFC 9110). The JDK HttpServer emits "Content-type",
        // so we must not assert exact case — a real lesson about the protocol.
        assertThat(raw).containsIgnoringCase("content-type: application/json");
        assertThat(raw).contains("\"message\":\"hi\"");
    }
}

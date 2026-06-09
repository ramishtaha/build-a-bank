// services/demand-account/src/test/java/com/buildabank/account/client/CifClientTest.java
package com.buildabank.account.client;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.InetSocketAddress;
import java.time.Duration;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

/**
 * Tests the declarative {@link CifClient} against an in-test stub HTTP server (no Spring context, no real
 * CIF): a normal call deserializes the JSON into a {@link CifCustomer}, and a slow dependency trips the
 * <strong>read timeout</strong> (fail fast) instead of hanging.
 */
class CifClientTest {

    private HttpServer stub;
    private String baseUrl;

    @BeforeEach
    void startStub() throws Exception {
        stub = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        stub.createContext("/api/customers/by-number/", exchange -> {
            if (exchange.getRequestURI().getPath().endsWith("/slow")) {
                try {
                    Thread.sleep(1500);   // longer than the client's read timeout → should time out
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            byte[] body = ("{\"customerNumber\":\"CIF-1\",\"firstName\":\"Ada\","
                    + "\"lastName\":\"Lovelace\",\"kycStatus\":\"VERIFIED\"}").getBytes(UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        stub.start();
        baseUrl = "http://localhost:" + stub.getAddress().getPort();
    }

    @AfterEach
    void stopStub() {
        stub.stop(0);
    }

    @Test
    void deserializesTheCustomer() {
        CifClient client = CifClientFactory.create(baseUrl, Duration.ofMillis(500), Duration.ofMillis(500));

        CifCustomer customer = client.getByNumber("CIF-1");

        assertThat(customer.customerNumber()).isEqualTo("CIF-1");
        assertThat(customer.firstName()).isEqualTo("Ada");
        assertThat(customer.kycStatus()).isEqualTo("VERIFIED");
    }

    @Test
    void failsFastWhenTheDependencyIsSlow() {
        CifClient client = CifClientFactory.create(baseUrl, Duration.ofMillis(500), Duration.ofMillis(400));

        // The stub sleeps 1.5s for "/slow"; the 400ms read timeout fires first → a transport exception, not a hang.
        assertThatThrownBy(() -> client.getByNumber("slow"))
                .isInstanceOf(ResourceAccessException.class);
    }
}

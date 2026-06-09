// services/hello/src/test/java/com/buildabank/hello/HelloApplicationTests.java
package com.buildabank.hello;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

/**
 * Two tests that actually prove the app works (not just compiles):
 *  1. the Spring context starts cleanly, and
 *  2. GET /api/hello returns 200 with the welcome message.
 *
 * <p>webEnvironment = RANDOM_PORT boots a real embedded server on a free port so we exercise
 * the full HTTP path, exactly as a client would.
 *
 * <p>NOTE (Spring Boot 4 / Framework 7): the classic {@code TestRestTemplate} was REMOVED in
 * Boot 4. Here we use {@link RestClient} (the course's standard synchronous HTTP client) pointed
 * at the live server. The dedicated Boot 4 test clients — {@code RestTestClient} and
 * {@code MockMvcTester} — are introduced in Steps 13 &amp; 28. (See this step's "Then vs Now".)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HelloApplicationTests {

    @LocalServerPort
    int port;

    @Test
    void contextLoads() {
        // If the application context fails to start, this test fails — the cheapest smoke test there is.
    }

    @Test
    void helloEndpointReturnsWelcome() {
        RestClient client = RestClient.create();

        ResponseEntity<String> response = client.get()
                .uri("http://localhost:{port}/api/hello", port)
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("Welcome to Build-a-Bank");
        assertThat(response.getBody()).contains("\"service\":\"hello\"");
    }
}

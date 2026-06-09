// services/hello/src/main/java/com/buildabank/hello/HelloController.java
package com.buildabank.hello;

import java.time.Instant;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The first HTTP surface your bank exposes.
 *
 * <p>{@code @RestController} = {@code @Controller} + {@code @ResponseBody}: return values are
 * serialized straight to the HTTP response body as JSON (via a Jackson message converter),
 * not resolved to a view template. The request lifecycle is dissected in Step 13.
 */
@RestController
public class HelloController {

    /**
     * GET /api/hello — returns a small JSON greeting and the server time (UTC).
     * Time is always {@link Instant} (UTC) in this codebase; money is always BigDecimal.
     */
    @GetMapping("/api/hello")
    public Map<String, Object> hello() {
        return Map.of(
                "message", "Welcome to Build-a-Bank 🏦",
                "service", "hello",
                "timestamp", Instant.now().toString()
        );
    }
}

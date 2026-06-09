// services/demand-account/src/main/java/com/buildabank/account/web/RequestIdFilter.java
package com.buildabank.account.web;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * A servlet {@code Filter} (via Spring's {@link OncePerRequestFilter}) that gives every request a
 * <strong>correlation id</strong>: it reuses an inbound {@code X-Request-Id} or mints one, and echoes it on
 * the response. Filters run at the <em>servlet-container</em> level — before the {@code DispatcherServlet},
 * around the entire request/response, even for requests that never reach a Spring handler — which is exactly
 * why cross-cutting concerns like correlation ids, CORS, and compression live here (vs. interceptors, which
 * are Spring-MVC-level and have handler context). We build on this for distributed tracing in Step 36.
 */
@Component
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = request.getHeader(HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        response.setHeader(HEADER, requestId);   // set BEFORE the chain runs, so it survives even on errors
        chain.doFilter(request, response);
    }
}

// services/demand-account/src/main/java/com/buildabank/account/web/TimingInterceptor.java
package com.buildabank.account.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * A Spring MVC {@link HandlerInterceptor} that times handler execution. Interceptors run <em>inside</em> the
 * {@code DispatcherServlet}, around the matched handler, with three hooks: {@code preHandle} (before the
 * handler — return false to short-circuit), {@code postHandle} (after the handler, before the view/body is
 * committed), and {@code afterCompletion} (always, even on exception — the right place for timing/cleanup).
 *
 * <p>Contrast with a {@code Filter} (see {@link RequestIdFilter}): the filter is at the servlet-container
 * level and wraps everything; an interceptor is Spring-MVC-level and knows which <em>handler</em> matched.
 * We set a marker header in {@code preHandle} (reliably, before the response commits) and log the elapsed
 * time in {@code afterCompletion}.
 */
@Component
public class TimingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TimingInterceptor.class);
    private static final String START_ATTR = "timing.startNanos";
    public static final String HEADER = "X-Timing-Enabled";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_ATTR, System.nanoTime());
        response.setHeader(HEADER, "true");   // set before the response is committed → reliably visible
        return true;                          // proceed to the handler
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Object start = request.getAttribute(START_ATTR);
        if (start instanceof Long startNanos) {
            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
            log.info("{} {} -> {} in {} ms", request.getMethod(), request.getRequestURI(), response.getStatus(), elapsedMs);
        }
    }
}

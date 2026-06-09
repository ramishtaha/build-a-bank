// services/demand-account/src/main/java/com/buildabank/account/web/WebConfig.java
package com.buildabank.account.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers the {@link TimingInterceptor} with Spring MVC. ({@code @Component} alone makes it a bean but does
 * NOT wire it into the handler chain — you must add it here via {@link WebMvcConfigurer}. Filters, by
 * contrast, are auto-registered when they're beans.)
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final TimingInterceptor timingInterceptor;

    public WebConfig(TimingInterceptor timingInterceptor) {
        this.timingInterceptor = timingInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(timingInterceptor);
    }
}

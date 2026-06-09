// services/demand-account/src/main/java/com/buildabank/account/web/OpenApiConfig.java
package com.buildabank.account.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/**
 * OpenAPI metadata for the generated docs. springdoc auto-generates the spec from the controllers and
 * serves it at {@code /v3/api-docs} (JSON) and a browsable Swagger UI at {@code /swagger-ui.html} — the
 * bank's first "click and see it" surface.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI demandAccountOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Demand Account API")
                .version("v1")
                .description("Accounts and double-entry ledger transfers (Build-a-Bank). "
                        + "Errors are RFC 9457 application/problem+json."));
    }
}

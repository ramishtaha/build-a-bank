// services/demand-account/src/test/java/com/buildabank/account/web/SecurityHardeningTest.java
package com.buildabank.account.web;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.buildabank.account.domain.Account;
import com.buildabank.account.service.IdempotentTransferService;
import com.buildabank.account.service.TransferService;
import com.buildabank.account.webhook.WebhookPublisher;

/**
 * Step 18 — proves the <strong>secure-by-default edge hardening</strong> wired into {@link SecurityConfig}:
 * security response headers on every response, and a <strong>deny-by-default CORS</strong> policy that rejects
 * a browser preflight from an un-listed origin. Web-layer slice (no DB); services are mocked, and the
 * {@code jwt()} post-processor supplies authentication where a request needs to reach the controller.
 */
@WebMvcTest(TransferController.class)
@Import(SecurityConfig.class)
class SecurityHardeningTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    TransferService transfers;

    @MockitoBean
    IdempotentTransferService idempotentTransfers;

    @MockitoBean
    WebhookPublisher webhookPublisher;

    // The resource-server config needs a JwtDecoder bean to start; jwt() bypasses real decoding.
    @MockitoBean
    JwtDecoder jwtDecoder;

    @Test
    void everyResponseCarriesHardenedSecurityHeaders() throws Exception {
        // Step 32: the balance endpoint now maps the whole entity (accountOf), not just balanceOf.
        given(transfers.accountOf(eq("ACC-A")))
                .willReturn(new Account("ACC-A", "USD", new BigDecimal("10.00"), Instant.now()));

        mvc.perform(get("/api/accounts/ACC-A")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))   // no MIME sniffing
                .andExpect(header().string("X-Frame-Options", "DENY"))             // no framing (clickjacking)
                .andExpect(header().string("Referrer-Policy", "no-referrer"));     // don't leak URLs cross-site
    }

    @Test
    void crossOriginPreflightFromAnUnlistedOriginIsRejected() throws Exception {
        // A browser preflight (OPTIONS + Origin + Access-Control-Request-Method) from evil.example.
        mvc.perform(options("/api/v1/transfers")
                        .header("Origin", "https://evil.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())                                 // deny-by-default CORS
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));  // origin NOT reflected
    }

    @Test
    void unauthenticatedMoneyRequestIs401() throws Exception {
        // Authentication is still the real gate — CORS/headers are defense-in-depth, not access control.
        mvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"from":"ACC-A","to":"ACC-B","amount":1.00}
                                """))
                .andExpect(status().isUnauthorized());
    }
}

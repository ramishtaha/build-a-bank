// services/demand-account/src/test/java/com/buildabank/account/web/TransferControllerTest.java
package com.buildabank.account.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.buildabank.account.domain.Account;
import com.buildabank.account.domain.InsufficientFundsException;
import com.buildabank.account.service.IdempotentTransferService;
import com.buildabank.account.service.TransferService;
import com.buildabank.account.webhook.WebhookPublisher;

/**
 * Web-layer slice: controller + advice + security + MVC infra (no DB). Services are Mockito mocks. Since
 * Step 17 the endpoints require a JWT, so requests carry one via {@code spring-security-test}'s {@code jwt()}
 * post-processor (which injects the authentication directly — no real token decoding needed in the slice).
 */
@WebMvcTest(TransferController.class)
@Import(SecurityConfig.class)
class TransferControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    TransferService transfers;

    @MockitoBean
    IdempotentTransferService idempotentTransfers;

    @MockitoBean
    WebhookPublisher webhookPublisher;

    // The resource-server config needs a JwtDecoder bean to start; jwt() below bypasses it, so a mock is fine.
    @MockitoBean
    JwtDecoder jwtDecoder;

    private static JwtRequestPostProcessor user() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Test
    void openReturns201() throws Exception {
        given(transfers.openAccount(eq("ACC-A"), eq("USD"), any()))
                .willReturn(new Account("ACC-A", "USD", new BigDecimal("100.00"), Instant.now()));

        mvc.perform(post("/api/accounts").with(user())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountNumber":"ACC-A","currency":"USD","openingBalance":100.00}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountNumber").value("ACC-A"))
                .andExpect(jsonPath("$.balance").value(100.00));
    }

    @Test
    void transferReturns200WithTransactionId() throws Exception {
        UUID txId = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
        given(transfers.transfer(eq("ACC-A"), eq("ACC-B"), any(), any())).willReturn(txId);

        mvc.perform(post("/api/transfers").with(user())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"from":"ACC-A","to":"ACC-B","amount":25.00,"description":"rent"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(txId.toString()));
    }

    @Test
    void unauthenticatedRequestIs401() throws Exception {
        // No jwt() → the resource-server filter chain rejects before the controller (Step 17).
        mvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"from":"ACC-A","to":"ACC-B","amount":25.00}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void overdrawReturnsProblemDetail422() throws Exception {
        given(transfers.transfer(any(), any(), any(), any()))
                .willThrow(new InsufficientFundsException("balance too low"));

        mvc.perform(post("/api/transfers").with(user())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"from":"ACC-A","to":"ACC-B","amount":9999.00}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))   // RFC 9457
                .andExpect(jsonPath("$.title").value("Insufficient funds"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.detail").value("balance too low"))
                .andExpect(jsonPath("$.type").value("https://buildabank.example/problems/insufficient-funds"));
    }

    @Test
    void negativeAmountReturnsValidationProblemDetail400() throws Exception {
        // @Positive on the amount fails Bean Validation before the controller body runs.
        mvc.perform(post("/api/transfers").with(user())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"from":"ACC-A","to":"ACC-B","amount":-5.00}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.amount").exists());   // per-field error attached
    }

    @Test
    void deprecatedTransferAdvertisesSuccessor() throws Exception {
        given(transfers.transfer(any(), any(), any(), any())).willReturn(UUID.randomUUID());

        mvc.perform(post("/api/transfers").with(user())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"from":"ACC-A","to":"ACC-B","amount":25.00}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("Deprecation", "true"))
                .andExpect(header().exists("Sunset"))
                .andExpect(header().string("Link", "</api/v1/transfers>; rel=\"successor-version\""));
    }

    @Test
    void v1TransferPassesTheIdempotencyKey() throws Exception {
        UUID txId = UUID.fromString("00000000-0000-0000-0000-0000000000cc");
        given(idempotentTransfers.transfer(eq("KEY-1"), eq("ACC-A"), eq("ACC-B"), any(), any()))
                .willReturn(txId);

        mvc.perform(post("/api/v1/transfers").with(user())
                        .header("Idempotency-Key", "KEY-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"from":"ACC-A","to":"ACC-B","amount":25.00}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(txId.toString()));
    }

    @Test
    void responseCarriesTheCorrelationIdHeader() throws Exception {
        UUID txId = UUID.fromString("00000000-0000-0000-0000-0000000000bb");
        given(transfers.transfer(any(), any(), any(), any())).willReturn(txId);

        mvc.perform(post("/api/transfers").with(user())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"from":"ACC-A","to":"ACC-B","amount":25.00}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))        // set by RequestIdFilter
                .andExpect(header().string("X-Timing-Enabled", "true"));   // set by TimingInterceptor.preHandle
    }

    @Test
    void adminPing_methodSecurity_enforcesRole() throws Exception {
        // @PreAuthorize("hasRole('ADMIN')") — a USER token is forbidden, an ADMIN token allowed.
        mvc.perform(get("/api/v1/admin/ping").with(user()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/ping").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("admin ok"));
    }
}

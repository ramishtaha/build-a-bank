// services/demand-account/src/test/java/com/buildabank/account/web/PaymentControllerTest.java
package com.buildabank.account.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.buildabank.account.payment.PaymentService;

/**
 * Step 21 · web-layer slice for the payments API. Confirms a payment returns the id, that the
 * {@code Idempotency-Key} header is passed through to the service, and that the endpoint is secured (Step 17).
 */
@WebMvcTest(PaymentController.class)
@Import(SecurityConfig.class)
class PaymentControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    PaymentService payments;

    @MockitoBean
    JwtDecoder jwtDecoder;   // resource-server config needs the bean to start; jwt() bypasses real decoding

    @Test
    void payReturnsPaymentId_andForwardsTheIdempotencyKey() throws Exception {
        UUID paymentId = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
        given(payments.pay(eq("ACC-A"), eq("ACC-B"), any(), eq("KEY-1"))).willReturn(paymentId);

        mvc.perform(post("/api/v1/payments")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .header("Idempotency-Key", "KEY-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"from":"ACC-A","to":"ACC-B","amount":40.00}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(paymentId.toString()));

        verify(payments).pay(eq("ACC-A"), eq("ACC-B"), any(), eq("KEY-1"));   // the Idempotency-Key is forwarded
    }

    @Test
    void unauthenticatedPaymentIs401() throws Exception {
        mvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"from":"ACC-A","to":"ACC-B","amount":40.00}
                                """))
                .andExpect(status().isUnauthorized());
    }
}

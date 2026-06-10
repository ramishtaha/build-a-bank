// services/onboarding/src/test/java/com/buildabank/onboarding/web/OnboardingControllerTest.java
package com.buildabank.onboarding.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.buildabank.onboarding.service.OnboardingService;

/**
 * Step 23 · web-layer slice for the onboarding API. The orchestrator is mocked; confirms a successful
 * onboarding returns 201 with the result, the bearer token is forwarded to the service, and an invalid
 * request is rejected by Bean Validation (400).
 */
@WebMvcTest(OnboardingController.class)
class OnboardingControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    OnboardingService onboarding;

    @Test
    void onboardReturns201WithResult_andForwardsTheToken() throws Exception {
        given(onboarding.onboard(any(), eq("Bearer t")))
                .willReturn(new OnboardingResult("CIF-1", 1L, "DDA-CIF-1", "ONBOARDED"));

        mvc.perform(post("/api/onboarding")
                        .header("Authorization", "Bearer t")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Ada","lastName":"Lovelace","email":"ada@bank.example","dateOfBirth":"1990-05-17","currency":"USD"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerNumber").value("CIF-1"))
                .andExpect(jsonPath("$.accountNumber").value("DDA-CIF-1"))
                .andExpect(jsonPath("$.status").value("ONBOARDED"));
    }

    @Test
    void invalidRequestReturns400() throws Exception {
        // Blank names + bad email → Bean Validation rejects before the controller body runs.
        mvc.perform(post("/api/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"","lastName":"","email":"not-an-email","dateOfBirth":"","currency":""}
                                """))
                .andExpect(status().isBadRequest());
    }
}

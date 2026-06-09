// services/cif/src/test/java/com/buildabank/cif/web/CustomerControllerTest.java
package com.buildabank.cif.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.buildabank.cif.domain.Customer;
import com.buildabank.cif.domain.KycStatus;
import com.buildabank.cif.service.CustomerService;

/**
 * Web-layer slice: only the controller + MVC infrastructure load (fast, no DB). The service is a Mockito
 * mock (via {@code @MockitoBean} — the Spring Framework replacement for {@code @MockBean}).
 */
@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    CustomerService service;

    @Test
    void createReturns201WithBody() throws Exception {
        var saved = new Customer("CIF-AB12CD34", "Ada", "Lovelace", "ada@bank.example",
                LocalDate.of(1990, 5, 17), KycStatus.PENDING, Instant.parse("2026-06-09T00:00:00Z"));
        given(service.create(any(), any(), any(), any())).willReturn(saved);

        mvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Ada","lastName":"Lovelace","email":"ada@bank.example","dateOfBirth":"1990-05-17"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerNumber").value("CIF-AB12CD34"))
                .andExpect(jsonPath("$.kycStatus").value("PENDING"));
    }

    @Test
    void invalidBodyReturns400() throws Exception {
        // Blank first name + malformed email → Bean Validation rejects before the controller runs.
        mvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"","lastName":"Lovelace","email":"not-an-email","dateOfBirth":"1990-05-17"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingCustomerReturns404() throws Exception {
        given(service.findById(99L)).willReturn(Optional.empty());
        mvc.perform(get("/api/customers/99")).andExpect(status().isNotFound());
    }
}

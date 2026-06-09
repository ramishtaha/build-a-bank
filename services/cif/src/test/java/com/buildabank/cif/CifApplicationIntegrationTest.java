// services/cif/src/test/java/com/buildabank/cif/CifApplicationIntegrationTest.java
package com.buildabank.cif;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Full end-to-end on a REAL Postgres (Testcontainers): the whole context boots, Flyway migrates, then we
 * POST a customer and GET it back through the actual HTTP stack — the honest "it really works" proof.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ContainersConfig.class)
class CifApplicationIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Test
    void createsThenFetchesACustomer() throws Exception {
        MvcResult created = mvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Grace","lastName":"Hopper","email":"grace@bank.example","dateOfBirth":"1906-12-09"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.kycStatus").value("PENDING"))
                .andExpect(jsonPath("$.customerNumber").exists())
                .andReturn();

        String number = JsonPath.read(created.getResponse().getContentAsString(), "$.customerNumber");

        mvc.perform(get("/api/customers/by-number/" + number))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("grace@bank.example"))
                .andExpect(jsonPath("$.firstName").value("Grace"));
    }
}

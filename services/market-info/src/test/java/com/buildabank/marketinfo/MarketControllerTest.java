// services/market-info/src/test/java/com/buildabank/marketinfo/MarketControllerTest.java
package com.buildabank.marketinfo;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Step 22 · the market read API. Standalone MockMvc around just the controller with a mocked service — no
 * Spring context (so no Redis/cache wiring needed to test a thin web layer). Confirms the path variables are
 * upper-cased before the lookup and the rate is returned as JSON.
 */
class MarketControllerTest {

    private final MarketRateService rates = mock(MarketRateService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new MarketController(rates)).build();

    @Test
    void returnsTheRateForAPair() throws Exception {
        given(rates.getRate("USD", "EUR")).willReturn(new FxRate("USD", "EUR", new BigDecimal("0.92"), 123L));

        mvc.perform(get("/api/market/rates/usd/eur"))           // lower-case in the path...
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.base").value("USD"))     // ...upper-cased before the lookup
                .andExpect(jsonPath("$.quote").value("EUR"))
                .andExpect(jsonPath("$.rate").value(0.92));
    }
}

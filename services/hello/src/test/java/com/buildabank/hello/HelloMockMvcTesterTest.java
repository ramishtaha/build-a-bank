// services/hello/src/test/java/com/buildabank/hello/HelloMockMvcTesterTest.java
package com.buildabank.hello;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Step 28 · testing mastery — a <strong>web slice</strong> ({@link WebMvcTest} loads only the MVC layer for
 * {@link HelloController}, not the whole app, so it's fast) tested with Spring Framework 7's
 * <strong>{@link MockMvcTester}</strong>: the AssertJ-fluent successor to the old {@code MockMvc.perform(...)}
 * chains (and to Boot 4's removed {@code TestRestTemplate}). This is the API the Step-1 test's "Then vs Now"
 * note pointed forward to.
 */
@WebMvcTest(HelloController.class)
class HelloMockMvcTesterTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    void getHelloReturns200WithJsonGreeting() {
        assertThat(mvc.get().uri("/api/hello"))
                .hasStatusOk()
                .bodyText()
                .contains("Welcome to Build-a-Bank", "\"service\":\"hello\"");
    }
}

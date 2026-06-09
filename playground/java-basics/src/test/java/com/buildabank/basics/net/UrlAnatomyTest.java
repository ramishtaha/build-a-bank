// playground/java-basics/src/test/java/com/buildabank/basics/net/UrlAnatomyTest.java
package com.buildabank.basics.net;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UrlAnatomyTest {

    @Test
    void parsesAllComponentsOfAnHttpsUrl() {
        var a = UrlAnatomy.of("https://api.bank.example/accounts?limit=10");
        assertThat(a.scheme()).isEqualTo("https");
        assertThat(a.host()).isEqualTo("api.bank.example");
        assertThat(a.port()).isEqualTo(443);          // default for https
        assertThat(a.path()).isEqualTo("/accounts");
        assertThat(a.query()).isEqualTo("limit=10");
        assertThat(a.isSecure()).isTrue();
    }

    @Test
    void usesExplicitPortAndHttpDefault() {
        assertThat(UrlAnatomy.of("http://localhost:8080/api/hello").port()).isEqualTo(8080);
        assertThat(UrlAnatomy.of("http://localhost/api/hello").port()).isEqualTo(80);
        assertThat(UrlAnatomy.of("http://localhost").path()).isEqualTo("/");
    }
}

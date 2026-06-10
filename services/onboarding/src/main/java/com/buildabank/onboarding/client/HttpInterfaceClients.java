// services/onboarding/src/main/java/com/buildabank/onboarding/client/HttpInterfaceClients.java
package com.buildabank.onboarding.client;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Builds any {@code @HttpExchange} interface client from a {@link RestClient} with explicit connect + read
 * timeouts (a service call must fail fast, never hang on a slow dependency — Step 15). Static so the Spring
 * config and the tests build clients the same way.
 */
public final class HttpInterfaceClients {

    private HttpInterfaceClients() {
    }

    public static <T> T create(Class<T> clientType, String baseUrl, Duration connectTimeout, Duration readTimeout) {
        HttpClient jdk = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(jdk);
        requestFactory.setReadTimeout(readTimeout);

        RestClient restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();

        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(clientType);
    }
}

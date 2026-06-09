// services/demand-account/src/main/java/com/buildabank/account/client/CifClientFactory.java
package com.buildabank.account.client;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Builds a {@link CifClient} from a {@link RestClient} with explicit <strong>connect and read timeouts</strong>
 * — a service-to-service call must never hang forever on a slow dependency (that's how one slow service takes
 * the whole system down). A static factory so both the Spring config and the tests build it the same way.
 */
public final class CifClientFactory {

    private CifClientFactory() {
    }

    public static CifClient create(String baseUrl, Duration connectTimeout, Duration readTimeout) {
        HttpClient jdk = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(jdk);
        requestFactory.setReadTimeout(readTimeout);   // a slow response → fail fast instead of hanging

        RestClient restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();

        HttpServiceProxyFactory proxyFactory = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build();
        return proxyFactory.createClient(CifClient.class);
    }
}

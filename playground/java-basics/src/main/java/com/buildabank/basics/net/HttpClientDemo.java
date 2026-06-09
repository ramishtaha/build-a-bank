// playground/java-basics/src/main/java/com/buildabank/basics/net/HttpClientDemo.java
package com.buildabank.basics.net;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * The modern Java HTTP client ({@code java.net.http}, Java 11+) — a request/response round trip.
 *
 * <p>This is the "client" half of the web: build a request (method + URL + headers), send it over a
 * TCP connection (TLS first if https), and read the status line + headers + body back. We will use the
 * Spring {@code RestClient} (a friendlier wrapper) from Step 15; here we use the raw JDK client so the
 * request/response anatomy is explicit.
 */
public final class HttpClientDemo {

    private HttpClientDemo() { }

    public static HttpResponse<String> get(String url) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public static void main(String[] args) throws Exception {
        String url = (args.length > 0) ? args[0] : "http://localhost:8080/api/hello";
        HttpResponse<String> response = get(url);
        System.out.println("GET " + url);
        System.out.println("HTTP version : " + response.version());                       // HTTP_1_1 / HTTP_2
        System.out.println("Status       : " + response.statusCode());                    // 200, 404, ...
        System.out.println("Content-Type : " + response.headers().firstValue("content-type").orElse("(none)"));
        System.out.println("Body         : " + response.body());
    }
}

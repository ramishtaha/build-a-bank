// playground/java-basics/src/main/java/com/buildabank/basics/net/UrlAnatomy.java
package com.buildabank.basics.net;

import java.net.URI;

/**
 * "What happens when you type a URL?" — part 1: the URL is parsed into pieces.
 *
 * <p>A record capturing the components a browser/client extracts from a URL before it can do anything:
 * the <b>scheme</b> (http/https → which default port + whether TLS), the <b>host</b> (resolved via DNS to
 * an IP), the <b>port</b> (explicit, or the scheme default: 80 for http, 443 for https), the <b>path</b>,
 * and the <b>query</b>. We parse with {@link URI} (the JDK's RFC-3986 parser).
 */
public record UrlAnatomy(String scheme, String host, int port, String path, String query) {

    public static UrlAnatomy of(String url) {
        URI u = URI.create(url);
        int port = (u.getPort() != -1) ? u.getPort() : defaultPort(u.getScheme());
        String path = (u.getPath() == null || u.getPath().isBlank()) ? "/" : u.getPath();
        return new UrlAnatomy(u.getScheme(), u.getHost(), port, path, u.getQuery());
    }

    private static int defaultPort(String scheme) {
        return "https".equalsIgnoreCase(scheme) ? 443 : 80;
    }

    public boolean isSecure() {
        return "https".equalsIgnoreCase(scheme);
    }
}

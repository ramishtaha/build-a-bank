// playground/java-basics/src/main/java/com/buildabank/basics/net/RawHttpDemo.java
package com.buildabank.basics.net;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * HTTP <strong>on the wire</strong> — there is no magic. We open a raw TCP {@link Socket}, type an
 * HTTP/1.1 request as plain text, and read the raw bytes the server sends back (status line + headers +
 * blank line + body). This is exactly what {@code curl} and your browser do underneath.
 *
 * <p>Note the protocol details: lines end with CRLF ({@code \r\n}), a blank line separates headers from
 * body, and {@code Connection: close} tells the server to close the socket when done (so our read ends).
 */
public final class RawHttpDemo {

    private RawHttpDemo() { }

    public static String fetch(String host, int port, String path) throws IOException {
        try (Socket socket = new Socket(host, port)) {
            OutputStream out = socket.getOutputStream();
            String request = "GET " + path + " HTTP/1.1\r\n"
                    + "Host: " + host + "\r\n"
                    + "Accept: application/json\r\n"
                    + "Connection: close\r\n"
                    + "\r\n";
            out.write(request.getBytes(StandardCharsets.US_ASCII));
            out.flush();
            // Read everything until the server closes the connection.
            return new String(socket.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public static void main(String[] args) throws Exception {
        String host = (args.length > 0) ? args[0] : "localhost";
        int port = (args.length > 1) ? Integer.parseInt(args[1]) : 8080;
        String path = (args.length > 2) ? args[2] : "/api/hello";
        System.out.println("--- raw HTTP response from " + host + ":" + port + path + " ---");
        System.out.println(fetch(host, port, path));
    }
}

// services/hello/src/main/java/com/buildabank/hello/HelloApplication.java
package com.buildabank.hello;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The entry point of your first Build-a-Bank service.
 *
 * <p>{@code @SpringBootApplication} is three annotations in one:
 * <ul>
 *   <li>{@code @SpringBootConfiguration} — marks this as a source of bean definitions;</li>
 *   <li>{@code @EnableAutoConfiguration} — turns on Spring Boot's "look at the classpath and
 *       configure sensible defaults" machinery (this is what wires up an embedded web server);</li>
 *   <li>{@code @ComponentScan} — scans this package and below for {@code @Component}/{@code @RestController} beans.</li>
 * </ul>
 * We unpack each of these "magic" pieces in Steps 5–7.
 */
@SpringBootApplication
public class HelloApplication {

    public static void main(String[] args) {
        // Boots the Spring context, starts embedded Tomcat, and blocks until shutdown.
        SpringApplication.run(HelloApplication.class, args);
    }
}

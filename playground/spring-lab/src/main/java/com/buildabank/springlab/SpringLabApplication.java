// playground/spring-lab/src/main/java/com/buildabank/springlab/SpringLabApplication.java
package com.buildabank.springlab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * A non-web Spring Boot application used to explore the IoC container itself.
 *
 * <p>When {@code SpringApplication.run} executes, Spring creates an {@code ApplicationContext},
 * scans this package for beans ({@code @Component}/{@code @Service}/{@code @Configuration}), resolves
 * their dependencies (DI), runs lifecycle callbacks, then invokes any {@code CommandLineRunner}.
 * We make every one of those steps visible in this step.
 */
@SpringBootApplication
public class SpringLabApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringLabApplication.class, args);
    }
}

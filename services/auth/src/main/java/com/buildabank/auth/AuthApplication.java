// services/auth/src/main/java/com/buildabank/auth/AuthApplication.java
package com.buildabank.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** The Identity & Auth service: issues and validates JWTs, secured by Spring Security (Step 16). */
@SpringBootApplication
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}

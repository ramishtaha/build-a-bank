// gateway/src/main/java/com/buildabank/gateway/GatewayApplication.java
package com.buildabank.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** The API Gateway / BFF — the single front door routing to the bank's services (Step 15). */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}

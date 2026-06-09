// services/demand-account/src/main/java/com/buildabank/account/DemandAccountApplication.java
package com.buildabank.account;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** The Demand Account service: accounts + a double-entry ledger, with safe money movement (Step 12). */
@SpringBootApplication
public class DemandAccountApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemandAccountApplication.class, args);
    }
}

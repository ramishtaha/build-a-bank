// services/cif/src/main/java/com/buildabank/cif/CifApplication.java
package com.buildabank.cif;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** The CIF (Customer Information File) service — the bank's customer master. */
@SpringBootApplication
public class CifApplication {

    public static void main(String[] args) {
        SpringApplication.run(CifApplication.class, args);
    }
}

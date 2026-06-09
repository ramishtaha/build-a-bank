// playground/spring-lab/src/main/java/com/buildabank/springlab/account/Account.java
package com.buildabank.springlab.account;

import java.math.BigDecimal;

/** A minimal account for the Phase-A capstone vertical slice. Money is BigDecimal (the bank's rule). */
public record Account(String id, String owner, BigDecimal balance) {
}

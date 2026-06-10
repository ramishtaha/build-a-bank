// libs/common/src/main/java/com/buildabank/common/money/MoneyProperties.java
package com.buildabank.common.money;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Step 28 · typed configuration for the starter, bound from {@code buildabank.money.*} in a consumer's
 * {@code application.yml}. Two knobs: {@code enabled} (turn the whole feature off) and {@code currency-code}
 * (the prefix the {@link MoneyFormatter} renders). The configuration-processor turns this class into IDE
 * auto-complete + docs for those keys.
 */
@ConfigurationProperties(prefix = "buildabank.money")
public class MoneyProperties {

    /** Whether the MoneyFormatter bean is auto-configured. Default true. */
    private boolean enabled = true;

    /** ISO currency code rendered as the prefix (e.g. USD, EUR). Default USD. */
    private String currencyCode = "USD";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }
}

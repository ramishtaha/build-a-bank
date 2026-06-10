// services/market-info/src/main/java/com/buildabank/marketinfo/FxRate.java
package com.buildabank.marketinfo;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * An FX rate read model: 1 unit of {@code base} = {@code rate} units of {@code quote}, as of {@code asOf}
 * (epoch millis — kept primitive so the value serializes cleanly into the Redis cache). Implements
 * {@link Serializable} because Spring's default Redis cache serializer (JDK serialization) stores it as bytes.
 */
public record FxRate(String base, String quote, BigDecimal rate, long asOf) implements Serializable {
}

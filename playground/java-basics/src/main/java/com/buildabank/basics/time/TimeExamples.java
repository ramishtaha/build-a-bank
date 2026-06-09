// playground/java-basics/src/main/java/com/buildabank/basics/time/TimeExamples.java
package com.buildabank.basics.time;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * The modern {@code java.time} API (Java 8+) — immutable, thread-safe, and unambiguous.
 *
 * <ul>
 *   <li>{@link Instant} — a point on the UTC timeline. <b>What the bank stores.</b></li>
 *   <li>{@link ZonedDateTime} — an instant rendered in a specific zone. <b>What a user sees.</b></li>
 *   <li>{@link Duration} — an elapsed amount of time.</li>
 * </ul>
 * Rule of thumb: persist {@code Instant} (UTC); convert to a zone only at the display edge.
 */
public final class TimeExamples {

    private TimeExamples() { }

    /** The canonical stored form: an ISO-8601 UTC instant string, e.g. {@code 2026-06-09T13:29:14.842Z}. */
    public static String nowUtcIso(Instant now) {
        return DateTimeFormatter.ISO_INSTANT.format(now);
    }

    /** Render a stored UTC instant in a user's zone (display edge only). */
    public static ZonedDateTime inZone(Instant instant, ZoneId zone) {
        return instant.atZone(zone);
    }

    /** Settlement window length between two instants. */
    public static Duration between(Instant start, Instant end) {
        return Duration.between(start, end);
    }
}

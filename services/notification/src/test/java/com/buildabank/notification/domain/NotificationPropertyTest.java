// services/notification/src/test/java/com/buildabank/notification/domain/NotificationPropertyTest.java
package com.buildabank.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.BigRange;
import net.jqwik.api.constraints.NotBlank;
import net.jqwik.api.constraints.Scale;
import net.jqwik.api.constraints.StringLength;

/**
 * Step 28 · <strong>property-based</strong> test with jqwik. An example test ({@link NotificationTest}) checks
 * one hand-picked case; a property states an <em>invariant</em> and jqwik generates many randomized inputs
 * (and shrinks any failure to a minimal counter-example). The invariant here: for ANY transfer event,
 * {@link Notification#from} preserves the identifiers and the message names both parties and the amount.
 */
class NotificationPropertyTest {

    @Property
    void fromPreservesIdentifiersAndNamesBothPartiesAndAmount(
            @ForAll @AlphaChars @NotBlank @StringLength(min = 1, max = 8) String eventId,
            @ForAll @AlphaChars @NotBlank @StringLength(min = 1, max = 8) String transactionId,
            @ForAll @AlphaChars @NotBlank @StringLength(min = 1, max = 6) String fromAccount,
            @ForAll @AlphaChars @NotBlank @StringLength(min = 1, max = 6) String toAccount,
            @ForAll @BigRange(min = "0.01", max = "1000000.00") @Scale(2) BigDecimal amount) {

        TransferEvent event = new TransferEvent(
                eventId, transactionId, fromAccount, toAccount, amount, "2026-06-10T00:00:00Z");

        Notification n = Notification.from(event);

        // identifiers and value are carried through unchanged
        assertThat(n.eventId()).isEqualTo(eventId);
        assertThat(n.transactionId()).isEqualTo(transactionId);
        assertThat(n.fromAccount()).isEqualTo(fromAccount);
        assertThat(n.toAccount()).isEqualTo(toAccount);
        assertThat(n.amount()).isEqualByComparingTo(amount);

        // the human message is well-formed and mentions both parties and the amount
        assertThat(n.message())
                .startsWith("Transfer of ")
                .endsWith(" completed.")
                .contains(fromAccount)
                .contains(toAccount)
                .contains(amount.toString());
    }
}

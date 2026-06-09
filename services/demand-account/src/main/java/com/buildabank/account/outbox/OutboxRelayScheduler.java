// services/demand-account/src/main/java/com/buildabank/account/outbox/OutboxRelayScheduler.java
package com.buildabank.account.outbox;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Step 20 · drives the {@link OutboxRelay} on a timer in production. Kept separate from the relay so the
 * polling can be switched off in tests (which invoke {@link OutboxRelay#publishPending()} directly for
 * deterministic assertions): set {@code bank.outbox.relay.scheduled=false}. Enabled by default
 * ({@code matchIfMissing = true}) so a running service relays automatically.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "bank.outbox.relay.scheduled", havingValue = "true", matchIfMissing = true)
public class OutboxRelayScheduler {

    private final OutboxRelay relay;

    public OutboxRelayScheduler(OutboxRelay relay) {
        this.relay = relay;
    }

    @Scheduled(fixedDelayString = "${bank.outbox.relay-delay-ms:2000}")
    void drainOutbox() {
        relay.publishPending();
    }
}

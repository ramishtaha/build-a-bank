// services/demand-account/src/main/java/com/buildabank/account/outbox/OutboxEventRepository.java
package com.buildabank.account.outbox;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Reads/writes {@link OutboxEvent} rows. The relay drains the oldest unpublished rows in batches; everything
 * else (the in-transaction insert) is plain {@code save}.
 */
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /** Oldest-first batch of not-yet-published events for the relay to publish. */
    @Query("select e from OutboxEvent e where e.published = false order by e.createdAt asc")
    List<OutboxEvent> findUnpublished(Limit limit);

    long countByPublishedFalse();
}

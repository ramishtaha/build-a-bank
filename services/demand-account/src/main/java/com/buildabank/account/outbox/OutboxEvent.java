// services/demand-account/src/main/java/com/buildabank/account/outbox/OutboxEvent.java
package com.buildabank.account.outbox;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Step 20 · the <strong>Outbox</strong> row. Solves the <em>dual-write problem</em>: you cannot atomically
 * "update the database AND publish to Kafka" — if you write the DB then crash before publishing, the event is
 * lost; publish-then-write loses the DB change. The fix: write this row <strong>in the same transaction</strong>
 * as the business change (the ledger update), so either both commit or neither does. A separate
 * {@link OutboxRelay} later reads unpublished rows and publishes them to Kafka (at-least-once), marking them
 * {@code published}. The row {@code id} is the event id, carried through to Kafka as the dedupe key — so a
 * duplicate relay/delivery still yields exactly-once <em>effect</em> at an idempotent consumer (Step 19).
 */
@Entity
@Table(name = "outbox_event")
public class OutboxEvent {

    @Id
    @Column(updatable = false)
    private UUID id;

    /** The aggregate that produced the event (e.g. {@code transfer}) — useful for routing/partitioning. */
    @Column(name = "aggregate_type", nullable = false, updatable = false)
    private String aggregateType;

    /** The event type / topic key (e.g. {@code transfer.completed}). */
    @Column(nullable = false, updatable = false)
    private String type;

    /** The serialized event body (JSON) — exactly what gets published to Kafka. */
    @Column(nullable = false, updatable = false)
    private String payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private boolean published;

    @Column(name = "published_at")
    private Instant publishedAt;

    protected OutboxEvent() {
    }

    public OutboxEvent(UUID id, String aggregateType, String type, String payload, Instant createdAt) {
        this.id = id;
        this.aggregateType = aggregateType;
        this.type = type;
        this.payload = payload;
        this.createdAt = createdAt;
        this.published = false;
    }

    /** Mark this row published (called by the relay after a successful Kafka send). */
    public void markPublished(Instant at) {
        this.published = true;
        this.publishedAt = at;
    }

    public UUID getId() {
        return id;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getType() {
        return type;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isPublished() {
        return published;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }
}

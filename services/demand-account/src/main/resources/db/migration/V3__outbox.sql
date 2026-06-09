-- services/demand-account/src/main/resources/db/migration/V3__outbox.sql
-- Transactional Outbox (Step 20): events are written here IN THE SAME TRANSACTION as the ledger change,
-- so the business write and the "intent to publish" commit atomically (no dual-write data loss). A relay
-- polls unpublished rows and publishes them to Kafka, then marks them published. The partial index keeps
-- the relay's "find unpublished" scan cheap even as the table grows with published history.

create table outbox_event (
    id             uuid         primary key,
    aggregate_type varchar(64)  not null,
    type           varchar(128) not null,
    payload        text         not null,
    created_at     timestamp(6) with time zone not null,
    published      boolean      not null default false,
    published_at   timestamp(6) with time zone
);

create index idx_outbox_unpublished on outbox_event (created_at) where published = false;

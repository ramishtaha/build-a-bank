-- services/demand-account/src/main/resources/db/migration/V2__idempotency_keys.sql
-- Public-API idempotency (Step 14): a store of Idempotency-Key -> the result it produced, so a retried
-- request returns the original result instead of moving money twice. The PRIMARY KEY on the key gives us
-- the concurrency guard: two racing requests with the same key can't both insert, so only one transfer commits.

create table idempotency_key (
    idempotency_key varchar(200) primary key,
    transaction_id  uuid        not null,
    created_at      timestamp(6) with time zone not null
);

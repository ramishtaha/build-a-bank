# ADR-0006: URI versioning, public-API idempotency, and signed webhooks

- **Status:** Accepted
- **Date:** 2026-06-10
- **Deciders:** Build-a-Bank (autonomous senior default)
- **Step:** 14 — API Design, Versioning & Webhooks

## Context
As the demand-account API grows toward a partner-facing contract, three design decisions had to be made and
recorded (they're standard interview topics and shape every later API in the bank).

1. **Versioning strategy** — how do clients pin to a compatible API as it evolves?
2. **Idempotency** — how do we make money-moving requests safe to retry?
3. **Webhook delivery & security** — how do partners receive events safely?

## Decision

### 1. URI versioning (`/api/v1/...`)
New endpoints live under **`/api/v1`**. Considered alternatives:
- **Header versioning** (`Accept: application/vnd.bank.v1+json` or `X-API-Version: 1`) — clean URLs, content-negotiation-pure, but invisible in logs/browsers, harder to curl/cache, and easy to forget.
- **Media-type versioning** — most RESTful, but heavy for learners and tooling.
- **URI versioning** — *chosen*: visible, trivially curl-able and cacheable, unambiguous in logs, and the most common choice in real public APIs (Stripe-style date headers are a richer variant we note but don't adopt).

The pre-existing `POST /api/transfers` stays for compatibility but is **deprecated** via standard headers
(`Deprecation: true`, `Sunset: <date>`, `Link: </api/v1/transfers>; rel="successor-version"`, RFC 8594) so
clients can migrate. We do not break existing consumers.

### 2. Idempotency via an `Idempotency-Key` header + a key store
`POST /api/v1/transfers` accepts an optional `Idempotency-Key`. We persist key → produced `transactionId` in
`idempotency_key` (PK on the key). A retry with the same key returns the stored result without re-executing;
the PK uniqueness is the concurrency guard (a racing duplicate can't double-insert, so only one transfer
commits). The key insert + transfer run in **one transaction** (REQUIRED propagation). Matches Stripe's model.

### 3. Webhooks: HMAC-SHA256 signing + timestamp replay window + bounded retries
Outbound `transfer.completed` events are signed with **HMAC-SHA256** over `"<timestamp>.<body>"`; the receiver
verifies the signature (constant-time compare) and rejects timestamps outside a tolerance window (**replay
protection**) — the Stripe/GitHub scheme. Delivery **retries** with backoff (at-least-once), so **receivers
must be idempotent**. The secret is config-supplied (Vault in Phase H).

## Consequences
- ✅ Clients can pin to `v1`, migrate off deprecated endpoints on a schedule, retry transfers safely, and
  verify webhook authenticity — a genuinely partner-grade contract, all test-proven.
- ✅ No new dependencies (HMAC via `javax.crypto`, delivery via the JDK `HttpClient`, pagination via Spring Data).
- ⚠️ **Dual-write problem:** the webhook is sent *after* the DB transaction commits (best-effort). If the send
  fails, the DB is committed but the partner never hears — the **Outbox pattern (Step 20)** fixes this
  (persist the event in the same transaction, deliver asynchronously). Flagged, not hidden.
- ⚠️ Idempotency keys accumulate; a real system expires them (TTL). Noted for a later cleanup job/Step.
- 🔁 Webhook signing/secrets harden in Phase H (Vault); exactly-once *effect* via Outbox + idempotent
  consumers in Phase D (Steps 20–21).
- 📝 Pagination returns a custom `PageResponse` envelope, never Spring Data's `Page` JSON (whose shape is an
  unstable internal detail) — the API owns its contract.

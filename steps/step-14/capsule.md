# 🧳 Capsule — Step 14

**Exists now:** Full repo = 7 Maven modules; `services/demand-account` (Spring Boot 4, own Postgres, Flyway V1+V2) with **25** green tests (IntegrationTest 3 · ConcurrentTransfer 2 · Idempotency 3 · OptimisticLock 1 · TxPropagation 1 · TransferService 2 · TransferController 7 · WebhookDelivery 2 · WebhookSigner 4). Endpoints: `POST /api/accounts` (201), `GET /api/accounts/{n}`, deprecated `POST /api/transfers`, `POST /api/v1/transfers` (Idempotency-Key + signed webhook), `GET /api/v1/accounts/{n}/entries` (PageResponse).

**This step added:**
- `/api/v1` namespace; old `POST /api/transfers` kept but deprecated via `Deprecation: true` + `Sunset` + `Link: …successor-version` headers
- Idempotency: `V2__idempotency_keys.sql` (PK = concurrency guard), `IdempotencyRecord(+Repository)`, `IdempotentTransferService` (lookup-or-execute-and-store in one `@Transactional`); proof: same key → same txId, balance 150 (moved once); diff key → 100; no key → 180
- Webhooks: `WebhookSigner` (HMAC-SHA256 over `ts.body`, tolerance-window replay protection, constant-time compare), `WebhookSender` (JDK HttpClient, MAX_ATTEMPTS=3, linear backoff), `WebhookPublisher` (gated by `bank.webhook.url`/`bank.webhook.secret` via `@Value`; owns a Jackson-2 ObjectMapper); in-test receiver verified the signature, transient 500 → retried (calls ≥ 2)
- Pagination: `LedgerEntryRepository.findByAccountId(…, Pageable)`, `TransferService.entriesOf`, `PageResponse` + `LedgerEntryResponse` envelope (never Spring's `Page`)
- Tests 13 → 25; ADR-0006; **no new dependencies**

**Gotchas:**
- Boot 4 web defaults to Jackson 3 → no Jackson-2 `ObjectMapper` bean; injecting one fails the context — `WebhookPublisher` owns `new ObjectMapper()`
- `@WebMvcTest` slice must mock `IdempotentTransferService` + `WebhookPublisher` or the controller slice won't load
- Shared-DB tests: clean `idempotency_key` (then ledger, then account — FK order) in `@BeforeEach` or stale keys skew balance assertions
- Concurrent same-key loser gets a unique-violation error (not the stored txId); only its next retry returns the stored result
- Keys have no TTL (noted in ADR-0006); webhook sends after commit → dual-write gap, closed by the Outbox in Step 20
- `Deprecation: true` is the legacy draft form our code sends; RFC 9745 standardizes an `@<unix-ts>` date (`Sunset` is RFC 8594)

**Callback hooks:**
- Idempotency-Key + PK-as-concurrency-guard → referenced by Step 20 (Outbox) and Step 21 (Saga + idempotent consumers)
- Webhook scheme: `X-Webhook-Signature` = HMAC_SHA256(secret, ts + "." + body) + `X-Webhook-Timestamp`; at-least-once → receivers dedupe
- `Deprecation`/`Sunset`/`Link` trio → Step 15's gateway can enforce versioning/deprecation centrally

**Next step starts:** `step-14-end == step-15-start`; green: `./mvnw -pl services/demand-account -am verify` (25 tests), §12.3 mutation reverted, `steps/step-14/smoke.sh` PASSED, clean-room verify (all 7 modules).

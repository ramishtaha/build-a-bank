# 🧳 Capsule - Step 9

**Exists now:** `services/cif` — `Customer` + `Address` entities, `KycStatus`, `CustomerRepository` (derived queries + `findAllWithAddresses` `@EntityGraph` + `findByKycStatus` projection), `CustomerSummary`, Flyway V1+V2, `ddl-auto=validate`, `open-in-view: false`. REST API unchanged from Step 8 (CIF on :8081; no new endpoints, no new `requests.http`). CIF 10 tests; repo-wide 50 tests, BUILD SUCCESS at `step-09-end` (🔴 Full tier).

**This step added:**
- `Address` entity (`@ManyToOne` LAZY; package-private `setCustomer`, set via `addAddress`)
- `Customer`: lazy `@OneToMany addresses` + `addAddress()` helper + `@Version long version`
- Flyway `V2__add_address_and_version.sql` (version column `default 0`; `address` table; FK index `idx_address_customer`)
- Repository: `findAllWithAddresses()` (`@EntityGraph`) + `findByKycStatus()` → `CustomerSummary` interface projection
- `CustomerFetchTest` (3 tests: N+1 = 3 stmts vs `@EntityGraph` = 1, via Hibernate statistics; projection) + `OptimisticLockingTest` (1 test: conflict → `ObjectOptimisticLockingFailureException`) — CIF 6→10 tests (+4)

**Gotchas:**
- Docker required: every proof runs on Testcontainers Postgres; H2 not trusted for locking or statement counts
- `@DataJpaTest` needs `@ImportAutoConfiguration(FlywayAutoConfiguration.class)` + `@AutoConfigureTestDatabase(replace = NONE)`, else Flyway is skipped / H2 swapped in
- Statistics tests need `entityManager.flush()` + `clear()` after seeding, or the 1st-level cache hides the N+1
- Never edit an applied migration (Flyway checksum mismatch); Postgres does not auto-index FKs (hence `idx_address_customer`)
- Windows: a stray `java` process holding the jar fails `repackage` ("Unable to rename") — kill it, rebuild

**Callback hooks:**
- `open-in-view: false` (set in Step 8, explained here) is why lazy-outside-tx fails fast — cited whenever fetching comes up
- Optimistic (`@Version`) vs pessimistic locking trade-off returns in Step 12 (ledger); JMM in Step 11; `EXPLAIN`/indexes in Step 10
- §12.3 mutation sanity-check (remove `@Version` → test FAILS → revert → green) proven and recorded in the ledger

**Next step starts:** `step-09-end == step-10-start`; green: `./mvnw -pl services/cif -am verify` (10 CIF tests, 50 repo-wide) and `steps/step-09/smoke.sh` PASSED.

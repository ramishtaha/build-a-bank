# 🃏 Build-a-Bank — Cumulative Flashcards

> Spaced-repetition deck. Each step appends 3–5 Q/A pairs. Import into Anki via the optional CSV, or just self-quiz.
> Format: **Q:** question — **A:** answer.

## Step 1 — Setup, CLI/Git & first Spring Boot app
- **Q:** What three annotations does `@SpringBootApplication` combine? — **A:** `@SpringBootConfiguration`, `@EnableAutoConfiguration`, `@ComponentScan`.
- **Q:** Which Spring Boot 4 removal broke our first test, and what replaces it? — **A:** `TestRestTemplate` (+ `org.springframework.boot.test.web.client`); use `RestClient` / `RestTestClient` / `MockMvcTester`.
- **Q:** `./mvnw verify` vs `package`? — **A:** `verify` runs the full lifecycle including all tests (ends in BUILD SUCCESS only if green); `package` just builds the jar.
- **Q:** Why pin versions and never use `latest`? — **A:** reproducibility — the identical build every time; no surprise upstream breakage.
- **Q:** The chain invariant? — **A:** `step-NN-end` == `step-(NN+1)-start`, and both build clean.

## Step 2 — Java language primer
- **Q:** Why never use `double` for money? — **A:** binary floating point can't represent decimal fractions exactly → rounding errors; use `BigDecimal` with an explicit scale + `RoundingMode`.
- **Q:** What does sealing an interface buy you in a `switch`? — **A:** exhaustiveness — the compiler verifies every permitted type is handled; no `default` needed, and adding a type fails the build until you handle it.
- **Q:** What does a `record` generate for you? — **A:** the canonical constructor, accessors, `equals`, `hashCode`, `toString` — and it's shallowly immutable.
- **Q:** `Instant` vs `LocalDate` vs `ZonedDateTime` — which do you store? — **A:** store `Instant` (UTC); `LocalDate` for zone-less dates (e.g. DOB); `ZonedDateTime` only at the display edge.
- **Q:** What problem does `Optional` solve? — **A:** it models "maybe absent" in the type system so callers can't forget the not-found case → no surprise `NullPointerException`.

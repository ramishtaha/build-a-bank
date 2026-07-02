# 🧳 Capsule - Step 1

**Exists now:** parent aggregator POM (Java 25, Spring Boot 4.0.6 BOM, Maven Wrapper 3.9.12) + one module `services/hello`; endpoints `GET /api/hello` → 200 JSON, `GET /actuator/health` → UP, `GET /actuator/info`; port 8080 (tests random); `./mvnw -B verify` = BUILD SUCCESS, 2/2 tests, repackaged executable jar (Tomcat 11.0.21).

**This step added:**
- `services/hello/pom.xml` — web + actuator + test starters, spring-boot-maven-plugin
- `HelloApplication.java` — `@SpringBootApplication` entry point
- `HelloController.java` — `GET /api/hello` returning JSON greeting + UTC `Instant` timestamp
- `application.yml` — port 8080, graceful shutdown, virtual threads on, actuator exposes health+info only
- `HelloApplicationTests.java` — `@SpringBootTest(RANDOM_PORT)` + `RestClient`: contextLoads + endpoint 200/body
- `steps/step-01/requests.http` + `smoke.sh`

**Gotchas:**
- `TestRestTemplate` (whole `org.springframework.boot.test.web.client` package) removed in Boot 4 — use `RestClient`/`RestTestClient`/`MockMvcTester`; the old import fails at compile time
- Wrong JDK on `PATH`/`JAVA_HOME` is the #1 snag; Windows invokes `.\mvnw.cmd`, not `./mvnw`
- No hot reload: restart `spring-boot:run` after edits; controller must sit at/below `com.buildabank.hello` or → 404
- Docker only verified installed, not used until Step 8

**Callback hooks:**
- `./mvnw -B verify` is the standing gate; proof-of-run evidence = random test port + Tomcat 11.0.21 + 2/2 tests
- House rules: time = UTC `Instant`, money = `BigDecimal`; secrets = gitignored `.env` + committed fake `.env.example`
- Pins live in `VERSIONS.md` (Java 25.0.3, Boot 4.0.6, Maven 3.9.12) — never `latest`

**Next step starts:** `step-01-end == step-02-start`; green = BUILD SUCCESS 2/2 tests, `/api/hello` 200 JSON, `/actuator/health` UP, `smoke.sh` PASSED.

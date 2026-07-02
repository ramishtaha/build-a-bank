# Build-a-Bank — convenience targets. The CLI (./mvnw, docker, git) is canonical;
# every target documents its raw equivalent so you are never blocked without `make`.
# Windows: run these in Git Bash/WSL, or just run the raw command shown in each recipe.
# (PowerShell users: see `make help` output for the .\mvnw.cmd equivalents.)

MVNW ?= ./mvnw

.DEFAULT_GOAL := help
.PHONY: help doctor verify build test run-hello play-01 play-10 play-11 run-demand-account play-12 play-13 play-14 run-gateway play-15 run-auth play-16 play-17 play-18 play-19 run-notification play-20 play-21 run-market-info play-22 run-onboarding play-23 play-24 play-25 play-26 play-27 play-28 mutation format play-29 play-30 frontend-install frontend-dev frontend-test frontend-build clean

help: ## Show this help
	@echo "Build-a-Bank targets:"
	@grep -E '^[a-zA-Z0-9_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN{FS=":.*?## "}{printf "  \033[36m%-12s\033[0m %s\n",$$1,$$2}'

doctor: ## Preflight: print versions of the required toolchain
	@echo "== Build-a-Bank doctor ==" ; \
	java -version  ; \
	$(MVNW) -v     ; \
	docker --version ; docker compose version ; \
	node --version ; npm --version ; \
	python --version ; \
	git --version ; \
	echo "Tip: if Docker is not 'running', start Docker Desktop. See CAPABILITIES.md."

verify: ## Full build + all tests (the gate before any step is 'done')
	$(MVNW) -B verify
	# Windows PowerShell equivalent: .\mvnw.cmd -B verify

build: ## Compile + package without running the verify phase
	$(MVNW) -B -DskipTests package

test: ## Run the test suite only
	$(MVNW) -B test

run-hello: ## Run the Step-1 hello-service (Ctrl-C to stop). http://localhost:8080
	$(MVNW) -pl services/hello spring-boot:run
	# Windows: .\mvnw.cmd -pl services/hello spring-boot:run

play-01: ## Step 1 demo: curl the running hello-service (start it first with `make run-hello`)
	@echo "GET /api/hello:"        ; curl -s  http://localhost:8080/api/hello ; echo ; \
	 echo "GET /actuator/health:" ; curl -s  http://localhost:8080/actuator/health ; echo

play-10: ## Step 10: run the six database labs on a real Postgres (needs Docker)
	$(MVNW) -pl services/cif test -Dtest='QueryPlanLabTest,MvccIsolationTest,WriteSkewTest,ConnectionPoolTest,PartitioningLabTest,OnlineSchemaChangeTest'
	@echo "Tip: explore the same SQL by hand — see steps/step-10/queries.sql"
	# Windows: .\mvnw.cmd -pl services/cif test -Dtest='QueryPlanLabTest,MvccIsolationTest,WriteSkewTest,ConnectionPoolTest,PartitioningLabTest,OnlineSchemaChangeTest'

play-11: ## Step 11: run the concurrency labs (pure JVM, no Docker) — watch the race lose deposits
	$(MVNW) -pl playground/concurrency-lab test
	# Windows: .\mvnw.cmd -pl playground/concurrency-lab test

run-demand-account: ## Run the Demand Account service (needs a Postgres; see services/demand-account/compose.yaml). http://localhost:8082
	SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/demand_account $(MVNW) -pl services/demand-account spring-boot:run
	# First: docker compose -f services/demand-account/compose.yaml up -d
	# Windows: $$env:SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:5433/demand_account'; .\mvnw.cmd -pl services/demand-account spring-boot:run

play-12: ## Step 12: the Phase-B capstone — fails without locking, passes with it (needs Docker)
	$(MVNW) -pl services/demand-account test -Dtest=ConcurrentTransferTest
	@echo "Then drive the live API with steps/step-12/requests.http (start it with 'make run-demand-account')"

play-13: ## Step 13: ProblemDetail + OpenAPI/Swagger UI. Run the service, then open http://localhost:8082/swagger-ui.html
	@echo "Start Postgres:  docker compose -f services/demand-account/compose.yaml up -d"
	@echo "Run service:     make run-demand-account   (then browse http://localhost:8082/swagger-ui.html)"
	@echo "Or just test it: $(MVNW) -pl services/demand-account -am verify"

play-14: ## Step 14: versioning, idempotency, pagination & signed webhooks (needs Docker)
	$(MVNW) -pl services/demand-account -am verify
	@echo "Then drive the live API with steps/step-14/requests.http (start it with 'make run-demand-account')"

run-gateway: ## Run the API Gateway (front door) on http://localhost:8080 (start cif:8081 + demand-account:8082 first)
	$(MVNW) -pl gateway spring-boot:run
	# Windows: .\mvnw.cmd -pl gateway spring-boot:run

play-15: ## Step 15: gateway routing + the declarative CifClient (needs Docker for demand-account tests)
	$(MVNW) -pl gateway,services/demand-account -am verify
	@echo "Then run all three (cif, demand-account, gateway) and drive steps/step-15/requests.http through :8080"

run-auth: ## Run the Auth service on http://localhost:8083 (no DB needed; demo users alice/admin)
	$(MVNW) -pl services/auth spring-boot:run
	# Windows: .\mvnw.cmd -pl services/auth spring-boot:run

play-16: ## Step 16: Spring Security — login for a JWT, then 401/403/200. See steps/step-16/requests.http
	$(MVNW) -pl services/auth test
	@echo "Run it: make run-auth  → then drive steps/step-16/requests.http (login alice/password, call /me, /admin)"

play-17: ## Step 17: resource servers + RS256/JWKS. Get a token from auth, use it at the secured money service.
	$(MVNW) -pl services/auth,services/demand-account -am verify
	@echo "Live: run auth (8083) + demand-account (8082, AUTH_JWKS_URI=http://localhost:8083/oauth2/jwks) — see steps/step-17/requests.http"

play-18: ## Step 18: secure coding — injection-safety + edge hardening (headers/CORS) tests + threat model (needs Docker)
	$(MVNW) -pl services/cif,services/demand-account -am test -Dtest='SqlInjectionSafetyTest,SecurityHardeningTest'
	@echo "Then read security/threat-model.md + security/risk-register.md; drive steps/step-18/requests.http (curl -i for headers, OPTIONS for CORS)"
	# Windows: .\mvnw.cmd -pl services/cif,services/demand-account -am test -Dtest='SqlInjectionSafetyTest,SecurityHardeningTest'

play-19: ## Step 19: distributed-systems theory labs — CAP/PACELC, quorums, clocks, delivery (pure JVM, no Docker)
	$(MVNW) -pl playground/distributed-lab test
	@echo "Tweak the knobs: see steps/step-19/lesson.md '🎮 Play With It' (LWW timestamps, W/R sizes, delivery counts)"
	# Windows: .\mvnw.cmd -pl playground/distributed-lab test

run-notification: ## Run the Notification service on http://localhost:8084 (needs a Kafka broker; set KAFKA_BOOTSTRAP_SERVERS)
	KAFKA_BOOTSTRAP_SERVERS=$${KAFKA_BOOTSTRAP_SERVERS:-localhost:9092} $(MVNW) -pl services/notification spring-boot:run
	# Broker: docker run -d --name bank-redpanda -p 9092:9092 redpandadata/redpanda:v24.2.7 redpanda start --mode dev-container --advertise-kafka-addr PLAINTEXT://localhost:9092
	# Windows: $$env:KAFKA_BOOTSTRAP_SERVERS='localhost:9092'; .\mvnw.cmd -pl services/notification spring-boot:run

play-20: ## Step 20: events + Outbox + Kafka + SSE notifications (needs Docker for Testcontainers Postgres + Redpanda)
	$(MVNW) -pl services/demand-account,services/notification test -Dtest='OutboxWriteTest,OutboxRelayKafkaTest,TransferEventConsumerKafkaTest,NotificationControllerTest'
	@echo "Live: start a broker + auth + demand-account + notification, open the SSE stream, then transfer — see steps/step-20/requests.http"

play-21: ## Step 21: payment Saga (compensation) + Redis idempotency + Kafka DLQ (needs Docker: Postgres + Redis + Redpanda)
	$(MVNW) -pl services/demand-account,services/notification test -Dtest='PaymentSagaTest,PaymentControllerTest,DeadLetterTest'
	@echo "Live: docker run -d -p 6379:6379 redis:7.4-alpine; then POST /api/v1/payments with an Idempotency-Key — see steps/step-21/requests.http"

run-market-info: ## Run the Market Info service on http://localhost:8085 (needs Redis; set REDIS_HOST)
	REDIS_HOST=$${REDIS_HOST:-localhost} $(MVNW) -pl services/market-info spring-boot:run
	# Redis: docker run -d --name bank-redis -p 6379:6379 redis:7.4-alpine
	# Windows: $$env:REDIS_HOST='localhost'; .\mvnw.cmd -pl services/market-info spring-boot:run

play-22: ## Step 22: Redis cache read model + @Async (virtual threads) + ShedLock scheduling (needs Docker: Redis)
	$(MVNW) -pl services/market-info test -Dtest='MarketCacheTest,ShedLockTest,MarketControllerTest'
	@echo "Live: run-market-info, then GET /api/market/rates/USD/EUR (first slow, then cached) — see steps/step-22/requests.http"

run-onboarding: ## Run the Onboarding orchestrator on http://localhost:8086 (set CIF_URL/ACCOUNT_URL to the services)
	CIF_URL=$${CIF_URL:-http://localhost:8081} ACCOUNT_URL=$${ACCOUNT_URL:-http://localhost:8082} $(MVNW) -pl services/onboarding spring-boot:run
	# Windows: $$env:CIF_URL='http://localhost:8081'; $$env:ACCOUNT_URL='http://localhost:8082'; .\mvnw.cmd -pl services/onboarding spring-boot:run

play-23: ## Step 23: onboarding orchestration + compensation (no Docker for the orchestration tests)
	$(MVNW) -pl services/onboarding test
	@echo "Live: run auth+cif+demand-account+onboarding, then POST /api/onboarding (with a token) — see steps/step-23/requests.http"

play-24: ## Step 24: Spring Batch EOD interest accrual + the Phase-D exactly-once capstone (needs Docker: Postgres + Redpanda)
	$(MVNW) -pl services/demand-account test -Dtest='InterestAccrualJobTest,PaymentExactlyOnceCapstoneTest'
	@echo "End of Phase D 🎖️ — fault-tolerant batch (skip/retry) + exactly-once effect end-to-end"

play-25: ## Step 25: SOLID refactor of the notification consumer — unchanged integration tests + new unit tests (needs Docker)
	$(MVNW) -pl services/notification test
	@echo "Behaviour-preserving refactor: the UNCHANGED integration tests pass + new TransferEventParserTest/InMemoryProcessedEventStoreTest"

play-26: ## Step 26: hexagonal restructure of notification (domain/application/adapter) — behaviour preserved (needs Docker)
	$(MVNW) -pl services/notification test
	@echo "Inspect the hexagon: ls -R services/notification/src/main/java/com/buildabank/notification — domain has only java.* imports"

play-27: ## Step 27: enforce architecture — ArchUnit hexagon (notification) + Spring Modulith modules (demand-account); NO Docker
	$(MVNW) -pl services/notification  -Dtest=HexagonalArchitectureTest test
	$(MVNW) -pl services/demand-account -Dtest=ModularityTest test
	@echo "Living docs: services/demand-account/target/spring-modulith-docs/ (components.puml + per-module canvases)"

play-28: ## Step 28: mutation testing (PITest) + property tests (jqwik) + custom starter + quality gates; NO Docker
	$(MVNW) -pl services/notification -Pmutation test-compile org.pitest:pitest-maven:mutationCoverage
	$(MVNW) -pl libs/common,services/hello -am test
	@echo "Mutation report: services/notification/target/pit-reports/index.html — 100% on the hexagon core. Gates run in 'make verify'."

mutation: ## Run PITest mutation coverage on the notification hexagon core (Phase-E capstone; NO Docker)
	$(MVNW) -pl services/notification -Pmutation test-compile org.pitest:pitest-maven:mutationCoverage

format: ## Auto-fix formatting with Spotless (then 'make verify' enforces it)
	$(MVNW) -B spotless:apply

play-29: ## Step 29: build, lint & test the React+TS SPA + gateway auth-route/CORS test; NO Docker (needs Node 22)
	cd frontend && npm ci && npm run build && npm run lint && npm test
	$(MVNW) -pl gateway -Dtest=GatewayRoutingTest test
	@echo "Live: 'make frontend-dev' (SPA :5173) + run gateway:8080 & auth:8083 (APP_CORS_ALLOWED_ORIGINS=http://localhost:5173)"

play-30: ## Step 30: SPA data/forms/SSE (TanStack Query + RHF/Zod + EventSource) + gateway notification route; NO Docker
	cd frontend && npm ci && npm run build && npm run lint && npm test
	$(MVNW) -pl gateway -Dtest=GatewayRoutingTest test
	@echo "Live: run gateway+auth+demand-account(+Postgres/Redis)+notification(+Redpanda), 'make frontend-dev', sign in, transfer ACC-A->ACC-B"

frontend-install: ## Install the SPA's dependencies from the lockfile (npm ci)
	cd frontend && npm ci

frontend-dev: ## Run the Vite dev server (http://localhost:5173)
	cd frontend && npm run dev

frontend-test: ## Lint + unit/component test the SPA (Vitest + Testing Library)
	cd frontend && npm run lint && npm test

frontend-build: ## Type-check + production-build the SPA (tsc + vite)
	cd frontend && npm run build

fullstack-up: ## Step 32: infra (Postgres:5433/Redis/Redpanda) + the SPA container. Then run the 4 services (see fullstack-services)
	docker compose -f deploy/compose.fullstack.yaml up -d --build
	@echo "Infra + SPA up. Now start the services (each in its own terminal) — see 'make fullstack-services'"

fullstack-services: ## Step 32: print the exact commands to run the four services on the host
	@echo "./mvnw -pl services/auth spring-boot:run"
	@echo "SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/demand_account APP_CORS_ALLOWED_ORIGINS=http://localhost:8080 ./mvnw -pl services/demand-account spring-boot:run"
	@echo "./mvnw -pl services/notification spring-boot:run"
	@echo "./mvnw -pl gateway spring-boot:run"
	@echo "Then: open http://localhost:8080 (the WHOLE bank through one origin) · capstone: cd frontend && npm run test:e2e:fullstack"

fullstack-down: ## Step 32: stop the compose stack (drops the data volume too)
	docker compose -f deploy/compose.fullstack.yaml down -v

# ── Step 33: the containerized bank (compose profile "bank") ────────────────
# Raw commands (make is a convenience, never a requirement):
#   docker compose -f deploy/compose.fullstack.yaml --profile bank up -d --build
#   docker compose -f deploy/compose.fullstack.yaml --profile bank down -v

bank-up: ## Step 33: the WHOLE bank in containers — 7 distroless Java images + SPA + infra, one origin :8080
	docker compose -f deploy/compose.fullstack.yaml --profile bank up -d --build
	@echo "Everything is a container now. Open http://localhost:8080 — capstone: cd frontend && npm run test:e2e:fullstack"

bank-down: ## Step 33: stop the containerized bank (drops the data volume too)
	docker compose -f deploy/compose.fullstack.yaml --profile bank down -v

bank-ps: ## Step 33: list the bank's containers + health
	docker compose -f deploy/compose.fullstack.yaml --profile bank ps

bank-logs: ## Step 33: follow one service's logs, e.g. `make bank-logs S=gateway`
	docker compose -f deploy/compose.fullstack.yaml --profile bank logs -f $(S)

image-service: ## Step 33: build one service image by hand, e.g. `make image-service MODULE=services/auth PORT=8083`
	docker build -f deploy/Dockerfile.service --build-arg MODULE=$(MODULE) --build-arg PORT=$(PORT) \
		-t bab-$(notdir $(MODULE)):0.1.0-SNAPSHOT .

clean: ## Remove all build output
	$(MVNW) -B clean

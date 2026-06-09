# Build-a-Bank — convenience targets. The CLI (./mvnw, docker, git) is canonical;
# every target documents its raw equivalent so you are never blocked without `make`.
# Windows: run these in Git Bash/WSL, or just run the raw command shown in each recipe.
# (PowerShell users: see `make help` output for the .\mvnw.cmd equivalents.)

MVNW ?= ./mvnw

.DEFAULT_GOAL := help
.PHONY: help doctor verify build test run-hello play-01 play-10 play-11 run-demand-account play-12 play-13 play-14 run-gateway play-15 run-auth play-16 play-17 play-18 play-19 run-notification play-20 play-21 clean

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

clean: ## Remove all build output
	$(MVNW) -B clean

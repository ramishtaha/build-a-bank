# Build-a-Bank — convenience targets. The CLI (./mvnw, docker, git) is canonical;
# every target documents its raw equivalent so you are never blocked without `make`.
# Windows: run these in Git Bash/WSL, or just run the raw command shown in each recipe.
# (PowerShell users: see `make help` output for the .\mvnw.cmd equivalents.)

MVNW ?= ./mvnw

.DEFAULT_GOAL := help
.PHONY: help doctor verify build test run-hello play-01 play-10 play-11 run-demand-account play-12 play-13 clean

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

clean: ## Remove all build output
	$(MVNW) -B clean

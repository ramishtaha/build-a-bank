# Build-a-Bank — convenience targets. The CLI (./mvnw, docker, git) is canonical;
# every target documents its raw equivalent so you are never blocked without `make`.
# Windows: run these in Git Bash/WSL, or just run the raw command shown in each recipe.
# (PowerShell users: see `make help` output for the .\mvnw.cmd equivalents.)

MVNW ?= ./mvnw

.DEFAULT_GOAL := help
.PHONY: help doctor verify build test run-hello play-01 clean

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

clean: ## Remove all build output
	$(MVNW) -B clean

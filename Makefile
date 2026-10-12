STATE   := $(CURDIR)/dev/.state
SCENARIO ?= basic

.DEFAULT_GOAL := help
.PHONY: help check dev backend frontend scenario reset test

help: ## Show available commands
	@grep -E '^[a-z]+:.*##' $(MAKEFILE_LIST) | awk -F':.*## ' '{printf "  make %-9s %s\n", $$1, $$2}'

check:
	@for tool in java mvn node npm git docker; do \
	  command -v $$tool >/dev/null || { echo "Missing prerequisite: $$tool" >&2; exit 1; }; \
	done

dev: check ## Start backend (127.0.0.1:8080) and dashboard (http://localhost:4200); login admin / admin
	@[ -d $(STATE)/scenario/config ] || $(MAKE) --no-print-directory scenario
	@$(MAKE) -j2 backend frontend

backend:
	@mkdir -p $(STATE)/data
	@cd backend && \
	  if [ -d $(STATE)/scenario/config ]; then export DIRIGENT_DEPLOYMENTS_GIT_URL=$(STATE)/scenario/config; fi && \
	  mvn -q test-compile spring-boot:run \
	    -Dspring-boot.run.profiles=dev \
	    -Dspring-boot.run.additional-classpath-elements=$(CURDIR)/backend/target/test-classes \
	    -Dspring-boot.run.workingDirectory=$(STATE) 2>&1 | sed -u 's/^/[backend]  /'

frontend:
	@cd frontend && { [ -d node_modules ] || npm install; } && npm start 2>&1 | sed -u 's/^/[frontend] /'

scenario: ## Load a test scenario: make scenario SCENARIO=basic (then restart 'make dev')
	@mkdir -p $(STATE)
	@dev/load-scenario.sh $(SCENARIO) $(STATE)

reset: ## Erase all local test data (database, secrets, tokens, scenario). Stop 'make dev' first
	@if ss -ltn 2>/dev/null | grep -q '127.0.0.1:8080 '; then echo "Backend is running. Stop it first." >&2; exit 1; fi
	@rm -rf $(STATE)
	@echo "Test environment reset."

test: check ## Run backend tests and the frontend production build
	@cd backend && mvn -q test
	@cd frontend && { [ -d node_modules ] || npm install; } && npm run build

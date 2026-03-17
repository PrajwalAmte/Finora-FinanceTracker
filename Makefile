SHELL   := /bin/bash
API_DIR := Finora-API
UI_DIR  := Finora-UI

.DEFAULT_GOAL := help

.PHONY: help test coverage lint-api lint-ui lint build-api build-ui build ci up down clean

help: ## Show all available targets
	@awk 'BEGIN{FS=":.*?## "} /^[a-zA-Z_-]+:.*?## /{printf "  \033[36m%-15s\033[0m %s\n",$$1,$$2}' $(MAKEFILE_LIST)

# ── Backend ────────────────────────────────────────────────────────────────────

test: ## Run backend unit tests (H2 in-memory, no DB needed)
	cd $(API_DIR) && mvn test --no-transfer-progress

coverage: ## Run backend tests + enforce JaCoCo coverage (≥ 85 % instruction, ≥ 75 % branch)
	cd $(API_DIR) && mvn verify --no-transfer-progress

lint-api: ## Run Checkstyle on backend source (naming errors fail; style issues warn)
	cd $(API_DIR) && mvn checkstyle:check --no-transfer-progress

build-api: ## Compile and package backend JAR (skips tests)
	cd $(API_DIR) && mvn package -DskipTests --no-transfer-progress

# ── Frontend ───────────────────────────────────────────────────────────────────

lint-ui: ## Run ESLint + TypeScript type-check on the frontend
	cd $(UI_DIR) && npm run lint && npx tsc --noEmit

build-ui: ## Install deps and build the frontend for production
	cd $(UI_DIR) && npm ci && npm run build

# ── Combined ───────────────────────────────────────────────────────────────────

lint: lint-api lint-ui ## Run all linters (Checkstyle + ESLint + tsc)

build: build-api build-ui ## Build both the API JAR and the UI bundle

ci: coverage lint-api lint-ui build-api build-ui ## Full local CI pass (mirrors the GitHub Actions pipeline)

# ── Docker ─────────────────────────────────────────────────────────────────────

up: ## Start all services via Docker Compose
	docker compose up -d

down: ## Stop and remove Docker Compose services
	docker compose down

# ── Cleanup ────────────────────────────────────────────────────────────────────

clean: ## Remove all build artefacts (Maven target/ + UI dist/)
	cd $(API_DIR) && mvn clean --no-transfer-progress
	rm -rf $(UI_DIR)/dist

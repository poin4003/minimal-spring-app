LLAMA_ENV ?= llama-server.env
-include $(LLAMA_ENV)
-include .env
POST_AI_MODERATION_ENABLED ?= false
.EXPORT_ALL_VARIABLES:

APP_ENV ?= dev
JAR_FILE := target/spring-application-0.0.1-SNAPSHOT.jar
AI_RUNTIME_DEPENDENCY :=

ifeq ($(strip $(POST_AI_MODERATION_ENABLED)),true)
    AI_RUNTIME_DEPENDENCY := ai-run
endif

ifeq ($(OS),Windows_NT)
    SHELL := cmd.exe
    MVN_CMD := mvn
    JAVA_CMD := java
    AI_RUN_CMD := powershell -NoProfile -ExecutionPolicy Bypass -File scripts\ai\start-llama.ps1
    AI_DOWN_CMD := powershell -NoProfile -ExecutionPolicy Bypass -File scripts\ai\stop-llama.ps1
    AI_HEALTH_CMD := powershell -NoProfile -ExecutionPolicy Bypass -File scripts\ai\health-llama.ps1
else
    MVN_CMD := ./mvnw
    JAVA_CMD := java
    AI_RUN_CMD := if command -v systemctl >/dev/null 2>&1 \
            && systemctl is-active --quiet llama-server.service; then \
        echo "llama-server.service is active."; \
    else \
        echo "WARNING: llama-server.service is unavailable; Spring will start without the optional AI runtime." >&2; \
    fi
    AI_DOWN_CMD := echo "Run: sudo systemctl stop llama-server.service"
    AI_HEALTH_CMD := sh ./scripts/ai/health-llama.sh
endif

.PHONY: dev build run clean check-build ai-run ai-up ai-down ai-health

dev: $(AI_RUNTIME_DEPENDENCY)
	@echo "Starting server in DEV mode..."
	$(MVN_CMD) -DskipTests spring-boot:run -Dspring-boot.run.profiles=dev

ai-run:
	@$(AI_RUN_CMD)

ai-up: ai-run

ai-down:
	@$(AI_DOWN_CMD)

ai-health:
	@$(AI_HEALTH_CMD)

build:
	@echo "Building executable JAR..."
	$(MVN_CMD) -DskipTests clean package

run: $(AI_RUNTIME_DEPENDENCY) check-build
	@echo "Running packaged application with APP_ENV=$(APP_ENV)..."
	$(JAVA_CMD) -jar "$(JAR_FILE)" --spring.profiles.active=$(APP_ENV)

clean:
	@echo "Cleaning build output..."
	$(MVN_CMD) clean

check-build:
	$(if $(wildcard $(JAR_FILE)),,$(error Missing executable JAR: $(JAR_FILE). Run 'make build' first))

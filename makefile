LLAMA_ENV ?= llama-server.env
-include $(LLAMA_ENV)
-include .env
.EXPORT_ALL_VARIABLES:

APP_ENV ?= dev
JAR_FILE := target/spring-application-0.0.1-SNAPSHOT.jar

ifeq ($(OS),Windows_NT)
    SHELL := cmd.exe
    MVN_CMD := mvn
    JAVA_CMD := java
    AI_UP_CMD := powershell -NoProfile -ExecutionPolicy Bypass -File scripts\ai\start-llama.ps1
    AI_DOWN_CMD := powershell -NoProfile -ExecutionPolicy Bypass -File scripts\ai\stop-llama.ps1
    AI_HEALTH_CMD := powershell -NoProfile -ExecutionPolicy Bypass -File scripts\ai\health-llama.ps1
else
    MVN_CMD := ./mvnw
    JAVA_CMD := java
    AI_UP_CMD := echo "llama-server is managed by systemd on Linux." && systemctl is-active --quiet llama-server.service
    AI_DOWN_CMD := echo "Run: sudo systemctl stop llama-server.service"
    AI_HEALTH_CMD := sh ./scripts/ai/health-llama.sh
endif

.PHONY: dev build run clean check-build ai-up ai-down ai-health

dev: ai-up
	@echo "Starting server in DEV mode..."
	$(MVN_CMD) -DskipTests spring-boot:run -Dspring-boot.run.profiles=dev

ai-up:
	@$(AI_UP_CMD)

ai-down:
	@$(AI_DOWN_CMD)

ai-health:
	@$(AI_HEALTH_CMD)

build:
	@echo "Building executable JAR..."
	$(MVN_CMD) -DskipTests clean package

run: ai-up check-build
	@echo "Running packaged application with APP_ENV=$(APP_ENV)..."
	$(JAVA_CMD) -jar "$(JAR_FILE)" --spring.profiles.active=$(APP_ENV)

clean:
	@echo "Cleaning build output..."
	$(MVN_CMD) clean

check-build:
	$(if $(wildcard $(JAR_FILE)),,$(error Missing executable JAR: $(JAR_FILE). Run 'make build' first))

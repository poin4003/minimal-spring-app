-include .env
.EXPORT_ALL_VARIABLES:

APP_ENV ?= dev
JAR_FILE := target/spring-application-0.0.1-SNAPSHOT.jar

ifeq ($(OS),Windows_NT)
    SHELL := cmd.exe
    MVN_CMD := mvn
    JAVA_CMD := java
else
    MVN_CMD := ./mvnw
    JAVA_CMD := java
endif

.PHONY: dev build run clean check-build

dev:
	@echo "Starting server in DEV mode..."
	$(MVN_CMD) -DskipTests clean spring-boot:run -Dspring-boot.run.profiles=dev

build:
	@echo "Building executable JAR..."
	$(MVN_CMD) -DskipTests clean package

run: check-build
	@echo "Running packaged application with APP_ENV=$(APP_ENV)..."
	$(JAVA_CMD) -jar "$(JAR_FILE)" --spring.profiles.active=$(APP_ENV)

clean:
	@echo "Cleaning build output..."
	$(MVN_CMD) clean

check-build:
	$(if $(wildcard $(JAR_FILE)),,$(error Missing executable JAR: $(JAR_FILE). Run 'make build' first))

-include .env

APP_ENV ?= dev
MAIN_CLASS := com.app.Application

ifeq ($(OS),Windows_NT)
    SHELL := cmd.exe
    MVN_CMD := mvnw.cmd
    JAVA_CMD := java
    RUNTIME_CP := target\classes;target\dependency\*
else
    MVN_CMD := ./mvnw
    JAVA_CMD := java
    RUNTIME_CP := target/classes:target/dependency/*
endif

PROFILE_FILE := src/main/resources/application-$(APP_ENV).yaml

.PHONY: dev build run clean check-profile

dev:
	@echo "Starting server in DEV mode..."
	$(MVN_CMD) -DskipTests clean spring-boot:run -Dspring-boot.run.profiles=dev

build:
	@echo "Building compiled output..."
	$(MVN_CMD) -DskipTests clean compile dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target/dependency

run: check-profile
	@echo "Running compiled output with APP_ENV=$(APP_ENV)..."
	$(JAVA_CMD) -cp "$(RUNTIME_CP)" $(MAIN_CLASS) --spring.profiles.active=$(APP_ENV)

clean:
	@echo "Cleaning build output..."
	$(MVN_CMD) clean

check-profile:
	$(if $(wildcard $(PROFILE_FILE)),,$(error Missing profile file: $(PROFILE_FILE)))

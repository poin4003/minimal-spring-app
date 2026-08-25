-include .env
.EXPORT_ALL_VARIABLES:

APP_ENV ?= dev
JAR_FILE := target/spring-application-0.0.1-SNAPSHOT.jar
JAVA_PREVIEW_ARGS := --add-modules jdk.incubator.vector --enable-preview
JLAMA_SETUP_MAIN := com.app.features.ai.setup.JlamaModelSetup
VISION_SETUP_MAIN := com.app.features.ai.vision.setup.ClipVisionModelSetup
MAVEN_OPTS := $(strip $(MAVEN_OPTS) $(JAVA_PREVIEW_ARGS))

ifeq ($(OS),Windows_NT)
    SHELL := cmd.exe
    MVN_CMD := mvn
    JAVA_CMD := java
else
    MVN_CMD := ./mvnw
    JAVA_CMD := java
endif

.PHONY: dev build run clean check-build ai-setup jlama-setup vision-setup

dev:
	@echo "Starting server in DEV mode..."
	$(MVN_CMD) -DskipTests -Dspring-boot.run.profiles=dev -Dspring-boot.run.optimizedLaunch=false spring-boot:run

ai-setup:
ifneq ($(filter true TRUE 1 yes YES,$(POST_AI_MODERATION_ENABLED)),)
	$(MAKE) jlama-setup
else
	@echo "Skipping Jlama setup because POST_AI_MODERATION_ENABLED is not true."
endif
ifneq ($(filter true TRUE 1 yes YES,$(AI_VISION_ENABLED)),)
	$(MAKE) vision-setup
else
	@echo "Skipping vision setup because AI_VISION_ENABLED is not true."
endif
	@echo "Optional AI model setup complete."

jlama-setup:
	@echo "Downloading the optional Jlama model..."
	$(MVN_CMD) -DskipTests compile exec:java -Dexec.mainClass=$(JLAMA_SETUP_MAIN)

vision-setup:
	@echo "Downloading and validating the optional CLIP ONNX model..."
	$(MVN_CMD) -DskipTests compile exec:java -Dexec.mainClass=$(VISION_SETUP_MAIN)

build:
	@echo "Building executable JAR..."
	$(MVN_CMD) -DskipTests clean package

run: check-build
	@echo "Running packaged application with APP_ENV=$(APP_ENV)..."
	$(JAVA_CMD) $(JAVA_PREVIEW_ARGS) -jar "$(JAR_FILE)" --spring.profiles.active=$(APP_ENV)

clean:
	@echo "Cleaning build output..."
	$(MVN_CMD) clean

check-build:
	$(if $(wildcard $(JAR_FILE)),,$(error Missing executable JAR: $(JAR_FILE). Run 'make build' first))

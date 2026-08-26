-include .env
.EXPORT_ALL_VARIABLES:

APP_ENV ?= dev
JAR_FILE := target/spring-application-0.0.1-SNAPSHOT.jar
JAVA_PREVIEW_ARGS := --add-modules jdk.incubator.vector --enable-preview
JLAMA_SETUP_MAIN := com.app.features.ai.setup.JlamaModelSetup
EMBEDDING_SETUP_MAIN := com.app.features.ai.embedding.setup.MultilingualE5ModelSetup
VISION_SETUP_MAIN := com.app.features.ai.vision.setup.ClipVisionModelSetup
AI_BENCHMARK_MAIN := com.app.features.ai.benchmark.AiBenchmarkRunner
AI_BENCHMARK_OUTPUT_DIRECTORY ?= data/benchmarks
AI_BENCHMARK_EMBEDDING_WARMUP ?= 3
AI_BENCHMARK_EMBEDDING_ITERATIONS ?= 20
AI_BENCHMARK_VISION_WARMUP ?= 3
AI_BENCHMARK_VISION_ITERATIONS ?= 10
AI_BENCHMARK_JLAMA_WARMUP ?= 1
AI_BENCHMARK_JLAMA_ITERATIONS ?= 3
AI_BENCHMARK_JLAMA_MAX_TOKENS ?= 32
MAVEN_OPTS := $(strip $(MAVEN_OPTS) $(JAVA_PREVIEW_ARGS))

ifeq ($(OS),Windows_NT)
    SHELL := cmd.exe
    MVN_CMD := mvn
    JAVA_CMD := java
else
    MVN_CMD := ./mvnw
    JAVA_CMD := java
endif

.PHONY: dev build run clean check-build ai-setup jlama-setup embedding-setup vision-setup ai-benchmark benchmark-embedding benchmark-vision benchmark-jlama

dev:
	@echo "Starting server in DEV mode..."
	$(MVN_CMD) -DskipTests -Dspring-boot.run.profiles=dev -Dspring-boot.run.optimizedLaunch=false spring-boot:run

ai-setup:
ifneq ($(filter true TRUE 1 yes YES,$(POST_AI_MODERATION_ENABLED)),)
	$(MAKE) jlama-setup
else
	@echo "Skipping Jlama setup because POST_AI_MODERATION_ENABLED is not true."
endif
ifneq ($(filter true TRUE 1 yes YES,$(AI_EMBEDDING_ENABLED)),)
	$(MAKE) embedding-setup
else
	@echo "Skipping embedding setup because AI_EMBEDDING_ENABLED is not true."
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

embedding-setup:
	@echo "Downloading and validating the optional multilingual E5 ONNX model..."
	$(MVN_CMD) -DskipTests compile exec:java -Dexec.mainClass=$(EMBEDDING_SETUP_MAIN)

vision-setup:
	@echo "Downloading and validating the optional CLIP ONNX model..."
	$(MVN_CMD) -DskipTests compile exec:java -Dexec.mainClass=$(VISION_SETUP_MAIN)

ai-benchmark:
	$(MAKE) benchmark-embedding
	$(MAKE) benchmark-vision
	$(MAKE) benchmark-jlama
	@echo "AI benchmark reports are available in $(AI_BENCHMARK_OUTPUT_DIRECTORY)."

benchmark-embedding:
	@echo "Benchmarking multilingual E5 in an isolated JVM..."
	$(MVN_CMD) -DskipTests compile exec:java -Dexec.mainClass=$(AI_BENCHMARK_MAIN) "-Dexec.args=--capability=embedding --output=$(AI_BENCHMARK_OUTPUT_DIRECTORY)/embedding.json --warmup=$(AI_BENCHMARK_EMBEDDING_WARMUP) --iterations=$(AI_BENCHMARK_EMBEDDING_ITERATIONS)"

benchmark-vision:
	@echo "Benchmarking CLIP ONNX in an isolated JVM..."
	$(MVN_CMD) -DskipTests compile exec:java -Dexec.mainClass=$(AI_BENCHMARK_MAIN) "-Dexec.args=--capability=vision --output=$(AI_BENCHMARK_OUTPUT_DIRECTORY)/vision.json --warmup=$(AI_BENCHMARK_VISION_WARMUP) --iterations=$(AI_BENCHMARK_VISION_ITERATIONS)"

benchmark-jlama:
	@echo "Benchmarking Jlama in an isolated JVM..."
	$(MVN_CMD) -DskipTests compile exec:java -Dexec.mainClass=$(AI_BENCHMARK_MAIN) "-Dexec.args=--capability=jlama --output=$(AI_BENCHMARK_OUTPUT_DIRECTORY)/jlama.json --warmup=$(AI_BENCHMARK_JLAMA_WARMUP) --iterations=$(AI_BENCHMARK_JLAMA_ITERATIONS) --max-tokens=$(AI_BENCHMARK_JLAMA_MAX_TOKENS)"

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

package com.app.features.ai.runtime;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import com.app.config.settings.AppProperties;
import com.app.features.ai.exceptions.JlamaRuntimeException;
import com.app.features.ai.generation.schema.model.AiTextGenerationRequest;
import com.app.features.ai.generation.schema.model.AiTextGenerationResult;
import com.app.features.ai.generation.service.AiTextGenerationClient;
import com.app.features.ai.generation.service.AiTextGenerationStreamObserver;
import com.app.features.ai.generation.service.AiTextTokenCounter;
import com.app.features.ai.generation.service.impl.NoOpAiTextGenerationStreamObserver;
import com.github.tjake.jlama.model.AbstractModel;
import com.github.tjake.jlama.model.ModelSupport;
import com.github.tjake.jlama.model.functions.Generator;
import com.github.tjake.jlama.safetensors.DType;
import com.github.tjake.jlama.safetensors.SafeTensorSupport;
import com.github.tjake.jlama.safetensors.prompt.PromptContext;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Validated
@ConditionalOnProperty(
        prefix = "app.ai.generation",
        name = "enabled",
        havingValue = "true")
public class JlamaRuntime
        implements AiTextGenerationClient, AiTextTokenCounter {

    private final AppProperties.AiGenerationMachine machine;
    private final Semaphore inferencePermits;
    private final ReentrantReadWriteLock lifecycleLock =
            new ReentrantReadWriteLock();

    private volatile AbstractModel model;

    public JlamaRuntime(AppProperties appProperties) {
        this.machine = appProperties.getAi()
                .getGeneration()
                .getMachine();
        this.inferencePermits = new Semaphore(
                machine.getMaxConcurrency(),
                true);
    }

    @PostConstruct
    public void start() {
        lifecycleLock.writeLock().lock();
        try {
            Path modelPath = resolveLocalModelPath();
            validateLocalModel(modelPath);

            log.info(
                    "Loading Jlama model [{}] from [{}] with [{}] threads.",
                    machine.getModelId(),
                    modelPath,
                    machine.getThreads());

            model = ModelSupport.loadModel(
                    modelPath.toFile(),
                    null,
                    DType.F32,
                    DType.I8,
                    Optional.empty(),
                    Optional.of(machine.getThreads()));

            log.info("Jlama model [{}] is ready.", machine.getModelId());
        } catch (RuntimeException exception) {
            model = null;
            log.error(
                    "Unable to initialize Jlama model [{}]. "
                            + "AI text generation remains unavailable.",
                    machine.getModelId(),
                    exception);
        } finally {
            lifecycleLock.writeLock().unlock();
        }
    }

    @Override
    public boolean isReady() {
        return model != null;
    }

    @Override
    public String getModelId() {
        return machine.getModelId();
    }

    @Override
    public int countTokens(String text) {
        lifecycleLock.readLock().lock();
        try {
            AbstractModel currentModel = model;
            if (currentModel == null) {
                throw new JlamaRuntimeException(
                        "Jlama runtime is not ready.");
            }
            return currentModel.getTokenizer().encode(text).length;
        } catch (JlamaRuntimeException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new JlamaRuntimeException(
                    "Jlama token counting failed.",
                    exception);
        } finally {
            lifecycleLock.readLock().unlock();
        }
    }

    @Override
    public AiTextGenerationResult generate(
            AiTextGenerationRequest request) {
        return generate(
                request,
                NoOpAiTextGenerationStreamObserver.INSTANCE);
    }

    @Override
    public AiTextGenerationResult generate(
            AiTextGenerationRequest request,
            AiTextGenerationStreamObserver streamObserver) {
        ensureNotCancelled(streamObserver);
        acquireInferencePermit();
        lifecycleLock.readLock().lock();

        try {
            ensureNotCancelled(streamObserver);
            AbstractModel currentModel = model;
            if (currentModel == null) {
                throw new JlamaRuntimeException(
                        "Jlama runtime is not ready.");
            }

            PromptContext promptContext = buildPromptContext(
                    currentModel,
                    request.systemPrompt(),
                    request.userPrompt());
            int totalTokenLimit = resolveTotalTokenLimit(
                    currentModel,
                    promptContext,
                    request.maxOutputTokens());
            Generator.Response response = currentModel.generate(
                    UUID.randomUUID(),
                    promptContext,
                    request.temperature(),
                    totalTokenLimit,
                    (token, probability) -> emitToken(
                            streamObserver,
                            token));

            log.info(
                    "Jlama generation completed with [{}] prompt tokens and "
                            + "[{}] generated tokens in [{}] ms prompt time "
                            + "and [{}] ms generation time; finish reason [{}].",
                    response.promptTokens,
                    response.generatedTokens,
                    response.promptTimeMs,
                    response.generateTimeMs,
                    response.finishReason);

            return new AiTextGenerationResult(
                    response.responseText,
                    response.promptTokens,
                    response.generatedTokens,
                    response.promptTimeMs,
                    response.generateTimeMs,
                    response.finishReason.name());
        } catch (CancellationException | JlamaRuntimeException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new JlamaRuntimeException(
                    "Jlama inference failed.",
                    exception);
        } finally {
            lifecycleLock.readLock().unlock();
            inferencePermits.release();
        }
    }

    @PreDestroy
    public void close() {
        lifecycleLock.writeLock().lock();
        try {
            AbstractModel currentModel = model;
            model = null;

            if (currentModel != null) {
                currentModel.close();
                log.info("Jlama model [{}] was closed.", machine.getModelId());
            }
        } finally {
            lifecycleLock.writeLock().unlock();
        }
    }

    private void acquireInferencePermit() {
        try {
            boolean acquired = inferencePermits.tryAcquire(
                    machine.getTimeout().toMillis(),
                    TimeUnit.MILLISECONDS);
            if (!acquired) {
                throw new JlamaRuntimeException(
                        "Timed out waiting for an available Jlama "
                                + "inference slot.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new JlamaRuntimeException(
                    "Interrupted while waiting for Jlama inference.",
                    exception);
        }
    }

    private void emitToken(
            AiTextGenerationStreamObserver streamObserver,
            String token) {
        ensureNotCancelled(streamObserver);
        if (token != null && !token.isEmpty()) {
            streamObserver.onToken(token);
        }
    }

    private void ensureNotCancelled(
            AiTextGenerationStreamObserver streamObserver) {
        if (streamObserver.isCancelled()) {
            throw new CancellationException(
                    "AI text generation was cancelled.");
        }
    }

    private PromptContext buildPromptContext(
            AbstractModel currentModel,
            String systemPrompt,
            String userPrompt) {
        return currentModel.promptSupport()
                .map(promptSupport -> promptSupport.builder()
                        .addSystemMessage(systemPrompt)
                        .addUserMessage(userPrompt)
                        .build())
                .orElseGet(() -> PromptContext.of("""
                        SYSTEM:
                        %s

                        USER:
                        %s
                        """.formatted(systemPrompt, userPrompt)));
    }

    private int resolveTotalTokenLimit(
            AbstractModel currentModel,
            PromptContext promptContext,
            int maxOutputTokens) {
        int promptTokenCount = currentModel.getTokenizer()
                .encode(promptContext.getPrompt())
                .length;
        int contextLength = currentModel.getConfig().contextLength;

        if (promptTokenCount >= contextLength) {
            throw new JlamaRuntimeException(
                    "Jlama prompt exceeds the model context length.");
        }

        long requestedTokenLimit = (long) promptTokenCount + maxOutputTokens;
        return (int) Math.min(requestedTokenLimit, contextLength);
    }

    private Path resolveLocalModelPath() {
        String[] modelCoordinates = machine.getModelId().split("/", -1);
        if (modelCoordinates.length != 2
                || modelCoordinates[0].isBlank()
                || modelCoordinates[1].isBlank()) {
            throw new JlamaRuntimeException(
                    "Jlama model ID must use owner/name format.");
        }

        Path modelRoot = Path.of(machine.getModelDirectory())
                .toAbsolutePath()
                .normalize();
        Path modelPath = SafeTensorSupport.constructLocalModelPath(
                        modelRoot.toString(),
                        modelCoordinates[0],
                        modelCoordinates[1])
                .toAbsolutePath()
                .normalize();

        if (!modelPath.startsWith(modelRoot)) {
            throw new JlamaRuntimeException(
                    "Resolved Jlama model path is outside the model directory.");
        }

        return modelPath;
    }

    private void validateLocalModel(Path modelPath) {
        if (!Files.isDirectory(modelPath)
                || !Files.isRegularFile(modelPath.resolve("config.json"))
                || !Files.isRegularFile(modelPath.resolve("tokenizer.json"))
                || !SafeTensorSupport.isModelLocal(modelPath)) {
            throw new JlamaRuntimeException(
                    "Jlama model is missing or incomplete at: " + modelPath
                            + ". Run 'make ai-setup' first.");
        }
    }
}

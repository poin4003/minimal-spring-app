package com.app.features.ai.runtime;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.app.config.settings.AppProperties;
import com.app.features.ai.exceptions.JlamaRuntimeException;
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
@ConditionalOnProperty(
        prefix = "app.post.ai-moderation",
        name = "enabled",
        havingValue = "true")
public class JlamaRuntime {

    private final AppProperties.AiModerationMachine machine;
    private final Semaphore inferencePermits;
    private final ReentrantReadWriteLock lifecycleLock =
            new ReentrantReadWriteLock();

    private volatile AbstractModel model;

    public JlamaRuntime(AppProperties appProperties) {
        this.machine = appProperties.getPost()
                .getAiModeration()
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
                            + "AI remains unavailable and manual moderation "
                            + "can continue.",
                    machine.getModelId(),
                    exception);
        } finally {
            lifecycleLock.writeLock().unlock();
        }
    }

    public boolean isReady() {
        return model != null;
    }

    public String getModelId() {
        return machine.getModelId();
    }

    public String generate(
            String systemPrompt,
            String userPrompt,
            float temperature,
            int maxTokens) {
        acquireInferencePermit();
        lifecycleLock.readLock().lock();

        try {
            AbstractModel currentModel = model;
            if (currentModel == null) {
                throw new JlamaRuntimeException(
                        "Jlama runtime is not ready.");
            }

            PromptContext promptContext = buildPromptContext(
                    currentModel,
                    systemPrompt,
                    userPrompt);
            int totalTokenLimit = resolveTotalTokenLimit(
                    currentModel,
                    promptContext,
                    maxTokens);
            Generator.Response response = currentModel.generate(
                    UUID.randomUUID(),
                    promptContext,
                    temperature,
                    totalTokenLimit);

            return response.responseText;
        } catch (JlamaRuntimeException exception) {
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

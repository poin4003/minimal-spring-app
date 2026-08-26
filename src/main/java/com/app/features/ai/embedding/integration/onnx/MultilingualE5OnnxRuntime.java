package com.app.features.ai.embedding.integration.onnx;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.app.config.settings.AppProperties;
import com.app.features.ai.embedding.exceptions.AiEmbeddingRuntimeException;
import com.app.features.ai.embedding.service.AiEmbeddingClient;
import com.app.features.ai.embedding.service.AiEmbeddingHealthClient;
import com.app.features.ai.embedding.support.MultilingualE5ModelContract;
import com.app.features.ai.onnx.schema.model.OnnxModelArtifact;
import com.app.features.ai.onnx.runtime.OnnxSessionResource;
import com.app.features.ai.onnx.support.OnnxModelFiles;
import com.app.features.ai.onnx.support.OnnxSessionFactory;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(
        prefix = "app.ai.embedding",
        name = "enabled",
        havingValue = "true")
public class MultilingualE5OnnxRuntime
        implements AiEmbeddingClient, AiEmbeddingHealthClient {

    private final OnnxModelArtifact modelArtifact;
    private final OnnxModelArtifact tokenizerArtifact;
    private final AppProperties.EmbeddingMachine machine;
    private final AppProperties.OnnxSettings onnxSettings;
    private final OrtEnvironment environment;
    private final Semaphore inferencePermits;
    private final ReadWriteLock lifecycleLock =
            new ReentrantReadWriteLock();

    private volatile RuntimeResources resources;

    public MultilingualE5OnnxRuntime(AppProperties appProperties) {
        AppProperties.EmbeddingSettings embedding =
                appProperties.getAi().getEmbedding();
        AppProperties.EmbeddingModel model = embedding.getModel();
        this.modelArtifact = new OnnxModelArtifact(
                model.getId(),
                model.getRevision(),
                model.getSha256(),
                model.getFile());
        this.tokenizerArtifact = new OnnxModelArtifact(
                model.getId(),
                model.getRevision(),
                model.getTokenizerSha256(),
                model.getTokenizerFile());
        this.machine = embedding.getMachine();
        this.onnxSettings = appProperties.getAi().getOnnx();
        this.environment = OrtEnvironment.getEnvironment();
        this.inferencePermits = new Semaphore(
                machine.getMaxConcurrency(),
                true);
    }

    @PostConstruct
    public void start() {
        OnnxSessionResource initializedSession = null;
        HuggingFaceTokenizer initializedTokenizer = null;
        try {
            Path modelPath = resolveLocalArtifactPath(modelArtifact);
            Path tokenizerPath = resolveLocalArtifactPath(tokenizerArtifact);
            validateLocalArtifact(modelPath, modelArtifact, "model");
            validateLocalArtifact(
                    tokenizerPath,
                    tokenizerArtifact,
                    "tokenizer");

            log.info(
                    "Loading multilingual E5 ONNX model [{}@{}] from [{}] "
                            + "with [{}] threads.",
                    modelArtifact.id(),
                    modelArtifact.revision(),
                    modelPath,
                    machine.getThreads());
            initializedTokenizer =
                    MultilingualE5ModelContract.createTokenizer(tokenizerPath);
            initializedSession = OnnxSessionFactory.create(
                    environment,
                    modelPath.toString(),
                    machine,
                    onnxSettings);
            MultilingualE5ModelContract.validateAndRunSmokeInference(
                    environment,
                    initializedSession.session(),
                    initializedTokenizer);

            lifecycleLock.writeLock().lock();
            try {
                resources = new RuntimeResources(
                        initializedSession,
                        initializedTokenizer);
            } finally {
                lifecycleLock.writeLock().unlock();
            }
            initializedSession = null;
            initializedTokenizer = null;

            log.info(
                    "Multilingual E5 ONNX model [{}@{}] is ready on [{}].",
                    modelArtifact.id(),
                    modelArtifact.revision(),
                    resources.sessionResource().executionProvider());
        } catch (Exception | LinkageError exception) {
            closeQuietly(initializedSession, initializedTokenizer);
            resources = null;
            log.error(
                    "Unable to initialize multilingual E5 ONNX model [{}@{}]. "
                            + "AI embedding remains unavailable.",
                    modelArtifact.id(),
                    modelArtifact.revision(),
                    exception);
        }
    }

    @Override
    public float[] embedQuery(String text) {
        return execute(resources -> MultilingualE5ModelContract.embedQuery(
                environment,
                resources.sessionResource().session(),
                resources.tokenizer(),
                text));
    }

    @Override
    public float[] embedPassage(String text) {
        return execute(resources -> MultilingualE5ModelContract.embedPassage(
                environment,
                resources.sessionResource().session(),
                resources.tokenizer(),
                text));
    }

    @Override
    public int getDimension() {
        return MultilingualE5ModelContract.EMBEDDING_DIMENSION;
    }

    @Override
    public String getModelVersion() {
        return modelArtifact.id() + "@" + modelArtifact.revision();
    }

    @Override
    public boolean isReady() {
        return resources != null;
    }

    @Override
    public String getRuntimeProvider() {
        RuntimeResources currentResources = resources;
        return currentResources == null
                ? "UNAVAILABLE"
                : currentResources.sessionResource()
                        .executionProvider()
                        .name();
    }

    @PreDestroy
    public void close() {
        RuntimeResources currentResources;
        lifecycleLock.writeLock().lock();
        try {
            currentResources = resources;
            resources = null;
        } finally {
            lifecycleLock.writeLock().unlock();
        }

        if (currentResources != null) {
            closeQuietly(
                    currentResources.sessionResource(),
                    currentResources.tokenizer());
            log.info(
                    "Multilingual E5 ONNX model [{}@{}] was closed.",
                    modelArtifact.id(),
                    modelArtifact.revision());
        }
    }

    private float[] execute(EmbeddingOperation operation) {
        acquirePermit();
        lifecycleLock.readLock().lock();
        try {
            RuntimeResources currentResources = resources;
            if (currentResources == null) {
                throw new AiEmbeddingRuntimeException(
                        "AI embedding runtime is unavailable.");
            }
            return operation.execute(currentResources);
        } catch (OrtException exception) {
            throw new AiEmbeddingRuntimeException(
                    "Multilingual E5 ONNX inference failed.",
                    exception);
        } finally {
            lifecycleLock.readLock().unlock();
            inferencePermits.release();
        }
    }

    private void acquirePermit() {
        try {
            inferencePermits.acquire();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AiEmbeddingRuntimeException(
                    "Interrupted while waiting for AI embedding capacity.",
                    exception);
        }
    }

    private Path resolveLocalArtifactPath(OnnxModelArtifact artifact) {
        return OnnxModelFiles.resolveModelPath(
                machine.getModelDirectory(),
                artifact);
    }

    private void validateLocalArtifact(
            Path artifactPath,
            OnnxModelArtifact artifact,
            String artifactType) throws IOException {
        if (!Files.isRegularFile(artifactPath)) {
            throw new AiEmbeddingRuntimeException(
                    "Multilingual E5 "
                            + artifactType
                            + " is missing at: "
                            + artifactPath
                            + ". Run the AI embedding setup first.");
        }
        OnnxModelFiles.verifySha256(
                artifactPath,
                artifact.sha256());
    }

    private void closeQuietly(
            OnnxSessionResource sessionResource,
            HuggingFaceTokenizer tokenizer) {
        if (sessionResource != null) {
            try {
                sessionResource.close();
            } catch (OrtException | RuntimeException exception) {
                log.warn(
                        "Unable to close multilingual E5 ONNX session cleanly.",
                        exception);
            }
        }
        if (tokenizer != null) {
            try {
                tokenizer.close();
            } catch (RuntimeException exception) {
                log.warn(
                        "Unable to close multilingual E5 tokenizer cleanly.",
                        exception);
            }
        }
    }

    @FunctionalInterface
    private interface EmbeddingOperation {

        float[] execute(RuntimeResources resources) throws OrtException;
    }

    private record RuntimeResources(
            OnnxSessionResource sessionResource,
            HuggingFaceTokenizer tokenizer) {
    }
}

package com.app.features.ai.vision.integration.onnx;

import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.app.config.settings.AppProperties;
import com.app.features.ai.onnx.schema.model.OnnxModelArtifact;
import com.app.features.ai.onnx.runtime.OnnxSessionResource;
import com.app.features.ai.onnx.support.OnnxModelFiles;
import com.app.features.ai.onnx.support.OnnxSessionFactory;
import com.app.features.ai.vision.exceptions.AiVisionRuntimeException;
import com.app.features.ai.vision.service.AiVisionHealthClient;
import com.app.features.ai.vision.support.ClipVisionModelContract;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(
        prefix = "app.ai.vision",
        name = "enabled",
        havingValue = "true")
public class ClipOnnxRuntime implements AiVisionHealthClient {

    private final OnnxModelArtifact artifact;
    private final AppProperties.VisionMachine machine;
    private final AppProperties.OnnxSettings onnxSettings;

    private volatile OnnxSessionResource sessionResource;

    public ClipOnnxRuntime(AppProperties appProperties) {
        AppProperties.VisionSettings vision =
                appProperties.getAi().getVision();
        AppProperties.VisionModel model = vision.getModel();
        this.artifact = new OnnxModelArtifact(
                model.getId(),
                model.getRevision(),
                model.getSha256(),
                model.getFile());
        this.machine = vision.getMachine();
        this.onnxSettings = appProperties.getAi().getOnnx();
    }

    @PostConstruct
    public void start() {
        OnnxSessionResource initializedSession = null;
        try {
            Path modelPath = resolveLocalModelPath();
            validateLocalModel(modelPath);

            log.info(
                    "Loading CLIP ONNX model [{}@{}] from [{}] with [{}] threads.",
                    artifact.id(),
                    artifact.revision(),
                    modelPath,
                    machine.getThreads());

            OrtEnvironment environment = OrtEnvironment.getEnvironment();
            initializedSession = OnnxSessionFactory.create(
                    environment,
                    modelPath.toString(),
                    machine,
                    onnxSettings);
            ClipVisionModelContract.validateAndRunSmokeInference(
                    environment,
                    initializedSession.session());
            sessionResource = initializedSession;

            log.info(
                    "CLIP ONNX model [{}@{}] is ready on [{}].",
                    artifact.id(),
                    artifact.revision(),
                    initializedSession.executionProvider());
        } catch (Exception | LinkageError exception) {
            closeQuietly(initializedSession);
            sessionResource = null;
            log.error(
                    "Unable to initialize CLIP ONNX model [{}@{}]. "
                            + "AI vision remains unavailable.",
                    artifact.id(),
                    artifact.revision(),
                    exception);
        }
    }

    @Override
    public boolean isReady() {
        return sessionResource != null;
    }

    @Override
    public String getRuntimeProvider() {
        OnnxSessionResource currentSession = sessionResource;
        return currentSession == null
                ? "UNAVAILABLE"
                : currentSession.executionProvider().name();
    }

    @PreDestroy
    public void close() {
        OnnxSessionResource currentSession = sessionResource;
        sessionResource = null;
        closeQuietly(currentSession);
        if (currentSession != null) {
            log.info(
                    "CLIP ONNX model [{}@{}] was closed.",
                    artifact.id(),
                    artifact.revision());
        }
    }

    private Path resolveLocalModelPath() {
        return OnnxModelFiles.resolveModelPath(
                machine.getModelDirectory(),
                artifact);
    }

    private void validateLocalModel(Path modelPath) throws java.io.IOException {
        if (!Files.isRegularFile(modelPath)) {
            throw new AiVisionRuntimeException(
                    "CLIP ONNX model is missing at: " + modelPath
                            + ". Run the AI vision setup first.");
        }
        OnnxModelFiles.verifySha256(
                modelPath,
                artifact.sha256());
    }

    private void closeQuietly(OnnxSessionResource targetSession) {
        if (targetSession == null) {
            return;
        }

        try {
            targetSession.close();
        } catch (OrtException | RuntimeException exception) {
            log.warn("Unable to close CLIP ONNX session cleanly.", exception);
        }
    }
}

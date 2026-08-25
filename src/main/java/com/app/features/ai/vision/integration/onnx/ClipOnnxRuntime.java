package com.app.features.ai.vision.integration.onnx;

import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.app.config.settings.AppProperties;
import com.app.features.ai.vision.exceptions.AiVisionRuntimeException;
import com.app.features.ai.onnx.schema.model.OnnxModelArtifact;
import com.app.features.ai.onnx.support.OnnxModelFiles;
import com.app.features.ai.onnx.support.OnnxSessionOptionsFactory;
import com.app.features.ai.vision.service.AiVisionHealthClient;
import com.app.features.ai.vision.support.ClipVisionModelContract;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

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

    private volatile OrtSession session;

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
    }

    @PostConstruct
    public void start() {
        OrtSession initializedSession = null;
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
            try (OrtSession.SessionOptions options =
                    OnnxSessionOptionsFactory.create(machine.getThreads())) {
                initializedSession = environment.createSession(
                        modelPath.toString(),
                        options);
            }
            ClipVisionModelContract.validateAndRunSmokeInference(
                    environment,
                    initializedSession);
            session = initializedSession;

            log.info(
                    "CLIP ONNX model [{}@{}] is ready.",
                    artifact.id(),
                    artifact.revision());
        } catch (Exception | LinkageError exception) {
            closeQuietly(initializedSession);
            session = null;
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
        return session != null;
    }

    @PreDestroy
    public void close() {
        OrtSession currentSession = session;
        session = null;
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

    private void closeQuietly(OrtSession targetSession) {
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

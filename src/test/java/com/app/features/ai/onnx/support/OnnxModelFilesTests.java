package com.app.features.ai.onnx.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.app.features.ai.onnx.schema.model.OnnxModelArtifact;

class OnnxModelFilesTests {

    private static final String MODEL_REVISION =
            "12b36594d53414ecfba93c7200dbb7c7db3c900a";
    private static final String MODEL_SHA256 =
            "57879bb1c23cdeb350d23569dd251ed4b740a96d747c529e94a2bb8040ac5d00";

    @TempDir
    private Path temporaryDirectory;

    @Test
    void resolvesModelInsidePinnedRevisionDirectory() {
        Path modelPath = OnnxModelFiles.resolveModelPath(
                temporaryDirectory.toString(),
                artifact("onnx/model.onnx"));

        assertThat(modelPath).isEqualTo(temporaryDirectory
                .resolve("openai")
                .resolve("clip-vit-base-patch32")
                .resolve(MODEL_REVISION)
                .resolve("onnx")
                .resolve("model.onnx")
                .toAbsolutePath()
                .normalize());
    }

    @Test
    void rejectsModelPathOutsideRevisionDirectory() {
        assertThatThrownBy(() -> OnnxModelFiles.resolveModelPath(
                temporaryDirectory.toString(),
                artifact("../model.onnx")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("safe relative path");
    }

    @Test
    void verifiesExpectedChecksum() throws Exception {
        Path model = Files.writeString(
                temporaryDirectory.resolve("model.onnx"),
                "clip-model");
        String checksum = OnnxModelFiles.calculateSha256(model);

        OnnxModelFiles.verifySha256(model, checksum);
    }

    @Test
    void rejectsUnexpectedChecksum() throws Exception {
        Path model = Files.writeString(
                temporaryDirectory.resolve("model.onnx"),
                "clip-model");

        assertThatThrownBy(() -> OnnxModelFiles.verifySha256(
                model,
                "0000000000000000000000000000000000000000000000000000000000000000"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("checksum does not match");
    }

    private OnnxModelArtifact artifact(String modelFile) {
        return new OnnxModelArtifact(
                "openai/clip-vit-base-patch32",
                MODEL_REVISION,
                MODEL_SHA256,
                modelFile);
    }
}

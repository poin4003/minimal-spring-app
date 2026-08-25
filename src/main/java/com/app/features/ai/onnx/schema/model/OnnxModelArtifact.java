package com.app.features.ai.onnx.schema.model;

public record OnnxModelArtifact(
        String id,
        String revision,
        String sha256,
        String file) {
}

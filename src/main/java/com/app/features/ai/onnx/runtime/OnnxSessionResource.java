package com.app.features.ai.onnx.runtime;

import com.app.features.ai.onnx.enums.OnnxExecutionProvider;

import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.providers.OrtCUDAProviderOptions;

public final class OnnxSessionResource implements AutoCloseable {

    private final OrtSession session;
    private final OnnxExecutionProvider executionProvider;
    private final OrtCUDAProviderOptions cudaProviderOptions;

    public OnnxSessionResource(
            OrtSession session,
            OnnxExecutionProvider executionProvider,
            OrtCUDAProviderOptions cudaProviderOptions) {
        this.session = session;
        this.executionProvider = executionProvider;
        this.cudaProviderOptions = cudaProviderOptions;
    }

    public OrtSession session() {
        return session;
    }

    public OnnxExecutionProvider executionProvider() {
        return executionProvider;
    }

    @Override
    public void close() throws OrtException {
        try {
            session.close();
        } finally {
            if (cudaProviderOptions != null) {
                cudaProviderOptions.close();
            }
        }
    }
}

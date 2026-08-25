package com.app.features.ai.onnx.support;

import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

public final class OnnxSessionOptionsFactory {

    private OnnxSessionOptionsFactory() {
    }

    public static OrtSession.SessionOptions create(int threads)
            throws OrtException {
        if (threads <= 0) {
            throw new IllegalArgumentException(
                    "ONNX runtime threads must be greater than zero.");
        }

        OrtSession.SessionOptions options = new OrtSession.SessionOptions();
        options.setIntraOpNumThreads(threads);
        options.setInterOpNumThreads(1);
        options.setOptimizationLevel(
                OrtSession.SessionOptions.OptLevel.ALL_OPT);
        return options;
    }
}

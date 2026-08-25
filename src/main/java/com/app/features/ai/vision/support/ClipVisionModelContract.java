package com.app.features.ai.vision.support;

import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.app.features.ai.vision.exceptions.AiVisionRuntimeException;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

public final class ClipVisionModelContract {

    private static final int CLIP_SEQUENCE_LENGTH = 77;
    private static final int CLIP_IMAGE_SIZE = 224;
    private static final Set<String> EXPECTED_INPUTS = Set.of(
            "input_ids",
            "attention_mask",
            "pixel_values");
    private static final Set<String> EXPECTED_OUTPUTS = Set.of(
            "image_embeds",
            "text_embeds",
            "logits_per_image",
            "logits_per_text");

    private ClipVisionModelContract() {
    }

    public static Schema validateAndRunSmokeInference(
            OrtEnvironment environment,
            OrtSession session) throws OrtException {
        validateSchema(session);

        Map<String, OnnxTensor> inputs = createSmokeInputs(environment);
        try {
            try (OrtSession.Result result = session.run(inputs)) {
                if (result.size() < EXPECTED_OUTPUTS.size()) {
                    throw new AiVisionRuntimeException(
                            "CLIP ONNX smoke inference returned an incomplete result.");
                }
            }
        } finally {
            inputs.values().forEach(tensor -> tensor.close());
        }

        return new Schema(
                session.getInputNames().stream().sorted().toList(),
                session.getOutputNames().stream().sorted().toList());
    }

    private static void validateSchema(OrtSession session) {
        Set<String> inputNames = session.getInputNames();
        if (!inputNames.equals(EXPECTED_INPUTS)) {
            throw new AiVisionRuntimeException(
                    "Unsupported CLIP ONNX inputs. Expected "
                            + EXPECTED_INPUTS
                            + " but found "
                            + inputNames
                            + ".");
        }

        Set<String> outputNames = session.getOutputNames();
        if (!outputNames.containsAll(EXPECTED_OUTPUTS)) {
            throw new AiVisionRuntimeException(
                    "Unsupported CLIP ONNX outputs. Required "
                            + EXPECTED_OUTPUTS
                            + " but found "
                            + outputNames
                            + ".");
        }
    }

    private static Map<String, OnnxTensor> createSmokeInputs(
            OrtEnvironment environment) throws OrtException {
        Map<String, OnnxTensor> inputs = new LinkedHashMap<>();
        try {
            inputs.put(
                    "input_ids",
                    createTokenTensor(environment, false));
            inputs.put(
                    "attention_mask",
                    createTokenTensor(environment, true));
            inputs.put(
                    "pixel_values",
                    createPixelTensor(environment));
            return inputs;
        } catch (RuntimeException | OrtException exception) {
            inputs.values().forEach(tensor -> tensor.close());
            throw exception;
        }
    }

    private static OnnxTensor createTokenTensor(
            OrtEnvironment environment,
            boolean attentionMask) throws OrtException {
        long[] values = new long[CLIP_SEQUENCE_LENGTH];
        if (attentionMask) {
            Arrays.fill(values, 1L);
        }
        return OnnxTensor.createTensor(
                environment,
                LongBuffer.wrap(values),
                new long[] { 1, CLIP_SEQUENCE_LENGTH });
    }

    private static OnnxTensor createPixelTensor(
            OrtEnvironment environment) throws OrtException {
        int pixelCount = 3 * CLIP_IMAGE_SIZE * CLIP_IMAGE_SIZE;
        return OnnxTensor.createTensor(
                environment,
                FloatBuffer.wrap(new float[pixelCount]),
                new long[] { 1, 3, CLIP_IMAGE_SIZE, CLIP_IMAGE_SIZE });
    }

    public record Schema(
            List<String> inputNames,
            List<String> outputNames) {

        public Schema {
            inputNames = List.copyOf(inputNames);
            outputNames = List.copyOf(outputNames);
        }
    }
}

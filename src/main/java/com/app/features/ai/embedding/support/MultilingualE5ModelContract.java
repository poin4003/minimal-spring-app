package com.app.features.ai.embedding.support;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.app.features.ai.embedding.exceptions.AiEmbeddingRuntimeException;
import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.TensorInfo;

public final class MultilingualE5ModelContract {

    public static final int MAX_SEQUENCE_LENGTH = 512;
    public static final int EMBEDDING_DIMENSION = 384;

    private static final String QUERY_PREFIX = "query: ";
    private static final String PASSAGE_PREFIX = "passage: ";
    private static final String LAST_HIDDEN_STATE = "last_hidden_state";
    private static final Set<String> REQUIRED_INPUTS = Set.of(
            "input_ids",
            "attention_mask");
    private static final Set<String> SUPPORTED_INPUTS = Set.of(
            "input_ids",
            "attention_mask",
            "token_type_ids");

    private MultilingualE5ModelContract() {
    }

    public static HuggingFaceTokenizer createTokenizer(Path tokenizerPath)
            throws IOException {
        return HuggingFaceTokenizer.builder()
                .optTokenizerPath(tokenizerPath)
                .optAddSpecialTokens(true)
                .optTruncation(true)
                .optMaxLength(MAX_SEQUENCE_LENGTH)
                .build();
    }

    public static Schema validateAndRunSmokeInference(
            OrtEnvironment environment,
            OrtSession session,
            HuggingFaceTokenizer tokenizer) throws OrtException {
        validateSchema(session);
        float[] embedding = embedQuery(
                environment,
                session,
                tokenizer,
                "health check");
        if (embedding.length != EMBEDDING_DIMENSION) {
            throw new AiEmbeddingRuntimeException(
                    "Multilingual E5 smoke inference returned an invalid dimension.");
        }

        return new Schema(
                session.getInputNames().stream().sorted().toList(),
                session.getOutputNames().stream().sorted().toList(),
                embedding.length);
    }

    public static float[] embedQuery(
            OrtEnvironment environment,
            OrtSession session,
            HuggingFaceTokenizer tokenizer,
            String text) throws OrtException {
        return embed(
                environment,
                session,
                tokenizer,
                QUERY_PREFIX + requireText(text));
    }

    public static float[] embedPassage(
            OrtEnvironment environment,
            OrtSession session,
            HuggingFaceTokenizer tokenizer,
            String text) throws OrtException {
        return embed(
                environment,
                session,
                tokenizer,
                PASSAGE_PREFIX + requireText(text));
    }

    private static float[] embed(
            OrtEnvironment environment,
            OrtSession session,
            HuggingFaceTokenizer tokenizer,
            String prefixedText) throws OrtException {
        validateSchema(session);
        Encoding encoding = tokenizer.encode(prefixedText);
        validateEncoding(encoding);

        Map<String, OnnxTensor> inputs = createInputs(
                environment,
                session,
                encoding);
        try {
            try (OrtSession.Result result = session.run(inputs)) {
                OnnxValue output = result.get(LAST_HIDDEN_STATE)
                        .orElseThrow(() -> new AiEmbeddingRuntimeException(
                                "Multilingual E5 response is missing last_hidden_state."));
                if (!(output instanceof OnnxTensor tensor)) {
                    throw new AiEmbeddingRuntimeException(
                            "Multilingual E5 last_hidden_state is not a tensor.");
                }
                return meanPoolAndNormalize(
                        tensor,
                        encoding.getAttentionMask());
            }
        } finally {
            inputs.values().forEach(OnnxTensor::close);
        }
    }

    private static void validateSchema(OrtSession session) {
        Set<String> inputNames = session.getInputNames();
        if (!inputNames.containsAll(REQUIRED_INPUTS)
                || !SUPPORTED_INPUTS.containsAll(inputNames)) {
            throw new AiEmbeddingRuntimeException(
                    "Unsupported multilingual E5 ONNX inputs. Required "
                            + REQUIRED_INPUTS
                            + ", supported "
                            + SUPPORTED_INPUTS
                            + ", but found "
                            + inputNames
                            + ".");
        }

        if (!session.getOutputNames().contains(LAST_HIDDEN_STATE)) {
            throw new AiEmbeddingRuntimeException(
                    "Unsupported multilingual E5 ONNX outputs. Required "
                            + LAST_HIDDEN_STATE
                            + " but found "
                            + session.getOutputNames()
                            + ".");
        }
    }

    private static void validateEncoding(Encoding encoding) {
        long[] inputIds = encoding.getIds();
        long[] attentionMask = encoding.getAttentionMask();
        long[] typeIds = encoding.getTypeIds();
        if (inputIds.length == 0
                || inputIds.length > MAX_SEQUENCE_LENGTH
                || attentionMask.length != inputIds.length
                || typeIds.length != inputIds.length) {
            throw new AiEmbeddingRuntimeException(
                    "Tokenizer returned an invalid multilingual E5 input shape.");
        }
    }

    private static Map<String, OnnxTensor> createInputs(
            OrtEnvironment environment,
            OrtSession session,
            Encoding encoding) throws OrtException {
        Map<String, OnnxTensor> inputs = new LinkedHashMap<>();
        try {
            inputs.put(
                    "input_ids",
                    createTokenTensor(environment, encoding.getIds()));
            inputs.put(
                    "attention_mask",
                    createTokenTensor(
                            environment,
                            encoding.getAttentionMask()));
            if (session.getInputNames().contains("token_type_ids")) {
                inputs.put(
                        "token_type_ids",
                        createTokenTensor(
                                environment,
                                encoding.getTypeIds()));
            }
            return inputs;
        } catch (RuntimeException | OrtException exception) {
            inputs.values().forEach(OnnxTensor::close);
            throw exception;
        }
    }

    private static OnnxTensor createTokenTensor(
            OrtEnvironment environment,
            long[] values) throws OrtException {
        return OnnxTensor.createTensor(
                environment,
                LongBuffer.wrap(values),
                new long[] { 1, values.length });
    }

    private static float[] meanPoolAndNormalize(
            OnnxTensor lastHiddenState,
            long[] attentionMask) {
        TensorInfo tensorInfo = (TensorInfo) lastHiddenState.getInfo();
        long[] shape = tensorInfo.getShape();
        if (shape.length != 3
                || shape[0] != 1
                || shape[1] != attentionMask.length
                || shape[2] != EMBEDDING_DIMENSION) {
            throw new AiEmbeddingRuntimeException(
                    "Multilingual E5 returned an unsupported last_hidden_state shape.");
        }

        FloatBuffer values = lastHiddenState.getFloatBuffer();
        float[] pooled = new float[EMBEDDING_DIMENSION];
        int includedTokens = 0;
        for (int tokenIndex = 0;
                tokenIndex < attentionMask.length;
                tokenIndex++) {
            if (attentionMask[tokenIndex] == 0L) {
                continue;
            }
            includedTokens++;
            int tokenOffset = tokenIndex * EMBEDDING_DIMENSION;
            for (int dimension = 0;
                    dimension < EMBEDDING_DIMENSION;
                    dimension++) {
                pooled[dimension] += values.get(tokenOffset + dimension);
            }
        }
        if (includedTokens == 0) {
            throw new AiEmbeddingRuntimeException(
                    "Multilingual E5 attention mask contains no usable tokens.");
        }

        double squaredNorm = 0.0;
        for (int dimension = 0;
                dimension < EMBEDDING_DIMENSION;
                dimension++) {
            pooled[dimension] /= includedTokens;
            squaredNorm += pooled[dimension] * pooled[dimension];
        }
        double norm = Math.sqrt(squaredNorm);
        if (!Double.isFinite(norm) || norm == 0.0) {
            throw new AiEmbeddingRuntimeException(
                    "Multilingual E5 returned a zero or invalid embedding.");
        }

        for (int dimension = 0;
                dimension < EMBEDDING_DIMENSION;
                dimension++) {
            pooled[dimension] /= (float) norm;
        }
        return pooled;
    }

    private static String requireText(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(
                    "Embedding text must not be blank.");
        }
        return text.trim();
    }

    public record Schema(
            List<String> inputNames,
            List<String> outputNames,
            int embeddingDimension) {

        public Schema {
            inputNames = List.copyOf(inputNames);
            outputNames = List.copyOf(outputNames);
        }
    }
}

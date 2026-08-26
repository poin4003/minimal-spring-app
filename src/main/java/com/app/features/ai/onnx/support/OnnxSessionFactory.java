package com.app.features.ai.onnx.support;

import java.util.EnumSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.app.config.settings.AppProperties;
import com.app.features.ai.onnx.enums.OnnxExecutionProvider;
import com.app.features.ai.onnx.runtime.OnnxSessionResource;

import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtProvider;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.providers.OrtCUDAProviderOptions;

public final class OnnxSessionFactory {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(OnnxSessionFactory.class);
    private static final long BYTES_PER_MEBIBYTE = 1024L * 1024L;

    private OnnxSessionFactory() {
    }

    public static OnnxSessionResource create(
            OrtEnvironment environment,
            String modelPath,
            AppProperties.OnnxMachine machine,
            AppProperties.OnnxSettings onnxSettings) throws OrtException {
        OnnxExecutionProvider requestedProvider =
                machine.getExecutionProvider();
        if (requestedProvider == OnnxExecutionProvider.CPU) {
            return createCpu(environment, modelPath, machine.getThreads());
        }

        EnumSet<OrtProvider> availableProviders =
                OrtEnvironment.getAvailableProviders();
        if (!availableProviders.contains(OrtProvider.CUDA)) {
            return handleUnavailableCuda(
                    environment,
                    modelPath,
                    machine,
                    onnxSettings,
                    "CUDA Execution Provider is not included in this runtime.");
        }

        try {
            return createCuda(
                    environment,
                    modelPath,
                    machine.getThreads(),
                    onnxSettings.getCuda());
        } catch (OrtException | LinkageError exception) {
            if (!canFallback(requestedProvider, onnxSettings)) {
                throw exception;
            }
            LOGGER.warn(
                    "Unable to create CUDA ONNX session for [{}]; falling "
                            + "back to CPU. Cause: {}",
                    modelPath,
                    exception.getMessage());
            return createCpu(environment, modelPath, machine.getThreads());
        }
    }

    private static OnnxSessionResource handleUnavailableCuda(
            OrtEnvironment environment,
            String modelPath,
            AppProperties.OnnxMachine machine,
            AppProperties.OnnxSettings onnxSettings,
            String reason) throws OrtException {
        if (!canFallback(machine.getExecutionProvider(), onnxSettings)) {
            throw new OrtException(reason);
        }
        LOGGER.warn("{} Falling back to CPU for [{}].", reason, modelPath);
        return createCpu(environment, modelPath, machine.getThreads());
    }

    private static boolean canFallback(
            OnnxExecutionProvider requestedProvider,
            AppProperties.OnnxSettings onnxSettings) {
        return requestedProvider == OnnxExecutionProvider.AUTO
                || onnxSettings.isFallbackToCpu();
    }

    private static OnnxSessionResource createCpu(
            OrtEnvironment environment,
            String modelPath,
            int threads) throws OrtException {
        try (OrtSession.SessionOptions sessionOptions =
                OnnxSessionOptionsFactory.create(threads)) {
            OrtSession session = environment.createSession(
                    modelPath,
                    sessionOptions);
            return new OnnxSessionResource(
                    session,
                    OnnxExecutionProvider.CPU,
                    null);
        }
    }

    private static OnnxSessionResource createCuda(
            OrtEnvironment environment,
            String modelPath,
            int threads,
            AppProperties.OnnxCudaSettings cudaSettings) throws OrtException {
        OrtCUDAProviderOptions cudaOptions = null;
        try {
            cudaOptions = new OrtCUDAProviderOptions(
                    cudaSettings.getDeviceId());
            cudaOptions.add(
                    "gpu_mem_limit",
                    Long.toString(Math.multiplyExact(
                            cudaSettings.getMemoryLimitMb(),
                            BYTES_PER_MEBIBYTE)));
            cudaOptions.add("arena_extend_strategy", "kSameAsRequested");
            cudaOptions.add("do_copy_in_default_stream", "1");

            try (OrtSession.SessionOptions sessionOptions =
                    OnnxSessionOptionsFactory.create(threads)) {
                sessionOptions.addCUDA(cudaOptions);
                OrtSession session = environment.createSession(
                        modelPath,
                        sessionOptions);
                return new OnnxSessionResource(
                        session,
                        OnnxExecutionProvider.CUDA,
                        cudaOptions);
            }
        } catch (OrtException | RuntimeException | LinkageError exception) {
            if (cudaOptions != null) {
                cudaOptions.close();
            }
            throw exception;
        }
    }
}

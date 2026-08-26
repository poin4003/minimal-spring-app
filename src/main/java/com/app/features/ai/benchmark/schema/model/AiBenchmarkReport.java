package com.app.features.ai.benchmark.schema.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record AiBenchmarkReport(
        int schemaVersion,
        String generatedAt,
        String capability,
        String status,
        String modelVersion,
        double loadDurationMs,
        RuntimeInfo runtime,
        Map<String, Object> configuration,
        List<CaseResult> cases,
        String error) {

    public AiBenchmarkReport {
        configuration = Map.copyOf(new LinkedHashMap<>(configuration));
        cases = List.copyOf(cases);
    }

    public record RuntimeInfo(
            String hostname,
            String javaVersion,
            String vmName,
            String osName,
            String osVersion,
            String osArchitecture,
            int availableProcessors,
            long maxHeapBytes,
            List<String> jvmArguments) {

        public RuntimeInfo {
            jvmArguments = List.copyOf(jvmArguments);
        }
    }

    public record CaseResult(
            String name,
            int warmupIterations,
            int measuredIterations,
            double totalDurationMs,
            double throughputPerSecond,
            double processCpuPercent,
            long observedHeapPeakBytes,
            Latency latencyMs,
            Map<String, Double> metrics) {

        public CaseResult {
            metrics = Map.copyOf(new LinkedHashMap<>(metrics));
        }
    }

    public record Latency(
            double min,
            double average,
            double p50,
            double p95,
            double p99,
            double max) {
    }
}

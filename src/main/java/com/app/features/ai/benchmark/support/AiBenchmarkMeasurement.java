package com.app.features.ai.benchmark.support;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.app.features.ai.benchmark.schema.model.AiBenchmarkReport.CaseResult;
import com.app.features.ai.benchmark.schema.model.AiBenchmarkReport.Latency;

public final class AiBenchmarkMeasurement {

    private static final double NANOS_PER_MILLISECOND = 1_000_000.0;
    private static final double NANOS_PER_SECOND = 1_000_000_000.0;

    private AiBenchmarkMeasurement() {
    }

    public static MeasuredCase measure(
            String name,
            int warmupIterations,
            int measuredIterations,
            BenchmarkOperation operation) throws Exception {
        for (int index = 0; index < warmupIterations; index++) {
            operation.run();
        }

        List<Double> latencies = new ArrayList<>(measuredIterations);
        Map<String, Double> metricTotals = new LinkedHashMap<>();
        long observedHeapPeak = usedHeapBytes();
        long cpuStart = processCpuTimeNanos();
        long wallStart = System.nanoTime();

        for (int index = 0; index < measuredIterations; index++) {
            long iterationStart = System.nanoTime();
            Map<String, Double> observation = operation.run();
            long iterationEnd = System.nanoTime();

            latencies.add(
                    (iterationEnd - iterationStart) / NANOS_PER_MILLISECOND);
            observation.forEach((metric, value) -> metricTotals.merge(
                    metric,
                    value,
                    Double::sum));
            observedHeapPeak = Math.max(observedHeapPeak, usedHeapBytes());
        }

        long wallEnd = System.nanoTime();
        long cpuEnd = processCpuTimeNanos();
        long wallDuration = wallEnd - wallStart;
        Map<String, Double> metricAverages = new LinkedHashMap<>();
        metricTotals.forEach((metric, total) -> metricAverages.put(
                metric + "Average",
                total / measuredIterations));

        CaseResult report = new CaseResult(
                name,
                warmupIterations,
                measuredIterations,
                wallDuration / NANOS_PER_MILLISECOND,
                measuredIterations / (wallDuration / NANOS_PER_SECOND),
                resolveCpuPercent(cpuStart, cpuEnd, wallDuration),
                observedHeapPeak,
                summarizeLatency(latencies),
                metricAverages);
        return new MeasuredCase(report, metricTotals);
    }

    public static CaseResult addMetrics(
            CaseResult report,
            Map<String, Double> additionalMetrics) {
        Map<String, Double> metrics = new LinkedHashMap<>(report.metrics());
        metrics.putAll(additionalMetrics);
        return new CaseResult(
                report.name(),
                report.warmupIterations(),
                report.measuredIterations(),
                report.totalDurationMs(),
                report.throughputPerSecond(),
                report.processCpuPercent(),
                report.observedHeapPeakBytes(),
                report.latencyMs(),
                metrics);
    }

    private static Latency summarizeLatency(List<Double> values) {
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        double sum = sorted.stream().mapToDouble(Double::doubleValue).sum();
        return new Latency(
                sorted.getFirst(),
                sum / sorted.size(),
                percentile(sorted, 0.50),
                percentile(sorted, 0.95),
                percentile(sorted, 0.99),
                sorted.getLast());
    }

    private static double percentile(
            List<Double> sortedValues,
            double percentile) {
        int index = (int) Math.ceil(percentile * sortedValues.size()) - 1;
        return sortedValues.get(Math.max(0, index));
    }

    private static long usedHeapBytes() {
        return ManagementFactory.getMemoryMXBean()
                .getHeapMemoryUsage()
                .getUsed();
    }

    private static long processCpuTimeNanos() {
        java.lang.management.OperatingSystemMXBean bean =
                ManagementFactory.getOperatingSystemMXBean();
        if (bean instanceof com.sun.management.OperatingSystemMXBean sunBean) {
            return sunBean.getProcessCpuTime();
        }
        return -1L;
    }

    private static double resolveCpuPercent(
            long cpuStart,
            long cpuEnd,
            long wallDuration) {
        if (cpuStart < 0L || cpuEnd < cpuStart || wallDuration <= 0L) {
            return -1.0;
        }
        int processors = Runtime.getRuntime().availableProcessors();
        return ((double) (cpuEnd - cpuStart) / wallDuration)
                / processors
                * 100.0;
    }

    @FunctionalInterface
    public interface BenchmarkOperation {

        Map<String, Double> run() throws Exception;
    }

    public record MeasuredCase(
            CaseResult report,
            Map<String, Double> metricTotals) {

        public MeasuredCase {
            metricTotals = Map.copyOf(metricTotals);
        }

        public double total(String metric) {
            return metricTotals.getOrDefault(metric, 0.0);
        }
    }
}

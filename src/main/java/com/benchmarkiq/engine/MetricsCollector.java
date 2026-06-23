package com.benchmarkiq.engine;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public class MetricsCollector {

    private final LongAdder totalRequests = new LongAdder();
    private final LongAdder successfulRequests = new LongAdder();
    private final LongAdder failedRequests = new LongAdder();
    private final LongAdder totalLatencyMs = new LongAdder();
    private final AtomicLong minLatencyMs = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxLatencyMs = new AtomicLong(0);

    private final List<Long> latencySamples = Collections.synchronizedList(new ArrayList<>());

    @Getter
    private volatile int currentConcurrentUsers = 0;

    private final long startTimeMs;

    public MetricsCollector() {
        this.startTimeMs = System.currentTimeMillis();
    }

    public void recordSuccess(long latencyMs) {
        totalRequests.increment();
        successfulRequests.increment();
        totalLatencyMs.add(latencyMs);
        updateMinMax(latencyMs);
        latencySamples.add(latencyMs);
    }

    public void recordFailure(long latencyMs) {
        totalRequests.increment();
        failedRequests.increment();
        totalLatencyMs.add(latencyMs);
        updateMinMax(latencyMs);
        latencySamples.add(latencyMs);
    }

    public void setCurrentConcurrentUsers(int count) {
        this.currentConcurrentUsers = count;
    }

    private void updateMinMax(long latencyMs) {
        minLatencyMs.updateAndGet(current -> Math.min(current, latencyMs));
        maxLatencyMs.updateAndGet(current -> Math.max(current, latencyMs));
    }

    public long getTotalRequests() { return totalRequests.sum(); }
    public long getSuccessfulRequests() { return successfulRequests.sum(); }
    public long getFailedRequests() { return failedRequests.sum(); }

    public double getAverageLatencyMs() {
        long total = totalRequests.sum();
        return total > 0 ? (double) totalLatencyMs.sum() / total : 0.0;
    }

    public long getMinLatencyMs() {
        long min = minLatencyMs.get();
        return min == Long.MAX_VALUE ? 0 : min;
    }

    public long getMaxLatencyMs() { return maxLatencyMs.get(); }

    public double getErrorRatePercent() {
        long total = totalRequests.sum();
        return total > 0 ? (double) failedRequests.sum() / total * 100.0 : 0.0;
    }

    public double getRequestsPerSecond() {
        long elapsed = System.currentTimeMillis() - startTimeMs;
        return elapsed > 0 ? (double) totalRequests.sum() / elapsed * 1000.0 : 0.0;
    }

    public long getElapsedSeconds() {
        return (System.currentTimeMillis() - startTimeMs) / 1000;
    }

    public long getPercentile(int percentile) {
        List<Long> sorted;
        synchronized (latencySamples) {
            if (latencySamples.isEmpty()) return 0;
            sorted = new ArrayList<>(latencySamples);
        }
        Collections.sort(sorted);
        int idx = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(idx, sorted.size() - 1)));
    }

    public SnapshotMetrics snapshot() {
        return SnapshotMetrics.builder()
                .totalRequests(getTotalRequests())
                .successfulRequests(getSuccessfulRequests())
                .failedRequests(getFailedRequests())
                .averageLatencyMs(getAverageLatencyMs())
                .minLatencyMs(getMinLatencyMs())
                .maxLatencyMs(getMaxLatencyMs())
                .p50LatencyMs(getPercentile(50))
                .p90LatencyMs(getPercentile(90))
                .p95LatencyMs(getPercentile(95))
                .p99LatencyMs(getPercentile(99))
                .requestsPerSecond(getRequestsPerSecond())
                .errorRatePercent(getErrorRatePercent())
                .currentConcurrentUsers(currentConcurrentUsers)
                .elapsedSeconds(getElapsedSeconds())
                .build();
    }

    @lombok.Data
    @lombok.Builder
    public static class SnapshotMetrics {
        private long totalRequests;
        private long successfulRequests;
        private long failedRequests;
        private double averageLatencyMs;
        private long minLatencyMs;
        private long maxLatencyMs;
        private long p50LatencyMs;
        private long p90LatencyMs;
        private long p95LatencyMs;
        private long p99LatencyMs;
        private double requestsPerSecond;
        private double errorRatePercent;
        private int currentConcurrentUsers;
        private long elapsedSeconds;
    }
}

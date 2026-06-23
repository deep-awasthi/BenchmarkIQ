package com.benchmarkiq.engine;

import com.benchmarkiq.entity.TestConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Slf4j
public class LoadTestRunner {

    private final TestConfig config;
    private final int httpTimeoutMs;

    @Getter
    private final MetricsCollector metrics;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);

    private ExecutorService workerPool;
    private ScheduledExecutorService metricsScheduler;
    private Consumer<MetricsCollector.SnapshotMetrics> metricsCallback;

    public LoadTestRunner(TestConfig config, int httpTimeoutMs) {
        this.config = config;
        this.httpTimeoutMs = httpTimeoutMs;
        this.metrics = new MetricsCollector();
    }

    public void setMetricsCallback(Consumer<MetricsCollector.SnapshotMetrics> callback) {
        this.metricsCallback = callback;
    }

    public void run() throws InterruptedException {
        running.set(true);
        int concurrentUsers = config.getConcurrentUsers();
        int durationSeconds = config.getDurationSeconds();
        int rampUpSeconds = config.getRampUpSeconds();

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(httpTimeoutMs))
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .build();

        workerPool = Executors.newFixedThreadPool(Math.min(concurrentUsers, 500));

        metricsScheduler = Executors.newSingleThreadScheduledExecutor();
        if (metricsCallback != null) {
            metricsScheduler.scheduleAtFixedRate(
                    () -> metricsCallback.accept(metrics.snapshot()),
                    1, 1, TimeUnit.SECONDS
            );
        }

        long endTime = System.currentTimeMillis() + (durationSeconds * 1000L);

        // Ramp-up: gradually add users
        int[] activeUsers = {0};
        long rampUpMs = rampUpSeconds * 1000L;
        long rampUpStart = System.currentTimeMillis();

        log.info("Starting load test: {} concurrent users, {}s duration, {}s ramp-up",
                concurrentUsers, durationSeconds, rampUpSeconds);

        while (System.currentTimeMillis() < endTime && !stopRequested.get()) {
            int targetUsers = concurrentUsers;
            if (rampUpMs > 0) {
                long elapsed = System.currentTimeMillis() - rampUpStart;
                double rampFraction = Math.min(1.0, (double) elapsed / rampUpMs);
                targetUsers = (int) Math.ceil(concurrentUsers * rampFraction);
            }

            metrics.setCurrentConcurrentUsers(targetUsers);

            for (int i = 0; i < targetUsers && !stopRequested.get(); i++) {
                if (System.currentTimeMillis() >= endTime) break;
                workerPool.submit(new HttpRequestWorker(config, metrics, httpClient, httpTimeoutMs));
            }

            Thread.sleep(100);
        }

        stop();
        log.info("Load test completed. Total requests: {}", metrics.getTotalRequests());
    }

    public void stop() {
        stopRequested.set(true);
        running.set(false);
        if (workerPool != null) {
            workerPool.shutdown();
            try {
                if (!workerPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    workerPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                workerPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        if (metricsScheduler != null) {
            metricsScheduler.shutdownNow();
        }
        metrics.setCurrentConcurrentUsers(0);
    }

    public boolean isRunning() {
        return running.get();
    }
}

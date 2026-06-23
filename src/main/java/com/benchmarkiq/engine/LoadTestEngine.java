package com.benchmarkiq.engine;

import com.benchmarkiq.entity.TestConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class LoadTestEngine {

    @Value("${app.load-test.http-client-timeout-ms:30000}")
    private int httpTimeoutMs;

    private final Map<Long, LoadTestRunner> activeRunners = new ConcurrentHashMap<>();

    public LoadTestRunner createRunner(Long executionId, TestConfig config) {
        LoadTestRunner runner = new LoadTestRunner(config, httpTimeoutMs);
        activeRunners.put(executionId, runner);
        return runner;
    }

    public void stopRunner(Long executionId) {
        LoadTestRunner runner = activeRunners.get(executionId);
        if (runner != null) {
            runner.stop();
            activeRunners.remove(executionId);
            log.info("Stopped load test runner for execution: {}", executionId);
        }
    }

    public LoadTestRunner getRunner(Long executionId) {
        return activeRunners.get(executionId);
    }

    public boolean isRunning(Long executionId) {
        LoadTestRunner runner = activeRunners.get(executionId);
        return runner != null && runner.isRunning();
    }

    public void removeRunner(Long executionId) {
        activeRunners.remove(executionId);
    }

    public Map<Long, LoadTestRunner> getActiveRunners() {
        return Map.copyOf(activeRunners);
    }
}

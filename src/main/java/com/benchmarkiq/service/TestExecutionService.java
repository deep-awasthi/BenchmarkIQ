package com.benchmarkiq.service;

import com.benchmarkiq.dto.response.ReportResponse;
import com.benchmarkiq.dto.response.TestExecutionResponse;
import com.benchmarkiq.dto.response.TestResultResponse;
import com.benchmarkiq.engine.LoadTestEngine;
import com.benchmarkiq.engine.LoadTestRunner;
import com.benchmarkiq.engine.MetricsCollector;
import com.benchmarkiq.entity.*;
import com.benchmarkiq.exception.ResourceNotFoundException;
import com.benchmarkiq.exception.TestAlreadyRunningException;
import com.benchmarkiq.repository.TestExecutionRepository;
import com.benchmarkiq.repository.TestResultRepository;
import com.benchmarkiq.repository.UserRepository;
import com.benchmarkiq.websocket.LiveMetricsWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

@Service
@Slf4j
public class TestExecutionService {

    private final TestExecutionRepository executionRepository;
    private final TestResultRepository resultRepository;
    private final UserRepository userRepository;
    private final TestConfigService configService;
    private final LoadTestEngine loadTestEngine;
    private final ThresholdEvaluationService thresholdService;
    private final LiveMetricsWebSocketHandler wsHandler;
    private final Executor loadTestExecutor;

    public TestExecutionService(TestExecutionRepository executionRepository,
                                 TestResultRepository resultRepository,
                                 UserRepository userRepository,
                                 TestConfigService configService,
                                 LoadTestEngine loadTestEngine,
                                 ThresholdEvaluationService thresholdService,
                                 LiveMetricsWebSocketHandler wsHandler,
                                 @Qualifier("loadTestExecutor") Executor loadTestExecutor) {
        this.executionRepository = executionRepository;
        this.resultRepository = resultRepository;
        this.userRepository = userRepository;
        this.configService = configService;
        this.loadTestEngine = loadTestEngine;
        this.thresholdService = thresholdService;
        this.wsHandler = wsHandler;
        this.loadTestExecutor = loadTestExecutor;
    }

    @Transactional
    public TestExecutionResponse startTest(Long configId, Long userId) {
        TestConfig config = configService.findConfig(configId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        List<TestExecution> running = executionRepository.findByStatus(ExecutionStatus.RUNNING);
        boolean alreadyRunning = running.stream()
                .anyMatch(e -> e.getTestConfig().getId().equals(configId));
        if (alreadyRunning) {
            throw new TestAlreadyRunningException(configId);
        }

        TestExecution execution = TestExecution.builder()
                .testConfig(config)
                .triggeredBy(user)
                .status(ExecutionStatus.PENDING)
                .build();
        execution = executionRepository.save(execution);

        final Long executionId = execution.getId();
        final TestExecution savedExecution = execution;

        loadTestExecutor.execute(() -> runTestAsync(executionId, config, savedExecution));

        log.info("Test execution {} started for config {} by user {}", executionId, configId, userId);
        return toResponse(execution);
    }

    private void runTestAsync(Long executionId, TestConfig config, TestExecution execution) {
        try {
            updateStatus(executionId, ExecutionStatus.RUNNING);

            LoadTestRunner runner = loadTestEngine.createRunner(executionId, config);
            runner.setMetricsCallback(snapshot -> wsHandler.broadcastMetrics(executionId, snapshot));
            runner.run();

            MetricsCollector metrics = runner.getMetrics();
            saveResult(executionId, config, metrics);
            updateStatus(executionId, ExecutionStatus.COMPLETED);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            updateStatusWithError(executionId, ExecutionStatus.STOPPED, "Test was interrupted");
        } catch (Exception e) {
            log.error("Test execution {} failed: {}", executionId, e.getMessage(), e);
            updateStatusWithError(executionId, ExecutionStatus.FAILED, e.getMessage());
        } finally {
            loadTestEngine.removeRunner(executionId);
            completeExecution(executionId);
        }
    }

    @Transactional
    public TestExecutionResponse stopTest(Long executionId, Long userId) {
        TestExecution execution = findExecution(executionId);
        if (execution.getStatus() != ExecutionStatus.RUNNING &&
            execution.getStatus() != ExecutionStatus.PENDING) {
            throw new InvalidStateException("Test is not running");
        }
        loadTestEngine.stopRunner(executionId);
        execution.setStatus(ExecutionStatus.STOPPED);
        execution.setCompletedAt(LocalDateTime.now());
        execution = executionRepository.save(execution);
        log.info("Test execution {} stopped by user {}", executionId, userId);
        return toResponse(execution);
    }

    @Transactional(readOnly = true)
    public List<TestExecutionResponse> getRunningTests() {
        return executionRepository.findByStatus(ExecutionStatus.RUNNING)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<TestExecutionResponse> getExecutionHistory(Long configId, Pageable pageable) {
        return executionRepository.findByTestConfigIdOrderByStartedAtDesc(configId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public TestExecutionResponse getExecution(Long executionId) {
        return toResponse(findExecution(executionId));
    }

    private void saveResult(Long executionId, TestConfig config, MetricsCollector metrics) {
        TestExecution execution = findExecution(executionId);
        List<ReportResponse.ThresholdCheck> checks = new ArrayList<>();

        TestResult result = TestResult.builder()
                .testExecution(execution)
                .totalRequests(metrics.getTotalRequests())
                .successfulRequests(metrics.getSuccessfulRequests())
                .failedRequests(metrics.getFailedRequests())
                .averageLatencyMs(metrics.getAverageLatencyMs())
                .minLatencyMs(metrics.getMinLatencyMs())
                .maxLatencyMs(metrics.getMaxLatencyMs())
                .p50LatencyMs(metrics.getPercentile(50))
                .p90LatencyMs(metrics.getPercentile(90))
                .p95LatencyMs(metrics.getPercentile(95))
                .p99LatencyMs(metrics.getPercentile(99))
                .requestsPerSecond(metrics.getRequestsPerSecond())
                .errorRatePercent(metrics.getErrorRatePercent())
                .completedAt(LocalDateTime.now())
                .build();

        ThresholdResult thresholdResult = thresholdService.evaluate(result, config, checks);
        result.setThresholdResult(thresholdResult);
        result.setThresholdDetails(thresholdService.serializeChecks(checks));

        resultRepository.save(result);
    }

    private void updateStatus(Long executionId, ExecutionStatus status) {
        executionRepository.findById(executionId).ifPresent(e -> {
            e.setStatus(status);
            executionRepository.save(e);
        });
    }

    private void updateStatusWithError(Long executionId, ExecutionStatus status, String errorMsg) {
        executionRepository.findById(executionId).ifPresent(e -> {
            e.setStatus(status);
            e.setErrorMessage(errorMsg);
            executionRepository.save(e);
        });
    }

    private void completeExecution(Long executionId) {
        executionRepository.findById(executionId).ifPresent(e -> {
            if (e.getCompletedAt() == null) {
                e.setCompletedAt(LocalDateTime.now());
                executionRepository.save(e);
            }
        });
    }

    private TestExecution findExecution(Long id) {
        return executionRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TestExecution", "id", id));
    }

    public TestExecutionResponse toResponse(TestExecution e) {
        TestResultResponse resultResponse = null;
        if (e.getTestResult() != null) {
            resultResponse = toResultResponse(e.getTestResult());
        }
        return TestExecutionResponse.builder()
                .id(e.getId())
                .testConfigId(e.getTestConfig().getId())
                .testConfigName(e.getTestConfig().getName())
                .status(e.getStatus())
                .startedAt(e.getStartedAt())
                .completedAt(e.getCompletedAt())
                .triggeredBy(e.getTriggeredBy().getUsername())
                .errorMessage(e.getErrorMessage())
                .result(resultResponse)
                .build();
    }

    public TestResultResponse toResultResponse(TestResult r) {
        return TestResultResponse.builder()
                .id(r.getId())
                .totalRequests(r.getTotalRequests())
                .successfulRequests(r.getSuccessfulRequests())
                .failedRequests(r.getFailedRequests())
                .averageLatencyMs(r.getAverageLatencyMs())
                .minLatencyMs(r.getMinLatencyMs())
                .maxLatencyMs(r.getMaxLatencyMs())
                .p50LatencyMs(r.getP50LatencyMs())
                .p90LatencyMs(r.getP90LatencyMs())
                .p95LatencyMs(r.getP95LatencyMs())
                .p99LatencyMs(r.getP99LatencyMs())
                .requestsPerSecond(r.getRequestsPerSecond())
                .errorRatePercent(r.getErrorRatePercent())
                .thresholdResult(r.getThresholdResult())
                .thresholdDetails(r.getThresholdDetails())
                .completedAt(r.getCompletedAt())
                .build();
    }

    static class InvalidStateException extends RuntimeException {
        InvalidStateException(String msg) { super(msg); }
    }
}

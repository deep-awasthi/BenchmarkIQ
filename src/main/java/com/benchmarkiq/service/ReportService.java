package com.benchmarkiq.service;

import com.benchmarkiq.config.CacheConfig;
import com.benchmarkiq.dto.response.ReportResponse;
import com.benchmarkiq.dto.response.TestResultResponse;
import com.benchmarkiq.entity.TestExecution;
import com.benchmarkiq.entity.TestResult;
import com.benchmarkiq.exception.ResourceNotFoundException;
import com.benchmarkiq.repository.TestExecutionRepository;
import com.benchmarkiq.repository.TestResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final TestExecutionRepository executionRepository;
    private final TestResultRepository resultRepository;
    private final ThresholdEvaluationService thresholdService;
    private final TestExecutionService executionService;

    @Cacheable(value = CacheConfig.CACHE_TEST_RESULTS, key = "#executionId")
    @Transactional(readOnly = true)
    public ReportResponse generateReport(Long executionId) {
        TestExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new ResourceNotFoundException("TestExecution", "id", executionId));

        TestResult result = resultRepository.findByTestExecutionId(executionId)
                .orElseThrow(() -> new ResourceNotFoundException("TestResult for execution", "id", executionId));

        List<ReportResponse.ThresholdCheck> checks =
                thresholdService.deserializeChecks(result.getThresholdDetails());

        long durationSeconds = 0;
        if (execution.getStartedAt() != null && execution.getCompletedAt() != null) {
            durationSeconds = ChronoUnit.SECONDS.between(execution.getStartedAt(), execution.getCompletedAt());
        }

        Map<String, Object> configuration = new HashMap<>();
        configuration.put("concurrentUsers", execution.getTestConfig().getConcurrentUsers());
        configuration.put("durationSeconds", execution.getTestConfig().getDurationSeconds());
        configuration.put("rampUpSeconds", execution.getTestConfig().getRampUpSeconds());
        configuration.put("httpMethod", execution.getTestConfig().getHttpMethod());
        if (execution.getTestConfig().getMaxAverageLatencyMs() != null) {
            configuration.put("maxAverageLatencyMs", execution.getTestConfig().getMaxAverageLatencyMs());
        }
        if (execution.getTestConfig().getMaxP95LatencyMs() != null) {
            configuration.put("maxP95LatencyMs", execution.getTestConfig().getMaxP95LatencyMs());
        }
        if (execution.getTestConfig().getMaxErrorRatePercent() != null) {
            configuration.put("maxErrorRatePercent", execution.getTestConfig().getMaxErrorRatePercent());
        }

        return ReportResponse.builder()
                .executionId(executionId)
                .testName(execution.getTestConfig().getName())
                .targetUrl(execution.getTestConfig().getTargetUrl())
                .httpMethod(execution.getTestConfig().getHttpMethod().name())
                .status(execution.getStatus())
                .startedAt(execution.getStartedAt())
                .completedAt(execution.getCompletedAt())
                .durationSeconds(durationSeconds)
                .triggeredBy(execution.getTriggeredBy().getUsername())
                .metrics(executionService.toResultResponse(result))
                .thresholdResult(result.getThresholdResult())
                .thresholdChecks(checks)
                .configuration(configuration)
                .build();
    }
}

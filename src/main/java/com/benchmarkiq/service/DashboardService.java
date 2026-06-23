package com.benchmarkiq.service;

import com.benchmarkiq.config.CacheConfig;
import com.benchmarkiq.dto.response.DashboardSummaryResponse;
import com.benchmarkiq.dto.response.TestExecutionResponse;
import com.benchmarkiq.entity.ExecutionStatus;
import com.benchmarkiq.entity.TestResult;
import com.benchmarkiq.entity.ThresholdResult;
import com.benchmarkiq.repository.TestConfigRepository;
import com.benchmarkiq.repository.TestExecutionRepository;
import com.benchmarkiq.repository.TestResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final TestConfigRepository configRepository;
    private final TestExecutionRepository executionRepository;
    private final TestResultRepository resultRepository;
    private final TestExecutionService executionService;

    @Cacheable(value = CacheConfig.CACHE_DASHBOARD, key = "'summary'")
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        long totalConfigs = configRepository.count();
        long totalExecutions = executionRepository.count();
        long runningExecutions = executionRepository.countByStatus(ExecutionStatus.RUNNING);
        long completedExecutions = executionRepository.countByStatus(ExecutionStatus.COMPLETED);
        long passedThresholds = resultRepository.countByThresholdResult(ThresholdResult.PASS);
        long failedThresholds = resultRepository.countByThresholdResult(ThresholdResult.FAIL);

        List<TestExecutionResponse> recent = executionRepository
                .findAll(PageRequest.of(0, 10))
                .stream()
                .sorted((a, b) -> b.getStartedAt().compareTo(a.getStartedAt()))
                .map(executionService::toResponse)
                .toList();

        List<DashboardSummaryResponse.TrendPoint> trend = buildTrend();

        return DashboardSummaryResponse.builder()
                .totalConfigs(totalConfigs)
                .totalExecutions(totalExecutions)
                .runningExecutions(runningExecutions)
                .completedExecutions(completedExecutions)
                .passedThresholds(passedThresholds)
                .failedThresholds(failedThresholds)
                .recentExecutions(recent)
                .performanceTrend(trend)
                .build();
    }

    @Transactional(readOnly = true)
    public List<TestExecutionResponse> getLatestResults(int limit) {
        return executionRepository.findAll(PageRequest.of(0, limit))
                .stream()
                .sorted((a, b) -> b.getStartedAt().compareTo(a.getStartedAt()))
                .map(executionService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DashboardSummaryResponse.TrendPoint> getTrend(Long configId, int days) {
        LocalDateTime from = LocalDateTime.now().minusDays(days);
        LocalDateTime to = LocalDateTime.now();
        List<TestResult> results = resultRepository.findTrendData(configId, from, to);
        return toTrendPoints(results);
    }

    private List<DashboardSummaryResponse.TrendPoint> buildTrend() {
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to = LocalDateTime.now();
        List<TestResult> results = resultRepository.findAll().stream()
                .filter(r -> r.getCompletedAt() != null &&
                             r.getCompletedAt().isAfter(from) &&
                             r.getCompletedAt().isBefore(to))
                .toList();
        return toTrendPoints(results);
    }

    private List<DashboardSummaryResponse.TrendPoint> toTrendPoints(List<TestResult> results) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return results.stream()
                .filter(r -> r.getCompletedAt() != null)
                .map(r -> DashboardSummaryResponse.TrendPoint.builder()
                        .timestamp(r.getCompletedAt().format(fmt))
                        .averageLatencyMs(r.getAverageLatencyMs())
                        .requestsPerSecond(r.getRequestsPerSecond())
                        .errorRatePercent(r.getErrorRatePercent())
                        .build())
                .toList();
    }
}

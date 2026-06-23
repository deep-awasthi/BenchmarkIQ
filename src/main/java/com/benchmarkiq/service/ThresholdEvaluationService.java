package com.benchmarkiq.service;

import com.benchmarkiq.dto.response.ReportResponse;
import com.benchmarkiq.entity.TestConfig;
import com.benchmarkiq.entity.TestResult;
import com.benchmarkiq.entity.ThresholdResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ThresholdEvaluationService {

    private final ObjectMapper objectMapper;

    public ThresholdResult evaluate(TestResult result, TestConfig config, List<ReportResponse.ThresholdCheck> checks) {
        if (config.getMaxAverageLatencyMs() == null &&
            config.getMaxP95LatencyMs() == null &&
            config.getMaxErrorRatePercent() == null) {
            return ThresholdResult.NOT_EVALUATED;
        }

        boolean allPassed = true;

        if (config.getMaxAverageLatencyMs() != null) {
            boolean passed = result.getAverageLatencyMs() <= config.getMaxAverageLatencyMs();
            allPassed &= passed;
            checks.add(ReportResponse.ThresholdCheck.builder()
                    .metric("Average Latency")
                    .threshold(config.getMaxAverageLatencyMs() + " ms")
                    .actual(String.format("%.2f ms", result.getAverageLatencyMs()))
                    .passed(passed)
                    .build());
        }

        if (config.getMaxP95LatencyMs() != null) {
            boolean passed = result.getP95LatencyMs() <= config.getMaxP95LatencyMs();
            allPassed &= passed;
            checks.add(ReportResponse.ThresholdCheck.builder()
                    .metric("P95 Latency")
                    .threshold(config.getMaxP95LatencyMs() + " ms")
                    .actual(result.getP95LatencyMs() + " ms")
                    .passed(passed)
                    .build());
        }

        if (config.getMaxErrorRatePercent() != null) {
            boolean passed = result.getErrorRatePercent() <= config.getMaxErrorRatePercent();
            allPassed &= passed;
            checks.add(ReportResponse.ThresholdCheck.builder()
                    .metric("Error Rate")
                    .threshold(config.getMaxErrorRatePercent() + "%")
                    .actual(String.format("%.2f%%", result.getErrorRatePercent()))
                    .passed(passed)
                    .build());
        }

        ThresholdResult thresholdResult = allPassed ? ThresholdResult.PASS : ThresholdResult.FAIL;
        log.info("Threshold evaluation: {}", thresholdResult);
        return thresholdResult;
    }

    public String serializeChecks(List<ReportResponse.ThresholdCheck> checks) {
        try {
            return objectMapper.writeValueAsString(checks);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize threshold checks", e);
            return "[]";
        }
    }

    public List<ReportResponse.ThresholdCheck> deserializeChecks(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ReportResponse.ThresholdCheck.class));
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize threshold checks", e);
            return new ArrayList<>();
        }
    }
}

package com.benchmarkiq.dto.response;

import com.benchmarkiq.entity.ExecutionStatus;
import com.benchmarkiq.entity.ThresholdResult;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ReportResponse {
    private Long executionId;
    private String testName;
    private String targetUrl;
    private String httpMethod;
    private ExecutionStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private long durationSeconds;
    private String triggeredBy;

    private TestResultResponse metrics;

    private ThresholdResult thresholdResult;
    private List<ThresholdCheck> thresholdChecks;

    private Map<String, Object> configuration;

    @Data
    @Builder
    public static class ThresholdCheck {
        private String metric;
        private String threshold;
        private String actual;
        private boolean passed;
    }
}

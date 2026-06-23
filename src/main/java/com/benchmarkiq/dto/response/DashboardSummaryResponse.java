package com.benchmarkiq.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DashboardSummaryResponse {
    private long totalConfigs;
    private long totalExecutions;
    private long runningExecutions;
    private long completedExecutions;
    private long passedThresholds;
    private long failedThresholds;
    private List<TestExecutionResponse> recentExecutions;
    private List<TrendPoint> performanceTrend;

    @Data
    @Builder
    public static class TrendPoint {
        private String timestamp;
        private double averageLatencyMs;
        private double requestsPerSecond;
        private double errorRatePercent;
    }
}

package com.benchmarkiq.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LiveMetricsResponse {
    private Long executionId;
    private long totalRequests;
    private long successfulRequests;
    private long failedRequests;
    private double requestsPerSecond;
    private double errorRatePercent;
    private double averageLatencyMs;
    private int currentConcurrentUsers;
    private String status;
    private long elapsedSeconds;
}

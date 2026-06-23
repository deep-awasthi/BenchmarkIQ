package com.benchmarkiq.websocket;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MetricsMessage {
    private Long executionId;
    private long totalRequests;
    private long successfulRequests;
    private long failedRequests;
    private double requestsPerSecond;
    private double errorRatePercent;
    private double averageLatencyMs;
    private int currentConcurrentUsers;
    private long elapsedSeconds;
}

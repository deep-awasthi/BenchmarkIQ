package com.benchmarkiq.dto.response;

import com.benchmarkiq.entity.ThresholdResult;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TestResultResponse {
    private Long id;
    private long totalRequests;
    private long successfulRequests;
    private long failedRequests;
    private double averageLatencyMs;
    private long minLatencyMs;
    private long maxLatencyMs;
    private long p50LatencyMs;
    private long p90LatencyMs;
    private long p95LatencyMs;
    private long p99LatencyMs;
    private double requestsPerSecond;
    private double errorRatePercent;
    private ThresholdResult thresholdResult;
    private String thresholdDetails;
    private LocalDateTime completedAt;
}

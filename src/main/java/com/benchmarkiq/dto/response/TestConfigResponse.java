package com.benchmarkiq.dto.response;

import com.benchmarkiq.entity.HttpMethod;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class TestConfigResponse {
    private Long id;
    private String name;
    private String description;
    private String targetUrl;
    private HttpMethod httpMethod;
    private Map<String, String> headers;
    private String requestBody;
    private int concurrentUsers;
    private int durationSeconds;
    private int rampUpSeconds;
    private Long maxAverageLatencyMs;
    private Long maxP95LatencyMs;
    private Double maxErrorRatePercent;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

package com.benchmarkiq.dto.request;

import com.benchmarkiq.entity.HttpMethod;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.util.Map;

@Data
public class TestConfigRequest {

    @NotBlank(message = "Test name is required")
    @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotBlank(message = "Target URL is required")
    @URL(message = "Invalid URL format")
    @Size(max = 2048)
    private String targetUrl;

    @NotNull(message = "HTTP method is required")
    private HttpMethod httpMethod;

    private Map<String, String> headers;

    private String requestBody;

    @NotNull(message = "Concurrent users is required")
    @Min(value = 1, message = "Concurrent users must be at least 1")
    @Max(value = 1000, message = "Concurrent users cannot exceed 1000")
    private Integer concurrentUsers;

    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 second")
    @Max(value = 3600, message = "Duration cannot exceed 3600 seconds")
    private Integer durationSeconds;

    @Min(value = 0, message = "Ramp-up time cannot be negative")
    @Max(value = 300, message = "Ramp-up time cannot exceed 300 seconds")
    private Integer rampUpSeconds = 0;

    @Min(value = 1, message = "Max average latency must be positive")
    private Long maxAverageLatencyMs;

    @Min(value = 1, message = "Max P95 latency must be positive")
    private Long maxP95LatencyMs;

    @DecimalMin(value = "0.0", message = "Error rate must be non-negative")
    @DecimalMax(value = "100.0", message = "Error rate cannot exceed 100%")
    private Double maxErrorRatePercent;
}

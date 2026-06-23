package com.benchmarkiq.entity;

import jakarta.persistence.*;
import lombok.*;
import com.benchmarkiq.config.MapToJsonConverter;
import jakarta.persistence.Convert;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "test_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "target_url", nullable = false, length = 2048)
    private String targetUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "http_method", nullable = false, length = 10)
    private HttpMethod httpMethod;

    @Convert(converter = MapToJsonConverter.class)
    @Column(name = "headers", columnDefinition = "TEXT")
    private Map<String, String> headers;

    @Column(name = "request_body", columnDefinition = "TEXT")
    private String requestBody;

    @Column(name = "concurrent_users", nullable = false)
    private int concurrentUsers;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    @Column(name = "ramp_up_seconds", nullable = false)
    @Builder.Default
    private int rampUpSeconds = 0;

    @Column(name = "max_average_latency_ms")
    private Long maxAverageLatencyMs;

    @Column(name = "max_p95_latency_ms")
    private Long maxP95LatencyMs;

    @Column(name = "max_error_rate_percent")
    private Double maxErrorRatePercent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

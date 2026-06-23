package com.benchmarkiq.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "test_results",
        indexes = {
                @Index(name = "idx_result_execution", columnList = "test_execution_id"),
                @Index(name = "idx_result_completed", columnList = "completed_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_execution_id", nullable = false)
    private TestExecution testExecution;

    @Column(name = "total_requests")
    private long totalRequests;

    @Column(name = "successful_requests")
    private long successfulRequests;

    @Column(name = "failed_requests")
    private long failedRequests;

    @Column(name = "average_latency_ms")
    private double averageLatencyMs;

    @Column(name = "min_latency_ms")
    private long minLatencyMs;

    @Column(name = "max_latency_ms")
    private long maxLatencyMs;

    @Column(name = "p50_latency_ms")
    private long p50LatencyMs;

    @Column(name = "p90_latency_ms")
    private long p90LatencyMs;

    @Column(name = "p95_latency_ms")
    private long p95LatencyMs;

    @Column(name = "p99_latency_ms")
    private long p99LatencyMs;

    @Column(name = "requests_per_second")
    private double requestsPerSecond;

    @Column(name = "error_rate_percent")
    private double errorRatePercent;

    @Enumerated(EnumType.STRING)
    @Column(name = "threshold_result", length = 20)
    @Builder.Default
    private ThresholdResult thresholdResult = ThresholdResult.NOT_EVALUATED;

    @Column(name = "threshold_details", columnDefinition = "TEXT")
    private String thresholdDetails;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}

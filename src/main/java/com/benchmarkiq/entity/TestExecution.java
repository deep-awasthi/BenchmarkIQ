package com.benchmarkiq.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "test_executions",
        indexes = {
                @Index(name = "idx_exec_config", columnList = "test_config_id"),
                @Index(name = "idx_exec_status", columnList = "status"),
                @Index(name = "idx_exec_started", columnList = "started_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_config_id", nullable = false)
    private TestConfig testConfig;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ExecutionStatus status = ExecutionStatus.PENDING;

    @CreationTimestamp
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "triggered_by", nullable = false)
    private User triggeredBy;

    @OneToOne(mappedBy = "testExecution", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private TestResult testResult;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;
}

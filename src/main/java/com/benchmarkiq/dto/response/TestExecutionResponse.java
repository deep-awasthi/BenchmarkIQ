package com.benchmarkiq.dto.response;

import com.benchmarkiq.entity.ExecutionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TestExecutionResponse {
    private Long id;
    private Long testConfigId;
    private String testConfigName;
    private ExecutionStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String triggeredBy;
    private String errorMessage;
    private TestResultResponse result;
}

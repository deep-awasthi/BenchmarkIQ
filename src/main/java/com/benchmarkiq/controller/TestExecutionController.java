package com.benchmarkiq.controller;

import com.benchmarkiq.dto.response.ApiResponse;
import com.benchmarkiq.dto.response.TestExecutionResponse;
import com.benchmarkiq.security.UserPrincipal;
import com.benchmarkiq.service.TestExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/executions")
@RequiredArgsConstructor
@Tag(name = "Test Executions", description = "Start, stop, and monitor load tests")
@SecurityRequirement(name = "Bearer Authentication")
public class TestExecutionController {

    private final TestExecutionService executionService;

    @PostMapping("/start/{configId}")
    @Operation(summary = "Start a load test for the given configuration")
    public ResponseEntity<ApiResponse<TestExecutionResponse>> start(
            @PathVariable Long configId,
            @AuthenticationPrincipal UserPrincipal principal) {
        TestExecutionResponse response = executionService.startTest(configId, principal.getId());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success("Test started", response));
    }

    @PostMapping("/stop/{executionId}")
    @Operation(summary = "Stop a running load test")
    public ResponseEntity<ApiResponse<TestExecutionResponse>> stop(
            @PathVariable Long executionId,
            @AuthenticationPrincipal UserPrincipal principal) {
        TestExecutionResponse response = executionService.stopTest(executionId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Test stopped", response));
    }

    @GetMapping("/running")
    @Operation(summary = "List all currently running tests")
    public ResponseEntity<ApiResponse<List<TestExecutionResponse>>> getRunning() {
        return ResponseEntity.ok(ApiResponse.success(executionService.getRunningTests()));
    }

    @GetMapping("/{executionId}")
    @Operation(summary = "Get execution details by ID")
    public ResponseEntity<ApiResponse<TestExecutionResponse>> getExecution(@PathVariable Long executionId) {
        return ResponseEntity.ok(ApiResponse.success(executionService.getExecution(executionId)));
    }

    @GetMapping("/history/{configId}")
    @Operation(summary = "Get execution history for a test configuration")
    public ResponseEntity<Page<TestExecutionResponse>> getHistory(
            @PathVariable Long configId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(executionService.getExecutionHistory(configId, pageable));
    }
}

package com.benchmarkiq.controller;

import com.benchmarkiq.dto.response.ApiResponse;
import com.benchmarkiq.dto.response.DashboardSummaryResponse;
import com.benchmarkiq.dto.response.TestExecutionResponse;
import com.benchmarkiq.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Summary, trends, and latest results")
@SecurityRequirement(name = "Bearer Authentication")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @Operation(summary = "Get overall dashboard summary")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getSummary() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getSummary()));
    }

    @GetMapping("/latest")
    @Operation(summary = "Get the most recent test results")
    public ResponseEntity<ApiResponse<List<TestExecutionResponse>>> getLatest(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getLatestResults(limit)));
    }

    @GetMapping("/trend/{configId}")
    @Operation(summary = "Get performance trend for a test configuration")
    public ResponseEntity<ApiResponse<List<DashboardSummaryResponse.TrendPoint>>> getTrend(
            @PathVariable Long configId,
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getTrend(configId, days)));
    }
}

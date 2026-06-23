package com.benchmarkiq.controller;

import com.benchmarkiq.dto.response.ApiResponse;
import com.benchmarkiq.dto.response.ReportResponse;
import com.benchmarkiq.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Detailed test execution reports")
@SecurityRequirement(name = "Bearer Authentication")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/{executionId}")
    @Operation(summary = "Generate a detailed report for a completed test execution")
    public ResponseEntity<ApiResponse<ReportResponse>> getReport(@PathVariable Long executionId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.generateReport(executionId)));
    }
}

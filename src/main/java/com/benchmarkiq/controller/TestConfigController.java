package com.benchmarkiq.controller;

import com.benchmarkiq.dto.request.TestConfigRequest;
import com.benchmarkiq.dto.response.ApiResponse;
import com.benchmarkiq.dto.response.TestConfigResponse;
import com.benchmarkiq.security.UserPrincipal;
import com.benchmarkiq.service.TestConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test-configs")
@RequiredArgsConstructor
@Tag(name = "Test Configurations", description = "Manage API load test configurations")
@SecurityRequirement(name = "Bearer Authentication")
public class TestConfigController {

    private final TestConfigService configService;

    @PostMapping
    @Operation(summary = "Create a new test configuration")
    public ResponseEntity<ApiResponse<TestConfigResponse>> create(
            @Valid @RequestBody TestConfigRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        TestConfigResponse response = configService.create(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Test config created", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get test configuration by ID")
    public ResponseEntity<ApiResponse<TestConfigResponse>> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        boolean isAdmin = isAdmin(principal);
        TestConfigResponse response = configService.getById(id, principal.getId(), isAdmin);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "List all test configurations (paginated)")
    public ResponseEntity<Page<TestConfigResponse>> getAll(
            @RequestParam(required = false) String name,
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        boolean isAdmin = isAdmin(principal);
        return ResponseEntity.ok(configService.getAll(principal.getId(), isAdmin, name, pageable));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a test configuration")
    public ResponseEntity<ApiResponse<TestConfigResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody TestConfigRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        boolean isAdmin = isAdmin(principal);
        TestConfigResponse response = configService.update(id, request, principal.getId(), isAdmin);
        return ResponseEntity.ok(ApiResponse.success("Test config updated", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a test configuration")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        boolean isAdmin = isAdmin(principal);
        configService.delete(id, principal.getId(), isAdmin);
        return ResponseEntity.ok(ApiResponse.success("Test config deleted"));
    }

    private boolean isAdmin(UserPrincipal principal) {
        return principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}

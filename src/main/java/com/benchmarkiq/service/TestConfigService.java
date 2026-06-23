package com.benchmarkiq.service;

import com.benchmarkiq.config.CacheConfig;
import com.benchmarkiq.dto.request.TestConfigRequest;
import com.benchmarkiq.dto.response.TestConfigResponse;
import com.benchmarkiq.entity.TestConfig;
import com.benchmarkiq.entity.User;
import com.benchmarkiq.exception.ResourceNotFoundException;
import com.benchmarkiq.exception.UnauthorizedException;
import com.benchmarkiq.repository.TestConfigRepository;
import com.benchmarkiq.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestConfigService {

    private final TestConfigRepository configRepository;
    private final UserRepository userRepository;

    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_TEST_CONFIGS, allEntries = true)
    public TestConfigResponse create(TestConfigRequest request, Long userId) {
        User user = findUser(userId);
        TestConfig config = buildConfig(request, user);
        config = configRepository.save(config);
        log.info("Created test config '{}' by user {}", config.getName(), userId);
        return toResponse(config);
    }

    @Cacheable(value = CacheConfig.CACHE_TEST_CONFIGS, key = "#id")
    @Transactional(readOnly = true)
    public TestConfigResponse getById(Long id, Long userId, boolean isAdmin) {
        TestConfig config = findConfig(id);
        if (!isAdmin && !config.getCreatedBy().getId().equals(userId)) {
            throw new UnauthorizedException("You don't have access to this test configuration");
        }
        return toResponse(config);
    }

    @Transactional(readOnly = true)
    public Page<TestConfigResponse> getAll(Long userId, boolean isAdmin, String name, Pageable pageable) {
        Long filterUserId = isAdmin ? null : userId;
        return configRepository.findWithFilters(filterUserId, name, pageable).map(this::toResponse);
    }

    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_TEST_CONFIGS, allEntries = true)
    public TestConfigResponse update(Long id, TestConfigRequest request, Long userId, boolean isAdmin) {
        TestConfig config = findConfig(id);
        if (!isAdmin && !config.getCreatedBy().getId().equals(userId)) {
            throw new UnauthorizedException("You don't have permission to update this configuration");
        }
        applyUpdate(config, request);
        config = configRepository.save(config);
        log.info("Updated test config {} by user {}", id, userId);
        return toResponse(config);
    }

    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_TEST_CONFIGS, allEntries = true)
    public void delete(Long id, Long userId, boolean isAdmin) {
        TestConfig config = findConfig(id);
        if (!isAdmin && !config.getCreatedBy().getId().equals(userId)) {
            throw new UnauthorizedException("You don't have permission to delete this configuration");
        }
        configRepository.delete(config);
        log.info("Deleted test config {} by user {}", id, userId);
    }

    public TestConfig findConfig(Long id) {
        return configRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TestConfig", "id", id));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    private TestConfig buildConfig(TestConfigRequest r, User user) {
        return TestConfig.builder()
                .name(r.getName())
                .description(r.getDescription())
                .targetUrl(r.getTargetUrl())
                .httpMethod(r.getHttpMethod())
                .headers(r.getHeaders())
                .requestBody(r.getRequestBody())
                .concurrentUsers(r.getConcurrentUsers())
                .durationSeconds(r.getDurationSeconds())
                .rampUpSeconds(r.getRampUpSeconds() != null ? r.getRampUpSeconds() : 0)
                .maxAverageLatencyMs(r.getMaxAverageLatencyMs())
                .maxP95LatencyMs(r.getMaxP95LatencyMs())
                .maxErrorRatePercent(r.getMaxErrorRatePercent())
                .createdBy(user)
                .build();
    }

    private void applyUpdate(TestConfig config, TestConfigRequest r) {
        config.setName(r.getName());
        config.setDescription(r.getDescription());
        config.setTargetUrl(r.getTargetUrl());
        config.setHttpMethod(r.getHttpMethod());
        config.setHeaders(r.getHeaders());
        config.setRequestBody(r.getRequestBody());
        config.setConcurrentUsers(r.getConcurrentUsers());
        config.setDurationSeconds(r.getDurationSeconds());
        config.setRampUpSeconds(r.getRampUpSeconds() != null ? r.getRampUpSeconds() : 0);
        config.setMaxAverageLatencyMs(r.getMaxAverageLatencyMs());
        config.setMaxP95LatencyMs(r.getMaxP95LatencyMs());
        config.setMaxErrorRatePercent(r.getMaxErrorRatePercent());
    }

    public TestConfigResponse toResponse(TestConfig c) {
        return TestConfigResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .description(c.getDescription())
                .targetUrl(c.getTargetUrl())
                .httpMethod(c.getHttpMethod())
                .headers(c.getHeaders())
                .requestBody(c.getRequestBody())
                .concurrentUsers(c.getConcurrentUsers())
                .durationSeconds(c.getDurationSeconds())
                .rampUpSeconds(c.getRampUpSeconds())
                .maxAverageLatencyMs(c.getMaxAverageLatencyMs())
                .maxP95LatencyMs(c.getMaxP95LatencyMs())
                .maxErrorRatePercent(c.getMaxErrorRatePercent())
                .createdBy(c.getCreatedBy().getUsername())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}

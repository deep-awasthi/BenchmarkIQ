package com.benchmarkiq.service;

import com.benchmarkiq.dto.request.TestConfigRequest;
import com.benchmarkiq.dto.response.TestConfigResponse;
import com.benchmarkiq.entity.HttpMethod;
import com.benchmarkiq.entity.Role;
import com.benchmarkiq.entity.TestConfig;
import com.benchmarkiq.entity.User;
import com.benchmarkiq.exception.ResourceNotFoundException;
import com.benchmarkiq.exception.UnauthorizedException;
import com.benchmarkiq.repository.TestConfigRepository;
import com.benchmarkiq.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestConfigServiceTest {

    @Mock TestConfigRepository configRepository;
    @Mock UserRepository userRepository;
    @InjectMocks TestConfigService configService;

    private User user;
    private TestConfig config;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("testuser").email("t@test.com").role(Role.USER).build();
        config = TestConfig.builder()
                .id(10L).name("My Test").targetUrl("http://example.com")
                .httpMethod(HttpMethod.GET).concurrentUsers(10).durationSeconds(30)
                .createdBy(user).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void create_success() {
        TestConfigRequest req = buildRequest();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(configRepository.save(any())).thenReturn(config);

        TestConfigResponse response = configService.create(req, 1L);

        assertThat(response.getName()).isEqualTo("My Test");
        assertThat(response.getCreatedBy()).isEqualTo("testuser");
        verify(configRepository).save(any());
    }

    @Test
    void create_userNotFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> configService.create(buildRequest(), 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getById_ownConfig_success() {
        when(configRepository.findById(10L)).thenReturn(Optional.of(config));

        TestConfigResponse response = configService.getById(10L, 1L, false);

        assertThat(response.getId()).isEqualTo(10L);
    }

    @Test
    void getById_otherUsersConfig_asNonAdmin_throws() {
        when(configRepository.findById(10L)).thenReturn(Optional.of(config));

        assertThatThrownBy(() -> configService.getById(10L, 99L, false))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void getById_otherUsersConfig_asAdmin_succeeds() {
        when(configRepository.findById(10L)).thenReturn(Optional.of(config));

        TestConfigResponse response = configService.getById(10L, 99L, true);

        assertThat(response.getId()).isEqualTo(10L);
    }

    @Test
    void delete_ownConfig_success() {
        when(configRepository.findById(10L)).thenReturn(Optional.of(config));

        configService.delete(10L, 1L, false);

        verify(configRepository).delete(config);
    }

    @Test
    void delete_otherUsersConfig_asNonAdmin_throws() {
        when(configRepository.findById(10L)).thenReturn(Optional.of(config));

        assertThatThrownBy(() -> configService.delete(10L, 99L, false))
                .isInstanceOf(UnauthorizedException.class);
        verify(configRepository, never()).delete(any());
    }

    private TestConfigRequest buildRequest() {
        TestConfigRequest req = new TestConfigRequest();
        req.setName("My Test");
        req.setTargetUrl("http://example.com");
        req.setHttpMethod(HttpMethod.GET);
        req.setConcurrentUsers(10);
        req.setDurationSeconds(30);
        req.setRampUpSeconds(0);
        return req;
    }
}

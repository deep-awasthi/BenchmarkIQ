package com.benchmarkiq.service;

import com.benchmarkiq.dto.request.LoginRequest;
import com.benchmarkiq.dto.request.RegisterRequest;
import com.benchmarkiq.dto.response.AuthResponse;
import com.benchmarkiq.entity.Role;
import com.benchmarkiq.entity.User;
import com.benchmarkiq.exception.InvalidTestConfigException;
import com.benchmarkiq.repository.UserRepository;
import com.benchmarkiq.security.JwtTokenProvider;
import com.benchmarkiq.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuthenticationManager authenticationManager;
    @Mock JwtTokenProvider tokenProvider;
    @InjectMocks AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "jwtExpirationMs", 86400000L);
    }

    @Test
    void register_success() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("newuser");
        req.setEmail("new@test.com");
        req.setPassword("Password@1");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encoded");

        User saved = User.builder().id(1L).username("newuser")
                .email("new@test.com").password("encoded").role(Role.USER).build();
        when(userRepository.save(any())).thenReturn(saved);
        when(tokenProvider.generateTokenFromUserId(any(), any())).thenReturn("jwt-token");

        AuthResponse response = authService.register(req);

        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        assertThat(response.getUser().getUsername()).isEqualTo("newuser");
        assertThat(response.getUser().getRole()).isEqualTo("USER");
    }

    @Test
    void register_duplicateUsername_throws() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("admin");
        req.setEmail("other@test.com");
        req.setPassword("Password@1");

        when(userRepository.existsByUsername("admin")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(InvalidTestConfigException.class)
                .hasMessageContaining("Username already taken");
    }

    @Test
    void register_duplicateEmail_throws() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("uniqueuser");
        req.setEmail("dup@test.com");
        req.setPassword("Password@1");

        when(userRepository.existsByUsername("uniqueuser")).thenReturn(false);
        when(userRepository.existsByEmail("dup@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(InvalidTestConfigException.class)
                .hasMessageContaining("Email already registered");
    }

    @Test
    void login_success() {
        LoginRequest req = new LoginRequest();
        req.setUsername("testuser");
        req.setPassword("Password@1");

        User user = User.builder().id(2L).username("testuser")
                .email("t@test.com").role(Role.USER).build();
        UserPrincipal principal = UserPrincipal.create(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(tokenProvider.generateToken(auth)).thenReturn("login-token");
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        AuthResponse response = authService.login(req);

        assertThat(response.getAccessToken()).isEqualTo("login-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
    }

    @Test
    void login_badCredentials_throws() {
        LoginRequest req = new LoginRequest();
        req.setUsername("bad");
        req.setPassword("wrong");

        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }
}

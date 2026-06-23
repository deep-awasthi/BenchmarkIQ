package com.benchmarkiq.integration;

import com.benchmarkiq.dto.request.LoginRequest;
import com.benchmarkiq.dto.request.RegisterRequest;
import com.benchmarkiq.dto.request.TestConfigRequest;
import com.benchmarkiq.entity.HttpMethod;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TestExecutionIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String authToken;
    private Long configId;

    @BeforeEach
    void setUp() throws Exception {
        // Register unique user for each test
        String username = "integtest_" + System.nanoTime();
        RegisterRequest reg = new RegisterRequest();
        reg.setUsername(username);
        reg.setEmail(username + "@test.com");
        reg.setPassword("Password@1");

        MvcResult regResult = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode regJson = objectMapper.readTree(regResult.getResponse().getContentAsString());
        authToken = regJson.get("accessToken").asText();

        // Create test config
        TestConfigRequest configReq = new TestConfigRequest();
        configReq.setName("Integration Test Config");
        configReq.setTargetUrl("https://httpbin.org/get");
        configReq.setHttpMethod(HttpMethod.GET);
        configReq.setConcurrentUsers(2);
        configReq.setDurationSeconds(5);
        configReq.setRampUpSeconds(0);
        configReq.setMaxErrorRatePercent(50.0);

        MvcResult configResult = mockMvc.perform(post("/test-configs")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(configReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode configJson = objectMapper.readTree(configResult.getResponse().getContentAsString());
        configId = configJson.get("data").get("id").asLong();
    }

    @Test
    void createConfig_and_getById_success() throws Exception {
        mockMvc.perform(get("/test-configs/" + configId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Integration Test Config"))
                .andExpect(jsonPath("$.data.httpMethod").value("GET"));
    }

    @Test
    void listConfigs_returnsPage() throws Exception {
        mockMvc.perform(get("/test-configs")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getDashboardSummary_authenticated_success() throws Exception {
        mockMvc.perform(get("/dashboard/summary")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalConfigs").isNumber());
    }

    @Test
    void getRunningTests_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/executions/running")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void accessProtectedEndpoint_withoutToken_returns403() throws Exception {
        mockMvc.perform(get("/test-configs"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteConfig_success() throws Exception {
        mockMvc.perform(delete("/test-configs/" + configId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}

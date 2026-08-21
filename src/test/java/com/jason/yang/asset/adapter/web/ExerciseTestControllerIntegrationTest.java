package com.jason.yang.asset.adapter.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ExerciseTestControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void triageEndpointProcessesTheConfiguredOrdersFile() throws Exception {
        mockMvc.perform(post("/api/v1/test/triage")
                        .header("X-Request-Id", "test-batch-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("test-batch-001"))
                .andExpect(jsonPath("$.total").value(14))
                .andExpect(jsonPath("$.failed").value(0))
                .andExpect(jsonPath("$.dispositionCounts.AUTO_COMPLETE").value(2))
                .andExpect(jsonPath("$.outputFile").isNotEmpty())
                .andExpect(jsonPath("$.auditFile").isNotEmpty());
    }

    @Test
    void evaluateEndpointRunsTheConfiguredGoldenCases() throws Exception {
        mockMvc.perform(post("/api/v1/test/evaluate")
                        .header("X-Request-Id", "test-evaluate-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("test-evaluate-001"))
                .andExpect(jsonPath("$.cases").value(14))
                .andExpect(jsonPath("$.passed").value(14))
                .andExpect(jsonPath("$.failed").value(0))
                .andExpect(jsonPath("$.unsafeAutoCompletions").value(0))
                .andExpect(jsonPath("$.successful").value(true))
                .andExpect(jsonPath("$.reportFile").isNotEmpty());
    }
}

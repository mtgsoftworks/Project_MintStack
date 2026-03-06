package com.mintstack.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mintstack.finance.config.CorsProperties;
import com.mintstack.finance.config.RateLimitConfig;
import com.mintstack.finance.config.SecurityConfig;
import com.mintstack.finance.dto.request.UpdateRateLimitRequest;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RateLimitAdminController.class)
@Import({CorsProperties.class, SecurityConfig.class})
@AutoConfigureDataJpa
class RateLimitAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RateLimitConfig rateLimitConfig;

    @BeforeEach
    void setUp() {
        Bucket bucket = Bucket.builder()
            .addLimit(Bandwidth.simple(10_000, Duration.ofMinutes(1)))
            .build();
        when(rateLimitConfig.resolveAdminBucket(anyString())).thenReturn(bucket);
        when(rateLimitConfig.resolveUserBucket(anyString())).thenReturn(bucket);
        when(rateLimitConfig.resolveAnonymousBucket(anyString())).thenReturn(bucket);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getSettings_ShouldReturnCurrentValues() throws Exception {
        when(rateLimitConfig.isEnabled()).thenReturn(true);
        when(rateLimitConfig.getAnonymousRequestsPerMinute()).thenReturn(120);
        when(rateLimitConfig.getAuthenticatedRequestsPerMinute()).thenReturn(300);
        when(rateLimitConfig.getAdminRequestsPerMinute()).thenReturn(900);
        when(rateLimitConfig.getBucketCount()).thenReturn(42);

        mockMvc.perform(get("/api/v1/admin/rate-limit"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.enabled").value(true))
            .andExpect(jsonPath("$.data.anonymousRequestsPerMinute").value(120))
            .andExpect(jsonPath("$.data.authenticatedRequestsPerMinute").value(300))
            .andExpect(jsonPath("$.data.adminRequestsPerMinute").value(900))
            .andExpect(jsonPath("$.data.bucketCount").value(42));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateSettings_ShouldApplyNewValues() throws Exception {
        UpdateRateLimitRequest request = new UpdateRateLimitRequest();
        request.setEnabled(true);
        request.setAnonymousRequestsPerMinute(150);
        request.setAuthenticatedRequestsPerMinute(280);
        request.setAdminRequestsPerMinute(700);
        request.setClearBuckets(true);

        when(rateLimitConfig.isEnabled()).thenReturn(true);
        when(rateLimitConfig.getAnonymousRequestsPerMinute()).thenReturn(150);
        when(rateLimitConfig.getAuthenticatedRequestsPerMinute()).thenReturn(280);
        when(rateLimitConfig.getAdminRequestsPerMinute()).thenReturn(700);
        when(rateLimitConfig.getBucketCount()).thenReturn(0);

        mockMvc.perform(put("/api/v1/admin/rate-limit")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.anonymousRequestsPerMinute").value(150));

        verify(rateLimitConfig).updateSettings(
            eq(true),
            eq(150),
            eq(280),
            eq(700),
            eq(true)
        );
    }

    @Test
    @WithMockUser(roles = "USER")
    void getSettings_ShouldReturnForbidden_WhenNotAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/rate-limit"))
            .andExpect(status().isForbidden());
    }
}

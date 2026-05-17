package com.mintstack.finance.controller;

import com.mintstack.finance.config.CorsProperties;
import com.mintstack.finance.config.RateLimitConfig;
import com.mintstack.finance.config.SecurityConfig;
import com.mintstack.finance.service.DataSourceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DataSourceController.class)
@Import({CorsProperties.class, SecurityConfig.class})
@AutoConfigureDataJpa
class DataSourceControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DataSourceService dataSourceService;

    @MockitoBean
    private RateLimitConfig rateLimitConfig;

    @Test
    void getCapabilities_ShouldReturnForbidden_WhenNotAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/data-sources/capabilities")
                .with(jwt()
                    .jwt(jwt -> jwt.subject("test-user"))
                    .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void getCapabilities_ShouldReturnSuccess_WhenAdmin() throws Exception {
        when(dataSourceService.getProviderCapabilities()).thenReturn(Map.of("TCMB", Map.of("enabled", true)));

        mockMvc.perform(get("/api/v1/data-sources/capabilities")
                .with(jwt()
                    .jwt(jwt -> jwt.subject("admin-user"))
                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }
}

package com.mintstack.finance.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class ObservabilityControllerTest {

    @Test
    void controller_ShouldHaveRestControllerAnnotation() {
        assertThat(ObservabilityController.class).hasAnnotation(RestController.class);
    }

    @Test
    void getDetailedHealth_ShouldHaveAdminRoleAuthorization() throws NoSuchMethodException {
        Method method = ObservabilityController.class.getMethod("getDetailedHealth");
        
        assertThat(method).isNotNull();
        assertThat(method.isAnnotationPresent(GetMapping.class)).isTrue();
        
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).contains("hasRole('ADMIN')");
    }

    @Test
    void getMetrics_ShouldHaveAdminRoleAuthorization() throws NoSuchMethodException {
        Method method = ObservabilityController.class.getMethod("getMetrics");
        
        assertThat(method).isNotNull();
        assertThat(method.isAnnotationPresent(GetMapping.class)).isTrue();
        
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).contains("hasRole('ADMIN')");
    }
}

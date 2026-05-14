package com.mintstack.finance.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class OpenApiCompatibilityController {

    @GetMapping({"/v3/api-docs", "/v3/api-docs/**"})
    public String forwardV3ApiDocs(HttpServletRequest request) {
        String suffix = request.getRequestURI().replaceFirst("^/v3/api-docs", "");
        if (suffix == null || suffix.isBlank() || "/".equals(suffix)) {
            return "forward:/api-docs";
        }
        return "forward:/api-docs" + suffix;
    }
}

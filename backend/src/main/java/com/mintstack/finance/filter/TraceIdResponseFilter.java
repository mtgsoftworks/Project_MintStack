package com.mintstack.finance.filter;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdResponseFilter extends OncePerRequestFilter {

    private static final Pattern TRACE_ID_PATTERN = Pattern.compile("^[0-9a-f]{32}$");
    private static final Pattern SHORT_TRACE_ID_PATTERN = Pattern.compile("^[0-9a-f]{16}$");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ObjectProvider<Tracer> tracerProvider;

    public TraceIdResponseFilter(ObjectProvider<Tracer> tracerProvider) {
        this.tracerProvider = tracerProvider;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String traceId = resolveTraceId(request);

        MDC.put("traceId", traceId);
        response.setHeader("X-Trace-Id", traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("traceId");
        }
    }

    private String resolveTraceId(HttpServletRequest request) {
        String traceId = traceIdFromTracer();
        if (traceId == null) {
            traceId = request.getHeader("X-Trace-Id");
        }
        return normalizeTraceId(traceId);
    }

    private String traceIdFromTracer() {
        Tracer tracer = tracerProvider.getIfAvailable();
        if (tracer == null) {
            return null;
        }

        Span span = tracer.currentSpan();
        if (span == null || span.context() == null) {
            return null;
        }
        return span.context().traceId();
    }

    private String normalizeTraceId(String candidate) {
        if (candidate == null) {
            return generateTraceId();
        }

        String normalized = candidate.trim().toLowerCase(Locale.ROOT);
        if (TRACE_ID_PATTERN.matcher(normalized).matches()) {
            return normalized;
        }
        if (SHORT_TRACE_ID_PATTERN.matcher(normalized).matches()) {
            return "0000000000000000" + normalized;
        }
        return generateTraceId();
    }

    private String generateTraceId() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        StringBuilder builder = new StringBuilder(32);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }
}

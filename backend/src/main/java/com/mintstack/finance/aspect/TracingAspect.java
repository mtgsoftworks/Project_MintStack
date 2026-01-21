package com.mintstack.finance.aspect;

import com.mintstack.finance.service.event.EventPublisher;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class TracingAspect {

    private final Tracer tracer;
    private final EventPublisher eventPublisher;

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void controllerMethods() {}

    @Pointcut("within(@org.springframework.stereotype.Service *)")
    public void serviceMethods() {}

    @Around("controllerMethods() || serviceMethods()")
    public Object traceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        String operationName = className + "." + methodName;

        Span currentSpan = tracer.currentSpan();
        String traceId = currentSpan != null ? currentSpan.context().traceId() : "no-trace";
        String spanId = currentSpan != null ? currentSpan.context().spanId() : "no-span";

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;

            // Log successful execution
            if (duration > 1000) {
                log.warn("Slow operation: {} took {}ms [traceId={}]", operationName, duration, traceId);
                publishLogEvent("WARN", "Slow operation detected", operationName, traceId, spanId, duration);
            }

            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            
            // Log error with trace context
            log.error("Error in {} [traceId={}]: {}", operationName, traceId, e.getMessage());
            publishLogEvent("ERROR", e.getMessage(), operationName, traceId, spanId, duration);
            
            throw e;
        }
    }

    private void publishLogEvent(String level, String message, String logger, 
                                  String traceId, String spanId, long duration) {
        try {
            eventPublisher.publishLogEvent(level, message, logger, Map.of(
                    "traceId", traceId,
                    "spanId", spanId,
                    "durationMs", duration
            ));
        } catch (Exception e) {
            log.debug("Failed to publish log event: {}", e.getMessage());
        }
    }
}

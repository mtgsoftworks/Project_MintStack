package com.mintstack.finance.aspect;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Aspect for measuring and recording performance metrics of service methods.
 * Uses Micrometer to record method execution times as Prometheus metrics.
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class PerformanceAspect {

    private final MeterRegistry meterRegistry;

    /**
     * Pointcut for external API client methods.
     */
    @Pointcut("within(com.mintstack.finance.service.external..*)")
    public void externalApiPointcut() {
        // External API calls
    }

    /**
     * Pointcut for repository methods.
     */
    @Pointcut("within(com.mintstack.finance.repository..*)")
    public void repositoryPointcut() {
        // Repository calls
    }

    /**
     * Measure external API call durations and record them as metrics.
     */
    @Around("externalApiPointcut()")
    public Object measureExternalApiCall(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        long start = System.nanoTime();
        String status = "success";

        try {
            return joinPoint.proceed();
        } catch (Throwable ex) {
            status = "error";
            throw ex;
        } finally {
            long duration = System.nanoTime() - start;

            Timer.builder("external.api.call.duration")
                .tag("client", className)
                .tag("method", methodName)
                .tag("status", status)
                .description("Duration of external API calls")
                .register(meterRegistry)
                .record(duration, TimeUnit.NANOSECONDS);

            if (duration > TimeUnit.SECONDS.toNanos(5)) {
                log.warn("SLOW EXTERNAL CALL: {}.{}() took {}ms",
                    className, methodName,
                    TimeUnit.NANOSECONDS.toMillis(duration));
            }
        }
    }

    /**
     * Measure repository query durations.
     */
    @Around("repositoryPointcut()")
    public Object measureRepositoryCall(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        long start = System.nanoTime();
        String status = "success";

        try {
            return joinPoint.proceed();
        } catch (Throwable ex) {
            status = "error";
            throw ex;
        } finally {
            long duration = System.nanoTime() - start;

            Timer.builder("repository.query.duration")
                .tag("repository", className)
                .tag("method", methodName)
                .tag("status", status)
                .description("Duration of repository queries")
                .register(meterRegistry)
                .record(duration, TimeUnit.NANOSECONDS);
        }
    }
}

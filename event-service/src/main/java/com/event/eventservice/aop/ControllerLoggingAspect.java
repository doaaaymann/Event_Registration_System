package com.event.eventservice.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ControllerLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(ControllerLoggingAspect.class);

    @Around("within(com.event.eventservice.controller..*)")
    public Object logControllerExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            log.info("Event controller {} executed in {} ms",
                    joinPoint.getSignature().toShortString(),
                    System.currentTimeMillis() - start);
            return result;
        } catch (Throwable ex) {
            log.error("Event controller {} failed after {} ms: {}",
                    joinPoint.getSignature().toShortString(),
                    System.currentTimeMillis() - start,
                    ex.getMessage());
            throw ex;
        }
    }
}

package com.Quotes.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Before("execution(* com.example.controller.*.*(..))")
    public void logBeforeControllerMethods(JoinPoint joinPoint) {
        logger.info("Calling controller method: {}", joinPoint.getSignature().toShortString());
    }

    @Before("execution(* com.example.service.*.*(..))")
    public void logBeforeServiceMethods(JoinPoint joinPoint) {
        logger.info("Calling service method: {}", joinPoint.getSignature().toShortString());
    }

    @AfterThrowing(pointcut = "execution(* com.example.service.*.*(..))", throwing = "ex")
    public void logAfterThrowingServiceMethods(JoinPoint joinPoint, Exception ex) {
        logger.error("Exception in method: {}. Exception message: {}", joinPoint.getSignature().toShortString(), ex.getMessage());
    }
}

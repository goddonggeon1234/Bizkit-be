package com.caro.bizkit.observability.auth;

import com.caro.bizkit.observability.redis.RedisExceptionClassifier;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class RefreshTokenServiceObservationAspect {

    private final AuthRotationMetrics metrics;

    @Around("execution(* com.caro.bizkit.security.RefreshTokenService.*(..))")
    public Object observe(ProceedingJoinPoint pjp) throws Throwable {

        String method = pjp.getSignature().getName();
        String op = mapOperation(method);

        try {
            Object result = pjp.proceed();

            if ("validateAndGetUserId".equals(method) && result == null) {
                metrics.fail(op, AuthRotationFailureReason.INVALID_REFRESH);
            } else {
                metrics.success(op);
            }

            return result;

        } catch (Throwable ex) {

            String type = RedisExceptionClassifier.classify(ex);

            switch (type) {
                case "timeout" ->
                        metrics.fail(op, AuthRotationFailureReason.REDIS_TIMEOUT);
                case "connection" ->
                        metrics.fail(op, AuthRotationFailureReason.REDIS_CONNECTION);
                case "readonly" ->
                        metrics.fail(op, AuthRotationFailureReason.REDIS_READONLY);
                default ->
                        metrics.fail(op, AuthRotationFailureReason.UNEXPECTED);
            }

            throw ex;
        }
    }

    private String mapOperation(String methodName) {
        return switch (methodName) {
            case "createRefreshToken" -> "create";
            case "validateAndGetUserId" -> "validate";
            case "deleteRefreshToken" -> "delete";
            default -> "other";
        };
    }
}
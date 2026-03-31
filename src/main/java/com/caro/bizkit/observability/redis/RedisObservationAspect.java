package com.caro.bizkit.observability.redis;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class RedisObservationAspect {

    private final RedisClientMetrics metrics;

    @AfterThrowing(
            pointcut =
                "execution(* org.springframework.data.redis.core.RedisTemplate.*(..)) || " +
                "execution(* org.springframework.data.redis.core.StringRedisTemplate.*(..)) || " +
                "execution(* org.springframework.data.redis.core.ValueOperations.*(..))",
            throwing = "ex"
    )
    public void recordRedisException(Throwable ex) {
        metrics.error(RedisExceptionClassifier.classify(ex));
    }
}
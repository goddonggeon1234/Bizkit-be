package com.caro.bizkit.observability.auth;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthRotationMetrics {

    private final MeterRegistry registry;

    public void success(String op) {
        registry.counter(
                "bizkit.auth.rotation.total",
                "op", op,
                "result", "success",
                "reason", "none"
        ).increment();
    }

    public void fail(String op, AuthRotationFailureReason reason) {
        registry.counter(
                "bizkit.auth.rotation.total",
                "op", op,
                "result", "fail",
                "reason", reason.tag()
        ).increment();
    }
}

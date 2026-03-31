package com.caro.bizkit.observability.redis;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisClientMetrics {

    private final MeterRegistry registry;

    public void error(String type) {
        registry.counter("bizkit.redis.client.errors.total", "type", type).increment();
    }

    public void reconnect() {
        registry.counter("bizkit.redis.client.reconnect.total").increment();
    }
}
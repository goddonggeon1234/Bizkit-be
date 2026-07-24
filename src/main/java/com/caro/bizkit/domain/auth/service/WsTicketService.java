package com.caro.bizkit.domain.auth.service;

import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WsTicketService {

    private static final String KEY_PREFIX = "ws:ticket:";
    private static final Duration TTL = Duration.ofSeconds(30);

    private final StringRedisTemplate redisTemplate;

    public String issueTicket(Integer userId) {
        String ticket = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(KEY_PREFIX + ticket, String.valueOf(userId), TTL);
        return ticket;
    }

    public Integer validateAndConsume(String ticket) {
        String userId = redisTemplate.opsForValue().getAndDelete(KEY_PREFIX + ticket);
        if (userId == null) {
            return null;
        }
        return Integer.valueOf(userId);
    }
}

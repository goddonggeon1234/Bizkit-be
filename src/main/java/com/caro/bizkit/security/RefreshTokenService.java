package com.caro.bizkit.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String USER_KEY_PREFIX = "refresh_token:user:";
    private static final String LOOKUP_KEY_PREFIX = "refresh_token:lookup:";

    private final StringRedisTemplate redisTemplate;
    private final JwtProperties jwtProperties;

    public String createRefreshToken(Integer userId) {
        deleteRefreshToken(userId);

        String refreshToken = UUID.randomUUID().toString();
        String hashedToken = hashToken(refreshToken);
        long ttl = jwtProperties.getRefreshTokenValiditySeconds();

        String userKey = USER_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(userKey, hashedToken, ttl, TimeUnit.SECONDS);

        String lookupKey = LOOKUP_KEY_PREFIX + hashedToken;
        redisTemplate.opsForValue().set(lookupKey, String.valueOf(userId), ttl, TimeUnit.SECONDS);

        // 쓰기 직후 검증
        String verifyUser = redisTemplate.opsForValue().get(userKey);
        String verifyLookup = redisTemplate.opsForValue().get(lookupKey);
        Long verifyTtl = redisTemplate.getExpire(lookupKey, TimeUnit.SECONDS);
        log.info("RefreshToken 생성: userId={}, userKey={}, lookupKey={}, hashedToken={}", userId, userKey, lookupKey, hashedToken);
        log.info("RefreshToken 쓰기 검증: userKey 존재={}, lookupKey 존재={}, lookupKey TTL={}초", verifyUser != null, verifyLookup != null, verifyTtl);

        return refreshToken;
    }

    public Integer validateAndGetUserId(String refreshToken) {
        String hashedToken = hashToken(refreshToken);
        String lookupKey = LOOKUP_KEY_PREFIX + hashedToken;

        log.info("RefreshToken 검증 시작: rawToken={}, lookupKey={}, hashedToken={}", refreshToken, lookupKey, hashedToken);

        String userIdStr = redisTemplate.opsForValue().get(lookupKey);
        if (userIdStr == null) {
            log.warn("RefreshToken 검증 실패: lookupKey에 해당하는 userId 없음. lookupKey={}", lookupKey);
            return null;
        }

        Integer userId = Integer.valueOf(userIdStr);
        String userKey = USER_KEY_PREFIX + userId;
        String storedHash = redisTemplate.opsForValue().get(userKey);

        log.info("RefreshToken 검증: userId={}, userKey={}, storedHash={}, hashedToken 일치={}", userId, userKey, storedHash, hashedToken.equals(storedHash));

        if (storedHash == null || !storedHash.equals(hashedToken)) {
            log.warn("RefreshToken 검증 실패: 해시 불일치. userId={}, storedHash={}, hashedToken={}", userId, storedHash, hashedToken);
            return null;
        }

        return userId;
    }

    public void deleteRefreshToken(Integer userId) {
        String userKey = USER_KEY_PREFIX + userId;
        String storedHash = redisTemplate.opsForValue().get(userKey);

        log.info("RefreshToken 삭제: userId={}, userKey={}, storedHash={}", userId, userKey, storedHash);

        if (storedHash != null) {
            String lookupKey = LOOKUP_KEY_PREFIX + storedHash;
            redisTemplate.delete(lookupKey);
        }
        redisTemplate.delete(userKey);
    }

    public long getRefreshTokenValiditySeconds() {
        return jwtProperties.getRefreshTokenValiditySeconds();
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}

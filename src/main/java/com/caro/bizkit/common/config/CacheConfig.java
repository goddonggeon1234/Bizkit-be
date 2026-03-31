package com.caro.bizkit.common.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    /**
     * Redis CacheManager — 분산 캐시 (UserPrincipal)
     *
     * GenericJackson2JsonRedisSerializer + default typing ObjectMapper 조합:
     * - JSON에 @class 타입 정보 포함 → 역직렬화 시 LinkedHashMap 캐스팅 오류 방지
     * - Spring Boot ObjectMapper를 복사해 ParameterNamesModule 등 기존 설정 유지
     * - activateDefaultTyping(NON_FINAL) → 비-final 타입에 @class 추가
     * - record(final) 등 캐싱 대상 클래스는 @JsonTypeInfo(CLASS) 어노테이션으로 @class 삽입
     * - allowIfSubType("com.caro.bizkit.") → @class 실제 값이 자사 패키지일 때만 허용 (보안 화이트리스트)
     * - @class 필드 포함 → 패키지 경로 변경 시 역직렬화 실패 주의
     */
    @Bean("redisCacheManager")
    @Primary
    public CacheManager redisCacheManager(RedisConnectionFactory cf, ObjectMapper objectMapper) {
        ObjectMapper redisMapper = objectMapper.copy()
                .activateDefaultTyping(
                        BasicPolymorphicTypeValidator.builder()
                                .allowIfSubType("com.caro.bizkit.")
                                .build(),
                        ObjectMapper.DefaultTyping.NON_FINAL,
                        JsonTypeInfo.As.PROPERTY
                );
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(redisMapper);

        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues();

        return RedisCacheManager.builder(cf)
                .withCacheConfiguration("principal", base.entryTtl(Duration.ofMinutes(30)))
                .build();
    }

    /**
     * Caffeine CacheManager — 로컬 캐시 (skills, tags)
     *
     * 준정적 데이터 (DB 변경 거의 없음). 인스턴스별 독립 캐시로 Redis 왕복 제거.
     */
    @Bean("caffeineCacheManager")
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(1)));
        manager.setCacheNames(List.of("skills", "tags"));
        return manager;
    }

    /**
     * Redis 장애 시 서비스 전체 장애로 전파되지 않도록 예외를 삼키고 DB fallback 허용.
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new NoOpCacheErrorHandler();
    }

    private static class NoOpCacheErrorHandler implements CacheErrorHandler {
        private static final Logger log = LoggerFactory.getLogger(NoOpCacheErrorHandler.class);

        @Override
        public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
            log.warn("Cache GET 실패 — DB fallback. cache={} key={}: {}", cache.getName(), key, e.getMessage());
        }

        @Override
        public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {
            log.warn("Cache PUT 실패. cache={} key={}: {}", cache.getName(), key, e.getMessage());
        }

        @Override
        public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) {
            log.warn("Cache EVICT 실패. cache={} key={}: {}", cache.getName(), key, e.getMessage());
        }

        @Override
        public void handleCacheClearError(RuntimeException e, Cache cache) {
            log.warn("Cache CLEAR 실패. cache={}: {}", cache.getName(), e.getMessage());
        }
    }
}

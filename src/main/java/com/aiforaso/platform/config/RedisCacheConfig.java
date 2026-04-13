package com.aiforaso.platform.config;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
public class RedisCacheConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheConfig.class);
    private static final String CACHE_KEY_PREFIX = "drug-platform:v2:";

    @Bean
    public CacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory, ObjectMapper objectMapper) {
        ObjectMapper cacheObjectMapper = objectMapper.copy();
        cacheObjectMapper.registerModule(new JavaTimeModule());

        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build();
        cacheObjectMapper.activateDefaultTypingAsProperty(
                ptv,
                ObjectMapper.DefaultTyping.EVERYTHING,
                "@class");

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(cacheObjectMapper);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(15))
                .disableCachingNullValues()
                .prefixCacheNameWith(CACHE_KEY_PREFIX)
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(serializer));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration("rag-results", defaultConfig.entryTtl(Duration.ofMinutes(20)))
                .withCacheConfiguration("analysis-results", defaultConfig.entryTtl(Duration.ofMinutes(10)))
                .withCacheConfiguration("literature-overview", defaultConfig.entryTtl(Duration.ofHours(1)))
                .build();
    }

    @Bean
    public CacheErrorHandler cacheErrorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Redis cache GET failed for cache={}, key={}. The broken entry will be evicted. Cause: {}",
                        cache == null ? "unknown" : cache.getName(),
                        key,
                        exception.getMessage());
                evictQuietly(cache, key);
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("Redis cache PUT failed for cache={}, key={}. Cause: {}",
                        cache == null ? "unknown" : cache.getName(),
                        key,
                        exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Redis cache EVICT failed for cache={}, key={}. Cause: {}",
                        cache == null ? "unknown" : cache.getName(),
                        key,
                        exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("Redis cache CLEAR failed for cache={}. Cause: {}",
                        cache == null ? "unknown" : cache.getName(),
                        exception.getMessage());
            }

            private void evictQuietly(Cache cache, Object key) {
                if (cache == null || key == null) {
                    return;
                }
                try {
                    cache.evict(key);
                } catch (RuntimeException ignored) {
                    log.warn("Failed to evict broken redis cache entry for cache={}, key={}.",
                            cache.getName(),
                            key);
                }
            }
        };
    }
}

package com.localfit.global.config;


import com.localfit.domain.recommendation.dto.RecommendationResponse;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@EnableCaching
@Configuration
public class RedisConfig {

    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration recommendationConfig = cacheConfig(RecommendationResponse.class);
        RedisCacheConfiguration favoriteScoresConfig = cacheConfig(List.class);

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(recommendationConfig)
                .withInitialCacheConfigurations(Map.of(
                        "recommendation", recommendationConfig,
                        "favoriteScores", favoriteScoresConfig
                ))
                .build();
    }

    private <T> RedisCacheConfiguration cacheConfig(Class<T> type) {
        JacksonJsonRedisSerializer<T> serializer = new JacksonJsonRedisSerializer<>(type);

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(DEFAULT_TTL)
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(serializer))
                .disableCachingNullValues();
    }

}

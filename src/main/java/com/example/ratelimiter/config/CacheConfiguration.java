package com.example.ratelimiter.config;

import com.example.ratelimiter.adapter.out.cache.LocalRuleCacheEvictor;
import com.example.ratelimiter.adapter.out.cache.RedisBroadcastRuleCacheInvalidator;
import java.util.UUID;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * MUC5: Caffeine-backed Spring cache (size, TTL and statistics come from
 * {@code spring.cache.caffeine.spec}) plus cross-instance invalidation over Redis pub/sub.
 */
@Configuration(proxyBeanMethods = false)
@EnableCaching
public class CacheConfiguration {

    @Bean
    public LocalRuleCacheEvictor localRuleCacheEvictor() {
        return new LocalRuleCacheEvictor();
    }

    @Bean
    public RedisBroadcastRuleCacheInvalidator ruleCacheInvalidator(
            LocalRuleCacheEvictor localEvictor, StringRedisTemplate redis) {
        return new RedisBroadcastRuleCacheInvalidator(
                localEvictor, redis, UUID.randomUUID().toString());
    }

    @Bean
    public RedisMessageListenerContainer ruleCacheInvalidationListener(
            RedisConnectionFactory connectionFactory,
                    RedisBroadcastRuleCacheInvalidator invalidator) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(
                invalidator, new ChannelTopic(RedisBroadcastRuleCacheInvalidator.CHANNEL));
        return container;
    }
}

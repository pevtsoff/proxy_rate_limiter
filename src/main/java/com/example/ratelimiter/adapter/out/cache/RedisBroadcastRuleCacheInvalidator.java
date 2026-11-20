package com.example.ratelimiter.adapter.out.cache;

import com.example.ratelimiter.application.port.out.RuleCacheInvalidationPort;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Keeps the in-process caches of all proxy instances consistent: the local cache is evicted
 * synchronously, then the change is broadcast over Redis pub/sub so every other instance evicts
 * its own copy. Messages sent by this instance are ignored on receipt.
 */
public class RedisBroadcastRuleCacheInvalidator implements RuleCacheInvalidationPort,
        MessageListener {

    public static final String CHANNEL = "proxy-rate-limiter:rule-cache:invalidate";

    private static final Logger LOG =
            LoggerFactory.getLogger(RedisBroadcastRuleCacheInvalidator.class);

    private final LocalRuleCacheEvictor localEvictor;
    private final StringRedisTemplate redis;
    private final String instanceId;

    public RedisBroadcastRuleCacheInvalidator(
            LocalRuleCacheEvictor localEvictor, StringRedisTemplate redis, String instanceId) {
        this.localEvictor = localEvictor;
        this.redis = redis;
        this.instanceId = instanceId;
    }

    @Override
    public void invalidateAll() {
        localEvictor.evictAll();
        try {
            redis.convertAndSend(CHANNEL, instanceId);
        } catch (RuntimeException exception) {
            // The change is committed and the local cache is clean; other instances fall back
            // to the cache TTL. Logged, not rethrown, so the admin operation still succeeds.
            LOG.error("Could not broadcast rule cache invalidation", exception);
        }
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String sender = new String(message.getBody(), StandardCharsets.UTF_8);
        if (!instanceId.equals(sender)) {
            LOG.info("Rule cache invalidation received from instance {}", sender);
            localEvictor.evictAll();
        }
    }
}

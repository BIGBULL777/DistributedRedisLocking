package org.kp.distributedredislocking.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.redis.util.RedisLockRegistry;

@Configuration
public class RedisConfig {

    /**
     * Defines the central RedisLockRegistry bean.
     * This registry will be injected into our service layer.
     *
     * @param connectionFactory The Redis connection factory automatically configured by Spring.
     * @return A configured RedisLockRegistry instance.
     */
    @Bean
    public RedisLockRegistry lockRegistry(RedisConnectionFactory connectionFactory) {
        String registryKey = "LOCK";

        // The default lock TTL (Time-To-Live) in milliseconds.
        // This is a safety measure: if a server crashes while holding a lock,
        // Redis will automatically release it after 30 seconds.
        long expireAfter = 30000; // 30 seconds

        return new RedisLockRegistry(connectionFactory, registryKey, expireAfter);
    }
}
package org.kp.distributedredislocking.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.integration.redis.util.RedisLockRegistry;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class RedisClusterConfig {

    /**
     * Injects the comma-separated string of cluster nodes (e.g., localhost:7000,localhost:7001).
     * This value is read from the 'spring.data.redis.cluster.nodes' property in application.properties.
     */
    @Value("${spring.data.redis.cluster.nodes}")
    private String clusterNodes;

    /**
     * Defines the RedisConnectionFactory configured explicitly for Cluster Mode.
     * The node list is parsed from the injected property string.
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        List<String> nodeList = Arrays.stream(clusterNodes.split(","))
                .map(String::trim)
                .collect(Collectors.toList());

        RedisClusterConfiguration clusterConfiguration = new RedisClusterConfiguration(nodeList);
        return new LettuceConnectionFactory(clusterConfiguration);
    }
    @Bean
    public RedisLockRegistry lockRegistryDistributed(RedisConnectionFactory connectionFactory) {
        // Use a unique registry key prefix
        String registryKey = "LOCKING_UTIL_REGISTRY";
        // 30 seconds expiration time as a safety measure
        long expireAfter = 30000;

        return new RedisLockRegistry(connectionFactory, registryKey, expireAfter);
    }
}
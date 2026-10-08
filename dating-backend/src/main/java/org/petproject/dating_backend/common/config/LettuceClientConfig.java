package org.petproject.dating_backend.common.config;

import io.lettuce.core.RedisClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Configuration
public class LettuceClientConfig {

    @Bean
    public RedisClient redisClient(LettuceConnectionFactory connectionFactory) {
        Object nativeClient = connectionFactory.getNativeClient();
        if (nativeClient instanceof RedisClient client) {
            return client;
        }
        throw new IllegalStateException(
                "Expected a Lettuce RedisClient, but got: " +
                        (nativeClient != null ? nativeClient.getClass().getName() : "null")
        );
    }
}

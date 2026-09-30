package com.crawler.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class RedisConfig {

    // =========================================================
    // REDIS TEMPLATE
    // =========================================================

    @Bean
    public StringRedisTemplate stringRedisTemplate(
            RedisConnectionFactory connectionFactory
    ) {
        return new StringRedisTemplate(connectionFactory);
    }

    // =========================================================
    // CRAWLER CONTROL CHANNEL
    // =========================================================

    @Bean
    public ChannelTopic crawlerControlTopic() {

        return new ChannelTopic(
                "crawler:control"
        );
    }

    // =========================================================
    // REDIS MESSAGE LISTENER CONTAINER
    // =========================================================

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory
    ) {

        RedisMessageListenerContainer container =
                new RedisMessageListenerContainer();

        container.setConnectionFactory(
                connectionFactory
        );

        return container;
    }
}
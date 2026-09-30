package com.crawler.config;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;

@Component
public class CrawlerControlPublisher {

    private final StringRedisTemplate redisTemplate;
    private final ChannelTopic crawlerControlTopic;

    public CrawlerControlPublisher(
            StringRedisTemplate redisTemplate,
            ChannelTopic crawlerControlTopic
    ) {
        this.redisTemplate = redisTemplate;
        this.crawlerControlTopic = crawlerControlTopic;
    }

    // =========================================================
    // PUBLISH START
    // =========================================================

    public void publishStart(String seedUrl) {

        if (seedUrl == null || seedUrl.isBlank()) {
            throw new IllegalArgumentException(
                    "Seed URL cannot be null or empty."
            );
        }

        String message =
                "START|" + seedUrl.trim();

        redisTemplate.convertAndSend(
                crawlerControlTopic.getTopic(),
                message
        );

        System.out.println(
                "Published crawler START command: "
                        + message
        );
    }

    // =========================================================
    // PUBLISH STOP
    // =========================================================

    public void publishStop() {

        redisTemplate.convertAndSend(
                crawlerControlTopic.getTopic(),
                "STOP"
        );

        System.out.println(
                "Published crawler STOP command."
        );
    }
}
package com.crawler.config;

import java.nio.charset.StandardCharsets;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import com.crawler.concurrency.CrawlCoordinator;
import com.crawler.crawler.CrawlTask;

@Component
public class CrawlerControlListener implements MessageListener {

    private final CrawlCoordinator crawlCoordinator;

    public CrawlerControlListener(
            RedisMessageListenerContainer listenerContainer,
            ChannelTopic crawlerControlTopic,
            CrawlCoordinator crawlCoordinator
    ) {

        this.crawlCoordinator = crawlCoordinator;

        /*
         * Subscribe this crawler instance to the
         * distributed crawler control channel.
         */
        listenerContainer.addMessageListener(
                this,
                crawlerControlTopic
        );
    }

    // =========================================================
    // RECEIVE REDIS COMMAND
    // =========================================================

    @Override
    public void onMessage(
            Message message,
            byte[] pattern
    ) {

        if (message == null || message.getBody() == null) {
            return;
        }

        String command =
                new String(
                        message.getBody(),
                        StandardCharsets.UTF_8
                ).trim();

        System.out.println(
                "Received crawler control command: "
                        + command
        );

        // =====================================================
        // START
        // =====================================================

        if (command.startsWith("START|")) {

            String seedUrl =
                    command.substring("START|".length()).trim();

            if (seedUrl.isBlank()) {

                System.err.println(
                        "START command received without seed URL."
                );

                return;
            }

            /*
             * Register the seed URL in the distributed
             * visited set and add it to Redis Stream.
             *
             * SADD guarantees that only one crawler instance
             * actually adds the seed.
             */
            crawlCoordinator.submitTask(
                    new CrawlTask(
                            seedUrl,
                            0
                    )
            );

            /*
             * Every crawler instance starts its LOCAL
             * Redis Stream consumer.
             */
            if (!crawlCoordinator.isRunning()) {

                System.out.println(
                        "Starting crawler on this instance..."
                );

                crawlCoordinator.start();
            }

            return;
        }

        // =====================================================
        // STOP
        // =====================================================

        if ("STOP".equalsIgnoreCase(command)) {

            if (crawlCoordinator.isRunning()) {

                System.out.println(
                        "Stopping crawler on this instance..."
                );

                crawlCoordinator.stop();
            }

            return;
        }

        // =====================================================
        // UNKNOWN COMMAND
        // =====================================================

        System.out.println(
                "Unknown crawler control command: "
                        + command
        );
    }
}
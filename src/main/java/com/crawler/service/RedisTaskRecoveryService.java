package com.crawler.service;

import java.time.Duration;
import java.util.List;

import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.crawler.concurrency.CrawlCoordinator;
import com.crawler.crawler.CrawlTask;

@Service
public class RedisTaskRecoveryService {

    private static final String STREAM_KEY = "crawler:stream";

    private static final String CONSUMER_GROUP = "crawler-group";

    /*
     * A task must be idle for at least 30 seconds
     * before another crawler instance can recover it.
     */
    private static final Duration MIN_IDLE_TIME =
            Duration.ofSeconds(30);

    /*
     * Maximum number of pending messages inspected
     * during one recovery cycle.
     */
    private static final long MAX_PENDING_MESSAGES = 100;

    private final StringRedisTemplate redisTemplate;

    private final CrawlCoordinator crawlCoordinator;

    public RedisTaskRecoveryService(
            StringRedisTemplate redisTemplate,
            CrawlCoordinator crawlCoordinator
    ) {
        this.redisTemplate = redisTemplate;
        this.crawlCoordinator = crawlCoordinator;
    }

    // =========================================================
    // RECOVER ABANDONED TASKS
    // =========================================================

    @Scheduled(fixedDelay = 10000)
    public void recoverTasks() {

        /*
         * Recovery must NEVER start crawling by itself.
         *
         * The CrawlCoordinator controls whether the crawler
         * is currently running.
         */
        if (!crawlCoordinator.isRunning()) {
            return;
        }

        try {

            PendingMessages pendingMessages =
                    redisTemplate
                            .opsForStream()
                            .pending(
                                    STREAM_KEY,
                                    CONSUMER_GROUP,
                                    Range.unbounded(),
                                    MAX_PENDING_MESSAGES
                            );

            if (pendingMessages == null ||
                    pendingMessages.isEmpty()) {

                return;
            }

            /*
             * Find tasks that have not been delivered for
             * at least MIN_IDLE_TIME.
             */
            List<RecordId> abandonedMessageIds =
                    pendingMessages
                            .stream()
                            .filter(message ->
                                    message
                                            .getElapsedTimeSinceLastDelivery()
                                            .compareTo(MIN_IDLE_TIME) >= 0
                            )
                            .map(message -> message.getId())
                            .toList();

            if (abandonedMessageIds.isEmpty()) {
                return;
            }

            /*
             * /stop could have been called while we were
             * checking the pending messages.
             *
             * Check again before claiming them.
             */
            if (!crawlCoordinator.isRunning()) {
                return;
            }

            System.out.println(
                    "[RECOVERY] Found "
                            + abandonedMessageIds.size()
                            + " abandoned task(s)."
            );

            /*
             * Every Docker crawler container gets a unique
             * HOSTNAME.
             *
             * Therefore each crawler becomes a different
             * Redis Stream consumer.
             */
            String consumerName = getConsumerName();

            List<MapRecord<String, Object, Object>> records =
                    redisTemplate
                            .opsForStream()
                            .claim(
                                    STREAM_KEY,
                                    CONSUMER_GROUP,
                                    consumerName,
                                    MIN_IDLE_TIME,
                                    abandonedMessageIds.toArray(
                                            new RecordId[0]
                                    )
                            );

            if (records == null || records.isEmpty()) {
                return;
            }

            for (MapRecord<String, Object, Object> record : records) {

                /*
                 * Stop recovery immediately if the crawler
                 * has been stopped.
                 */
                if (!crawlCoordinator.isRunning()) {

                    System.out.println(
                            "[RECOVERY] Crawler stopped. "
                                    + "Recovery terminated."
                    );

                    return;
                }

                processRecoveredTask(record);
            }

        } catch (Exception e) {

            System.err.println(
                    "[RECOVERY] Failed to recover Redis tasks: "
                            + e.getMessage()
            );

            e.printStackTrace();
        }
    }

    // =========================================================
    // PROCESS RECOVERED TASK
    // =========================================================

    private void processRecoveredTask(
            MapRecord<String, Object, Object> record
    ) {

        if (record == null) {
            return;
        }

        try {

            Object urlObject =
                    record
                            .getValue()
                            .get("url");

            Object depthObject =
                    record
                            .getValue()
                            .get("depth");

            /*
             * Ignore the artificial initialization message
             * created when the Redis Stream is initialized.
             */
            if (urlObject != null &&
                    "__stream_init__".equals(
                            urlObject.toString()
                    )) {

                acknowledge(record.getId());

                return;
            }

            /*
             * Validate URL.
             */
            if (urlObject == null) {

                System.err.println(
                        "[RECOVERY] Invalid task: URL is null. "
                                + "messageId="
                                + record.getId()
                );

                acknowledge(record.getId());

                return;
            }

            String url = urlObject.toString();

            if (url.isBlank()) {

                System.err.println(
                        "[RECOVERY] Invalid task: URL is blank. "
                                + "messageId="
                                + record.getId()
                );

                acknowledge(record.getId());

                return;
            }

            /*
             * Validate depth.
             */
            if (depthObject == null) {

                System.err.println(
                        "[RECOVERY] Invalid task: depth is null. "
                                + "url="
                                + url
                );

                acknowledge(record.getId());

                return;
            }

            int depth;

            try {

                depth = Integer.parseInt(
                        depthObject.toString()
                );

            } catch (NumberFormatException e) {

                System.err.println(
                        "[RECOVERY] Invalid depth: "
                                + depthObject
                                + " | url="
                                + url
                );

                acknowledge(record.getId());

                return;
            }

            /*
             * Reuse the ORIGINAL Redis Stream message ID.
             *
             * Do NOT create a new Redis Stream message.
             */
            String messageId =
                    record
                            .getId()
                            .getValue();

            System.out.println(
                    "[RECOVERY] Recovering task -> "
                            + url
                            + " | depth="
                            + depth
                            + " | messageId="
                            + messageId
                            + " | consumer="
                            + getConsumerName()
            );

            CrawlTask recoveredTask =
                    new CrawlTask(
                            url,
                            depth,
                            messageId
                    );

            /*
             * Final state check before submitting.
             */
            if (!crawlCoordinator.isRunning()) {

                System.out.println(
                        "[RECOVERY] Crawler stopped before "
                                + "submitting task -> "
                                + url
                );

                return;
            }

            /*
             * IMPORTANT:
             *
             * Recovered tasks go through CrawlCoordinator.
             *
             * They do NOT directly create Workers.
             *
             * They do NOT directly execute the crawl.
             */
            crawlCoordinator.submitTask(
                    recoveredTask
            );

            System.out.println(
                    "[RECOVERY] Task submitted to coordinator -> "
                            + url
            );

        } catch (Exception e) {

            System.err.println(
                    "[RECOVERY] Failed to process recovered task: "
                            + e.getMessage()
            );

            e.printStackTrace();
        }
    }

    // =========================================================
    // CONSUMER NAME
    // =========================================================

    private String getConsumerName() {

        /*
         * Docker Compose automatically gives each container
         * its own hostname.
         *
         * Example:
         *
         * crawler-1
         * crawler-2
         * crawler-3
         *
         * Therefore Redis sees them as separate consumers.
         */
        String hostname =
                System.getenv("HOSTNAME");

        if (hostname == null ||
                hostname.isBlank()) {

            return "crawler-recovery-"
                    + System.currentTimeMillis();
        }

        return hostname;
    }

    // =========================================================
    // ACKNOWLEDGE INVALID / INITIALIZATION TASK
    // =========================================================

    private void acknowledge(
            RecordId messageId
    ) {

        if (messageId == null) {
            return;
        }

        try {

            Long acknowledged =
                    redisTemplate
                            .opsForStream()
                            .acknowledge(
                                    STREAM_KEY,
                                    CONSUMER_GROUP,
                                    messageId
                            );

            System.out.println(
                    "[RECOVERY] ACK -> "
                            + messageId
                            + " | acknowledged="
                            + acknowledged
            );

        } catch (Exception e) {

            System.err.println(
                    "[RECOVERY] Failed to ACK message "
                            + messageId
                            + ": "
                            + e.getMessage()
            );
        }
    }
}
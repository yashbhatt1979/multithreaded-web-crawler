
package com.crawler.crawler;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class CrawlQueue {

    private static final String STREAM_KEY =
            "crawler:stream";

    private static final String CONSUMER_GROUP =
            "crawler-group";

    private static final String STREAM_INIT_URL =
            "__stream_init__";

    private final String consumerName;
    private final StringRedisTemplate redisTemplate;

    public CrawlQueue(
            StringRedisTemplate redisTemplate
    ) {
        this.redisTemplate = redisTemplate;

        String hostname =
                System.getenv("HOSTNAME");

        if (hostname == null ||
                hostname.isBlank()) {

            hostname =
                    "crawler-"
                            + System.currentTimeMillis();
        }

        this.consumerName = hostname;

        initializeConsumerGroup();
    }

    // =========================================================
    // INITIALIZE CONSUMER GROUP
    // =========================================================

    private void initializeConsumerGroup() {

        try {

            /*
             * The Redis Stream must exist before the
             * consumer group can be created.
             */
            if (Boolean.FALSE.equals(
                    redisTemplate.hasKey(STREAM_KEY)
            )) {

                createStream();
            }

            /*
             * IMPORTANT:
             *
             * "$" means that a newly created consumer group
             * starts from NEW messages only.
             *
             * Existing old messages are NOT automatically
             * consumed when the application starts.
             */
            redisTemplate
                    .opsForStream()
                    .createGroup(
                            STREAM_KEY,
                            ReadOffset.latest(),
                            CONSUMER_GROUP
                    );

            System.out.println(
                    "Redis consumer group created: "
                            + CONSUMER_GROUP
            );

        } catch (Exception e) {

            /*
             * BUSYGROUP simply means the group already exists.
             */
            if (
                    e.getMessage() == null
                            ||
                    !e.getMessage()
                            .contains("BUSYGROUP")
            ) {

                System.err.println(
                        "Failed to initialize Redis consumer group: "
                                + e.getMessage()
                );
            }
        }
    }

    // =========================================================
    // CREATE STREAM
    // =========================================================

    private void createStream() {

        redisTemplate
                .opsForStream()
                .add(
                        STREAM_KEY,
                        Map.of(
                                "url",
                                STREAM_INIT_URL,

                                "depth",
                                "-1"
                        )
                );
    }

    // =========================================================
    // RESET FOR FRESH CRAWL
    // =========================================================

    public synchronized void resetForFreshCrawl() {

        System.out.println(
                "Resetting Redis crawl queue for a fresh crawl..."
        );

        /*
         * Delete the existing stream.
         *
         * This removes:
         *
         * - old unconsumed messages
         * - pending messages
         * - old consumer state
         */
        redisTemplate.delete(
                STREAM_KEY
        );

        /*
         * Recreate the stream.
         */
        createStream();

        /*
         * Create a fresh consumer group.
         *
         * latest() ensures the artificial initialization
         * message is not treated as a crawl task.
         */
        try {

            redisTemplate
                    .opsForStream()
                    .createGroup(
                            STREAM_KEY,
                            ReadOffset.latest(),
                            CONSUMER_GROUP
                    );

        } catch (Exception e) {

            if (
                    e.getMessage() == null
                            ||
                    !e.getMessage()
                            .contains("BUSYGROUP")
            ) {

                throw new IllegalStateException(
                        "Failed to recreate Redis consumer group.",
                        e
                );
            }
        }

        System.out.println(
                "Redis crawl queue reset successfully."
        );
    }

    // =========================================================
    // ADD TASK
    // =========================================================

    public void add(CrawlTask task) {

        if (task == null ||
                task.getUrl() == null ||
                task.getUrl().isBlank()) {

            return;
        }

        Map<String, String> message =
                Map.of(
                        "url",
                        task.getUrl(),

                        "depth",
                        String.valueOf(
                                task.getDepth()
                        )
                );

        RecordId recordId =
                redisTemplate
                        .opsForStream()
                        .add(
                                STREAM_KEY,
                                message
                        );

        System.out.println(
                "Redis Stream <- "
                        + task.getUrl()
                        + " | depth="
                        + task.getDepth()
                        + " | messageId="
                        + recordId
        );
    }

    // =========================================================
    // SUBMIT RECOVERED TASK
    // =========================================================

    /*
     * A recovered task already exists in the Redis
     * Pending Entries List.
     *
     * DO NOT create another Redis Stream message.
     */
    public void submitRecoveredTask(
            CrawlTask task
    ) {

        if (task == null) {
            return;
        }

        System.out.println(
                "Recovered task submitted: "
                        + task.getUrl()
                        + " | depth="
                        + task.getDepth()
                        + " | messageId="
                        + task.getMessageId()
        );
    }

    // =========================================================
    // POLL TASK
    // =========================================================

    public CrawlTask poll() {

        while (true) {

            List<MapRecord<String, Object, Object>> records =
                    redisTemplate
                            .opsForStream()
                            .read(
                                    Consumer.from(
                                            CONSUMER_GROUP,
                                            consumerName
                                    ),

                                    StreamReadOptions
                                            .empty()
                                            .count(1)
                                            .block(
                                                    Duration.ofSeconds(5)
                                            ),

                                    StreamOffset.create(
                                            STREAM_KEY,
                                            ReadOffset.lastConsumed()
                                    )
                            );

            if (
                    records == null
                            ||
                    records.isEmpty()
            ) {

                return null;
            }

            MapRecord<String, Object, Object> record =
                    records.get(0);

            Map<Object, Object> values =
                    record.getValue();

            Object urlValue =
                    values.get("url");

            Object depthValue =
                    values.get("depth");

            if (urlValue == null ||
                    depthValue == null) {

                System.err.println(
                        "Invalid Redis Stream message: "
                                + record.getId()
                );

                acknowledge(
                        record.getId().getValue()
                );

                continue;
            }

            String url =
                    urlValue.toString();

            /*
             * Ignore the artificial stream initialization
             * message.
             */
            if (STREAM_INIT_URL.equals(url)) {

                acknowledge(
                        record.getId().getValue()
                );

                continue;
            }

            int depth;

            try {

                depth =
                        Integer.parseInt(
                                depthValue.toString()
                        );

            } catch (NumberFormatException e) {

                System.err.println(
                        "Invalid depth in Redis message: "
                                + record.getId()
                );

                acknowledge(
                        record.getId().getValue()
                );

                continue;
            }

            String messageId =
                    record
                            .getId()
                            .getValue();

            System.out.println(
                    consumerName
                            + " received: "
                            + url
                            + " | depth="
                            + depth
                            + " | messageId="
                            + messageId
            );

            /*
             * DO NOT ACK here.
             *
             * The message remains pending until Worker
             * successfully completes the task.
             */
            return new CrawlTask(
                    url,
                    depth,
                    messageId
            );
        }
    }

    // =========================================================
    // ACK TASK
    // =========================================================

    public void acknowledge(
            String messageId
    ) {

        if (
                messageId == null
                        ||
                messageId.isBlank()
        ) {

            System.err.println(
                    "Cannot ACK empty Redis message ID."
            );

            return;
        }

        Long acknowledged =
                redisTemplate
                        .opsForStream()
                        .acknowledge(
                                STREAM_KEY,
                                CONSUMER_GROUP,
                                messageId
                        );

        System.out.println(
                consumerName
                        + " ACK -> "
                        + messageId
                        + " | acknowledged="
                        + acknowledged
        );
    }

    // =========================================================
    // STREAM SIZE
    // =========================================================

    public long size() {

        Long size =
                redisTemplate
                        .opsForStream()
                        .size(
                                STREAM_KEY
                        );

        return size == null
                ? 0
                : size;
    }

    // =========================================================
    // CONSUMER NAME
    // =========================================================

    public String getConsumerName() {

        return consumerName;
    }

    // =========================================================
    // STREAM KEY
    // =========================================================

    public String getStreamKey() {

        return STREAM_KEY;
    }

    // =========================================================
    // CONSUMER GROUP
    // =========================================================

    public String getConsumerGroup() {

        return CONSUMER_GROUP;
    }
}
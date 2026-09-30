package com.crawler.concurrency;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.crawler.crawler.CrawlQueue;
import com.crawler.crawler.CrawlTask;
import com.crawler.crawler.Worker;
import com.crawler.repository.CrawledPageRepository;
import com.crawler.service.ScraperService;
import com.crawler.service.UrlCanonicalizer;
import com.crawler.service.UrlFetcherService;

@Component
public class CrawlCoordinator {

    private static final String VISITED_SET_KEY =
            "crawler:visited";

    /*
     * Number of Redis consumers PER crawler instance.
     *
     * With 3 containers:
     *
     * crawler-1 -> 3 consumers
     * crawler-2 -> 3 consumers
     * crawler-3 -> 3 consumers
     *
     * Total = 9 Redis consumers.
     */
    private static final int REDIS_CONSUMER_THREADS = 3;

    private final CrawlQueue crawlQueue;
    private final CrawlTaskExecutor taskExecutor;
    private final UrlFetcherService urlFetcherService;
    private final ScraperService scraperService;
    private final CrawledPageRepository crawledPageRepository;
    private final StringRedisTemplate redisTemplate;
    private final UrlCanonicalizer urlCanonicalizer;

    private volatile boolean running = false;

    private final List<Thread> consumerThreads =
            new ArrayList<>();

    public CrawlCoordinator(
            CrawlQueue crawlQueue,
            CrawlTaskExecutor taskExecutor,
            UrlFetcherService urlFetcherService,
            ScraperService scraperService,
            CrawledPageRepository crawledPageRepository,
            StringRedisTemplate redisTemplate,
            UrlCanonicalizer urlCanonicalizer
    ) {

        this.crawlQueue = crawlQueue;
        this.taskExecutor = taskExecutor;
        this.urlFetcherService = urlFetcherService;
        this.scraperService = scraperService;
        this.crawledPageRepository = crawledPageRepository;
        this.redisTemplate = redisTemplate;
        this.urlCanonicalizer = urlCanonicalizer;
    }

    // =========================================================
    // SUBMIT TASK
    // =========================================================

    public void submitTask(CrawlTask task) {

        if (task == null ||
                task.getUrl() == null ||
                task.getUrl().isBlank()) {

            return;
        }

        /*
         * Canonicalize URL BEFORE Redis deduplication.
         *
         * This is important in the distributed crawler because
         * crawler-1, crawler-2 and crawler-3 must all use the
         * same representation of the URL.
         */
        String canonicalUrl =
                urlCanonicalizer.canonicalize(task.getUrl());

        if (canonicalUrl == null) {

            System.out.println(
                    "Skipping invalid URL: "
                            + task.getUrl()
            );

            return;
        }

        /*
         * Redis must contain the canonical URL.
         */
        Long added =
                redisTemplate
                        .opsForSet()
                        .add(
                                VISITED_SET_KEY,
                                canonicalUrl
                        );

        if (added == null ||
                added == 0) {

            System.out.println(
                    "Skipping duplicate URL: "
                            + canonicalUrl
            );

            return;
        }

        /*
         * Create a new task using the canonical URL.
         *
         * We preserve:
         * - depth
         * - Redis message ID
         */
        CrawlTask canonicalTask =
                new CrawlTask(
                        canonicalUrl,
                        task.getDepth(),
                        task.getMessageId()
                );

        crawlQueue.add(canonicalTask);
    }

    // =========================================================
    // START
    // =========================================================

    public synchronized void start() {

        if (running) {

            System.out.println(
                    "Crawler is already running."
            );

            return;
        }

        if (taskExecutor.isShutdown()) {

            throw new IllegalStateException(
                    "Crawler worker executor has already been shut down."
            );
        }

        running = true;

        System.out.println(
                "================================================="
        );

        System.out.println(
                "Distributed crawler STARTED"
        );

        System.out.println(
                "Container = "
                        + crawlQueue.getConsumerName()
        );

        System.out.println(
                "Redis Consumer Group = "
                        + crawlQueue.getConsumerGroup()
        );

        System.out.println(
                "Redis Consumer Threads = "
                        + REDIS_CONSUMER_THREADS
        );

        System.out.println(
                "================================================="
        );

        consumerThreads.clear();

        for (int i = 0;
             i < REDIS_CONSUMER_THREADS;
             i++) {

            final int consumerNumber = i + 1;

            Thread thread =
                    new Thread(
                            () -> consumeTasks(
                                    consumerNumber
                            ),
                            "redis-consumer-"
                                    + consumerNumber
                    );

            thread.setDaemon(true);

            consumerThreads.add(thread);

            thread.start();

            System.out.println(
                    "Started Redis consumer thread: "
                            + thread.getName()
            );
        }
    }

    // =========================================================
    // REDIS CONSUMER
    // =========================================================

    private void consumeTasks(
            int consumerNumber
    ) {

        String threadName =
                Thread.currentThread()
                        .getName();

        System.out.println(
                "["
                        + crawlQueue.getConsumerName()
                        + "]["
                        + threadName
                        + "] Redis consumer started."
        );

        try {

            while (running) {

                CrawlTask task =
                        crawlQueue.poll();

                if (task == null) {

                    continue;
                }

                if (!running) {

                    System.out.println(
                            "["
                                    + threadName
                                    + "] Crawler stopped before processing: "
                                    + task.getUrl()
                    );

                    break;
                }

                System.out.println(
                        "["
                                + crawlQueue.getConsumerName()
                                + "]["
                                + threadName
                                + "] Dispatching -> "
                                + task.getUrl()
                );

                submitToWorkerPool(task);
            }

        } catch (Exception e) {

            System.err.println(
                    "["
                            + crawlQueue.getConsumerName()
                            + "]["
                            + threadName
                            + "] Redis consumer failed: "
                            + e.getMessage()
            );

            e.printStackTrace();

        } finally {

            System.out.println(
                    "["
                            + crawlQueue.getConsumerName()
                            + "]["
                            + threadName
                            + "] Redis consumer stopped."
            );
        }
    }

    // =========================================================
    // SUBMIT TO LOCAL WORKER POOL
    // =========================================================

    private void submitToWorkerPool(
            CrawlTask task
    ) {

        if (task == null) {

            return;
        }

        if (!running) {

            System.out.println(
                    "Crawler stopped. Task remains pending: "
                            + task.getUrl()
            );

            return;
        }

        Worker worker =
                new Worker(
                        task,
                        this,
                        urlFetcherService,
                        scraperService,
                        crawledPageRepository
                );

        try {

            taskExecutor.submit(() -> {

                try {

                    worker.run();

                } catch (Exception e) {

                    System.err.println(
                            "Worker crashed for URL: "
                                    + task.getUrl()
                    );

                    e.printStackTrace();
                }
            });

        } catch (RejectedExecutionException e) {

            System.err.println(
                    "Worker rejected for URL: "
                            + task.getUrl()
            );
        }
    }

    // =========================================================
    // ACK TASK
    // =========================================================

    public void acknowledgeTask(
            CrawlTask task
    ) {

        if (task == null) {

            return;
        }

        String messageId =
                task.getMessageId();

        if (messageId == null ||
                messageId.isBlank()) {

            System.err.println(
                    "Cannot ACK task without Redis message ID: "
                            + task.getUrl()
            );

            return;
        }

        crawlQueue.acknowledge(
                messageId
        );
    }

    // =========================================================
    // STOP
    // =========================================================

    public synchronized void stop() {

        if (!running) {

            System.out.println(
                    "Crawler is already stopped."
            );

            return;
        }

        System.out.println(
                "Crawler coordinator stopping..."
        );

        running = false;

        for (Thread thread :
                consumerThreads) {

            if (thread != null &&
                    thread.isAlive()) {

                thread.interrupt();
            }
        }

        consumerThreads.clear();

        System.out.println(
                "Crawler coordinator stopped."
        );
    }

    // =========================================================
    // SHUTDOWN
    // =========================================================

    public synchronized void shutdown() {

        stop();

        if (!taskExecutor.isShutdown()) {

            taskExecutor.shutdown();
        }
    }

    // =========================================================
    // CLEAR VISITED URLS
    // =========================================================

    public void clearVisitedUrls() {

        redisTemplate.delete(
                VISITED_SET_KEY
        );

        System.out.println(
                "Distributed visited URL set cleared."
        );
    }

    // =========================================================
    // VISITED COUNT
    // =========================================================

    public long getVisitedUrlCount() {

        Long size =
                redisTemplate
                        .opsForSet()
                        .size(
                                VISITED_SET_KEY
                        );

        return size == null
                ? 0
                : size;
    }

    // =========================================================
    // QUEUE SIZE
    // =========================================================

    public long getQueueSize() {

        return crawlQueue.size();
    }

    // =========================================================
    // STATUS
    // =========================================================

    public boolean isRunning() {

        return running;
    }
}
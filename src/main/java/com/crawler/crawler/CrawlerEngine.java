package com.crawler.crawler;

import org.springframework.stereotype.Component;

import com.crawler.concurrency.CrawlCoordinator;

@Component
public class CrawlerEngine {

    private final CrawlCoordinator crawlCoordinator;

    public CrawlerEngine(CrawlCoordinator crawlCoordinator) {
        this.crawlCoordinator = crawlCoordinator;
    }

    // =========================================================
    // START
    // =========================================================

    public void start(String startUrl) {

        if (startUrl == null || startUrl.isBlank()) {
            throw new IllegalArgumentException(
                    "Start URL cannot be null or empty."
            );
        }

        String normalizedUrl = startUrl.trim();

        /*
         * Do not start another crawl if the coordinator
         * is already running.
         */
        if (crawlCoordinator.isRunning()) {
            throw new IllegalStateException(
                    "Crawler is already running."
            );
        }

        /*
         * Create the initial seed task.
         *
         * This happens ONLY when /start is called.
         */
        CrawlTask initialTask =
                new CrawlTask(normalizedUrl, 0);

        /*
         * Register the seed URL and enqueue it into Redis.
         */
        crawlCoordinator.submitTask(initialTask);

        /*
         * Start the crawler consumer.
         */
        crawlCoordinator.start();
    }

    // =========================================================
    // STOP
    // =========================================================

    /*
     * Normal crawler stop.
     *
     * IMPORTANT:
     * This must NOT shut down the worker executor.
     * The executor should remain reusable for the next /start.
     */
    public void stop() {
        crawlCoordinator.stop();
    }

    // =========================================================
    // APPLICATION SHUTDOWN
    // =========================================================

    /*
     * Called only when the application itself is shutting down.
     */
    public void shutdown() {
        crawlCoordinator.shutdown();
    }
}
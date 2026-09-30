package com.crawler.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Service;

import com.crawler.config.CrawlerControlPublisher;
import com.crawler.crawler.CrawlerEngine;

@Service
public class CrawlerService {

    private final CrawlerEngine crawlerEngine;
    private final CrawlerControlPublisher controlPublisher;

    private final AtomicBoolean running =
            new AtomicBoolean(false);

    private volatile String currentSeedUrl;
    private volatile Instant startedAt;

    public CrawlerService(
            CrawlerEngine crawlerEngine,
            CrawlerControlPublisher controlPublisher
    ) {
        this.crawlerEngine = crawlerEngine;
        this.controlPublisher = controlPublisher;
    }

    // =========================================================
    // START CRAWLER
    // =========================================================

    public synchronized void startCrawl(String seedUrl) {

        if (seedUrl == null || seedUrl.isBlank()) {

            throw new IllegalArgumentException(
                    "Seed URL cannot be null or empty"
            );
        }

        if (running.get()) {

            throw new IllegalStateException(
                    "Crawler is already running"
            );
        }

        String normalizedUrl = seedUrl.trim();

        /*
         * IMPORTANT:
         *
         * Do NOT directly call crawlerEngine.start().
         *
         * Publish the START command through Redis so that
         * every crawler instance receives it.
         */
        controlPublisher.publishStart(
                normalizedUrl
        );

        /*
         * Update the API instance state.
         *
         * The actual crawler startup happens asynchronously
         * when the Redis START message is received.
         */
        currentSeedUrl = normalizedUrl;
        startedAt = Instant.now();
        running.set(true);
    }

    // =========================================================
    // STOP CRAWLER
    // =========================================================

    public synchronized void stopCrawl() {

        if (!running.get()) {
            return;
        }

        /*
         * Publish STOP through Redis.
         *
         * Every crawler instance will receive the command
         * and stop its local coordinator.
         */
        controlPublisher.publishStop();

        running.set(false);
    }

    // =========================================================
    // STATUS
    // =========================================================

    public Map<String, Object> getStatus() {

        Map<String, Object> status =
                new LinkedHashMap<>();

        status.put(
                "running",
                running.get()
        );

        status.put(
                "seedUrl",
                currentSeedUrl
        );

        status.put(
                "startedAt",
                startedAt
        );

        return status;
    }
}
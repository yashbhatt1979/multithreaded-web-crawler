package com.crawler.crawler;

import org.springframework.stereotype.Component;

import com.crawler.concurrency.CrawlCoordinator;

@Component
public class CrawlerEngine {

    private final CrawlCoordinator crawlCoordinator;

    public CrawlerEngine(CrawlCoordinator crawlCoordinator) {
        this.crawlCoordinator = crawlCoordinator;
    }

    public void start(String startUrl) {

        CrawlTask initialTask =
                new CrawlTask(startUrl);

        crawlCoordinator.submitTask(initialTask);

        crawlCoordinator.start();
    }

    public void shutdown() {
        crawlCoordinator.shutdown();
    }
}
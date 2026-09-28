package com.crawler.service;

import org.springframework.stereotype.Service;

import com.crawler.crawler.CrawlerEngine;

@Service
public class CrawlerService {

    private final CrawlerEngine crawlerEngine;

    public CrawlerService(CrawlerEngine crawlerEngine) {
        this.crawlerEngine = crawlerEngine;
    }

    public void startCrawl(String seedUrl) {

        if (seedUrl == null || seedUrl.isBlank()) {
            throw new IllegalArgumentException(
                    "Seed URL cannot be null or empty"
            );
        }

        crawlerEngine.start(seedUrl);
    }

    public void stopCrawl() {
        crawlerEngine.shutdown();
    }
}
package com.crawler.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CrawlerConfig {

    @Value("${crawler.max-depth:3}")
    private int maxDepth;

    @Value("${crawler.max-pages:100}")
    private int maxPages;

    @Value("${crawler.request-timeout-seconds:10}")
    private int requestTimeoutSeconds;

    @Value("${crawler.user-agent:Java-Web-Crawler/1.0}")
    private String userAgent;

    public int getMaxDepth() {
        return maxDepth;
    }

    public int getMaxPages() {
        return maxPages;
    }

    public int getRequestTimeoutSeconds() {
        return requestTimeoutSeconds;
    }

    public String getUserAgent() {
        return userAgent;
    }
}
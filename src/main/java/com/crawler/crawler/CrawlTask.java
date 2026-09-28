package com.crawler.crawler;

public class CrawlTask {

    private final String url;

    public CrawlTask(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }
}
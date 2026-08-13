package com.example.crawler.service;

import org.springframework.stereotype.Service;

@Service
public class CrawlerService {

    public String startCrawling(String url) {
        return "Crawling started for: " + url;
    }
}
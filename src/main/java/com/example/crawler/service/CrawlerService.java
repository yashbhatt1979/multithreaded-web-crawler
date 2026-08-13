package com.example.crawler.service;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.crawler.crawler.CrawlerEngine;

@Service
public class CrawlerService {

    private final CrawlerEngine crawlerEngine;

    public CrawlerService(CrawlerEngine crawlerEngine) {
        this.crawlerEngine = crawlerEngine;
    }

    public List<String> crawl(String url) {
        try {
            return crawlerEngine.crawl(url);

        } catch (IOException e) {
            throw new RuntimeException("Failed to crawl URL: " + url, e);
        }
    }
}
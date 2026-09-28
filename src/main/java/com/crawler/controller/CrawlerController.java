package com.crawler.controller;

import com.crawler.service.CrawlerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/crawler")
public class CrawlerController {

    private final CrawlerService crawlerService;

    public CrawlerController(CrawlerService crawlerService) {
        this.crawlerService = crawlerService;
    }

    @PostMapping("/start")
    public ResponseEntity<String> startCrawler(
            @RequestParam String url
    ) {

        crawlerService.startCrawl(url);

        return ResponseEntity.ok(
                "Crawler started for URL: " + url
        );
    }

    @PostMapping("/stop")
    public ResponseEntity<String> stopCrawler() {

        crawlerService.stopCrawl();

        return ResponseEntity.ok(
                "Crawler stopped"
        );
    }
}
package com.crawler.controller;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.crawler.service.CrawlerService;
@RestController
@RequestMapping("/api/crawler")
public class CrawlerController {

    private final CrawlerService crawlerService;

    public CrawlerController(CrawlerService crawlerService) {
        this.crawlerService = crawlerService;
    }

    // =========================================================
    // START CRAWLER
    // =========================================================

    @PostMapping("/start")
    public ResponseEntity<String> startCrawler(
            @RequestParam String url
    ) { 

        if (url == null || url.isBlank()) {

            return ResponseEntity.badRequest()
                    .body("URL must not be empty.");
        }

        crawlerService.startCrawl(url.trim());

        return ResponseEntity.ok(
                "Crawler started for URL: " + url.trim()
        );
    }

    // =========================================================
    // STOP CRAWLER
    // =========================================================

    @PostMapping("/stop")
    public ResponseEntity<String> stopCrawler() {

        crawlerService.stopCrawl();

        return ResponseEntity.ok(
                "Crawler stopped."
        );
    }

    // =========================================================
    // STATUS
    // =========================================================

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {

        return ResponseEntity.ok(
                crawlerService.getStatus()
        );
    }
}
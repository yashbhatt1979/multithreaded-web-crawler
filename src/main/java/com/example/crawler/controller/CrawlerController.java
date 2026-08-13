package com.example.crawler.controller;

import com.example.crawler.service.CrawlerService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/crawl")
public class CrawlerController {

    private final CrawlerService crawlerService;

    public CrawlerController(CrawlerService crawlerService) {
        this.crawlerService = crawlerService;
    }

    @PostMapping
    public List<String> crawl(@RequestParam String url) {
        return crawlerService.crawl(url);
    }
}
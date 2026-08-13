package com.example.crawler.controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/crawl")
public class CrawlerController {
    @PostMapping
    public String crawl(@RequestParam String url){
        return "Received URL : "+url;
    }
}

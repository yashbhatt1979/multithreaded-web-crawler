// package com.example.crawler.controller;
// import org.springframework.web.bind.annotation.PostMapping;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RequestParam;
// import org.springframework.web.bind.annotation.RestController;
// @RestController
// @RequestMapping("/crawl")
// public class CrawlerController {
//     @PostMapping
//     public String crawl(@RequestParam String url){
//         return "Received URL : "+url;
//     }
// }
package com.example.crawler.controller;

import com.example.crawler.service.CrawlerService;
import org.springframework.web.bind.annotation.*;

@RestController
public class CrawlerController {

    private final CrawlerService crawlerService;

    public CrawlerController(CrawlerService crawlerService) {
        this.crawlerService = crawlerService;
    }

    @PostMapping("/crawl")
    public String crawl(@RequestParam String url) {
        return crawlerService.startCrawling(url);
    }
}
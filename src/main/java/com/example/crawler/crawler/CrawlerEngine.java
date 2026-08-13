package com.example.crawler.crawler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

@Component
public class CrawlerEngine {

    public List<String> crawl(String url) throws IOException {

        System.out.println("Crawling: " + url);

        // Connect to the website and download its HTML
        Document document = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .timeout(10_000)
                .get();

        // Extract all <a href="..."> links
        Elements links = document.select("a[href]");

        List<String> discoveredUrls = new ArrayList<>();

        for (Element link : links) {

            String absoluteUrl = link.absUrl("href");

            if (!absoluteUrl.isEmpty()) {
                discoveredUrls.add(absoluteUrl);
            }
        }

        System.out.println("Found " + discoveredUrls.size() + " URLs");

        return discoveredUrls;
    }
}
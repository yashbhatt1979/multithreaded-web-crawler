package com.crawler.scraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

@Component
public class HtmlParser {

    public Document parse(String html) {

        if (html == null || html.isBlank()) {
            throw new IllegalArgumentException(
                    "HTML content cannot be null or empty"
            );
        }

        return Jsoup.parse(html);
    }
}
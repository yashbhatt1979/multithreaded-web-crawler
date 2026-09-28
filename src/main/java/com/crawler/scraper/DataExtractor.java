package com.crawler.scraper;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class DataExtractor {

    public Map<String, Object> extract(Document document) {

        Map<String, Object> data = new LinkedHashMap<>();

        // Page title
        data.put("title", document.title());

        // Main textual content
        data.put("text", document.body() != null
                ? document.body().text()
                : "");

        // Number of links
        data.put(
                "linkCount",
                document.select("a[href]").size()
        );

        // Number of images
        data.put(
                "imageCount",
                document.select("img").size()
        );

        // Meta description
        Element metaDescription =
                document.selectFirst(
                        "meta[name=description]"
                );

        data.put(
                "description",
                metaDescription != null
                        ? metaDescription.attr("content")
                        : ""
        );

        return data;
    }
}
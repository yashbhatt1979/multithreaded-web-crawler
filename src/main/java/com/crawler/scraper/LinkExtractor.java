package com.crawler.scraper;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

@Component
public class LinkExtractor {

    public Set<String> extract(Document document, String baseUrl) {

        Set<String> links = new HashSet<>();

        Elements elements = document.select("a[href]");

        for (Element element : elements) {

            String absoluteUrl =
                    element.absUrl("href");

            if (absoluteUrl.isBlank()) {
                continue;
            }

            try {

                URI uri = URI.create(absoluteUrl);

                String scheme = uri.getScheme();

                if ("http".equalsIgnoreCase(scheme)
                        || "https".equalsIgnoreCase(scheme)) {

                    links.add(absoluteUrl);
                }

            } catch (IllegalArgumentException ignored) {
                // Ignore malformed URLs
            }
        }

        return links;
    }
}
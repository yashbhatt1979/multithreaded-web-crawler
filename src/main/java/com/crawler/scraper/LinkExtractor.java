package com.crawler.scraper;

import com.crawler.service.UrlCanonicalizer;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class LinkExtractor {

    private final UrlCanonicalizer urlCanonicalizer;

    public LinkExtractor(UrlCanonicalizer urlCanonicalizer) {
        this.urlCanonicalizer = urlCanonicalizer;
    }

    public Set<String> extract(Document document, String baseUrl) {

        Set<String> links = new HashSet<>();

        Elements elements = document.select("a[href]");

        for (Element element : elements) {

            String absoluteUrl = element.absUrl("href");

            if (absoluteUrl.isBlank()) {
                continue;
            }

            String canonicalUrl =
                    urlCanonicalizer.canonicalize(absoluteUrl);

            if (canonicalUrl == null) {
                continue;
            }

            links.add(canonicalUrl);
        }

        return links;
    }
}
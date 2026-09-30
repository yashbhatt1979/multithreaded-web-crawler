package com.crawler.service;

import com.crawler.scraper.DataExtractor;
import com.crawler.scraper.HtmlParser;
import com.crawler.scraper.LinkExtractor;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
public class ScraperService {

    private final HtmlParser htmlParser;
    private final LinkExtractor linkExtractor;
    private final DataExtractor dataExtractor;

    public ScraperService(
            HtmlParser htmlParser,
            LinkExtractor linkExtractor,
            DataExtractor dataExtractor
    ) {
        this.htmlParser = htmlParser;
        this.linkExtractor = linkExtractor;
        this.dataExtractor = dataExtractor;
    }

    public ScrapingResult scrape(
            String html,
            String baseUrl
    ) {

        Document document = htmlParser.parse(html);

        Set<String> links =
                linkExtractor.extract(
                        document,
                        baseUrl
                );

        Map<String, Object> extractedData =
                dataExtractor.extract(document);

        return new ScrapingResult(
                document,
                links,
                extractedData
        );
    }

    public static class ScrapingResult {

        private final Document document;
        private final Set<String> links;
        private final Map<String, Object> extractedData;

        public ScrapingResult(
                Document document,
                Set<String> links,
                Map<String, Object> extractedData
        ) {
            this.document = document;
            this.links = links;
            this.extractedData = extractedData;
        }

        public Document getDocument() {
            return document;
        }

        public Set<String> getLinks() {
            return links;
        }

        public Map<String, Object> getExtractedData() {
            return extractedData;
        }
    }
}
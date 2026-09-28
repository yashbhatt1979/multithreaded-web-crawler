package com.crawler.model;

import java.util.List;
import java.util.Map;

public class CrawlResult {

    private String url;

    private int statusCode;

    private String title;

    private String content;

    private List<String> links;

    private Map<String, Object> extractedData;

    private int depth;

    public CrawlResult() {
    }

    public CrawlResult(
            String url,
            int statusCode,
            String title,
            String content,
            List<String> links,
            Map<String, Object> extractedData,
            int depth
    ) {
        this.url = url;
        this.statusCode = statusCode;
        this.title = title;
        this.content = content;
        this.links = links;
        this.extractedData = extractedData;
        this.depth = depth;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getLinks() {
        return links;
    }

    public void setLinks(List<String> links) {
        this.links = links;
    }

    public Map<String, Object> getExtractedData() {
        return extractedData;
    }

    public void setExtractedData(
            Map<String, Object> extractedData
    ) {
        this.extractedData = extractedData;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }
}
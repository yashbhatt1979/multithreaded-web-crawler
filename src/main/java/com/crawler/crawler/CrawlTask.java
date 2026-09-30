package com.crawler.crawler;

public class CrawlTask {

    private final String url;
    private final int depth;
    private final String messageId;

    public CrawlTask(
            String url,
            int depth
    ) {
        this(url, depth, null);
    }

    public CrawlTask(
            String url,
            int depth,
            String messageId
    ) {
        this.url = url;
        this.depth = depth;
        this.messageId = messageId;
    }

    public String getUrl() {
        return url;
    }

    public int getDepth() {
        return depth;
    }

    public String getMessageId() {
        return messageId;
    }

    public boolean isRecoveredTask() {
        return messageId != null;
    }

    @Override
    public String toString() {
        return "CrawlTask{" +
                "url='" + url + '\'' +
                ", depth=" + depth +
                ", messageId='" + messageId + '\'' +
                '}';
    }
}
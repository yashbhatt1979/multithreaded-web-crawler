package com.crawler.crawler;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.stereotype.Component;

@Component
public class CrawlQueue {

    private final BlockingQueue<CrawlTask> queue =
            new LinkedBlockingQueue<>();

    public void add(CrawlTask task) {
        queue.offer(task);
    }

    public CrawlTask poll() {
        return queue.poll();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public int size() {
        return queue.size();
    }
}
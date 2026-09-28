package com.crawler.concurrency;

import java.util.concurrent.ExecutorService;

import org.springframework.stereotype.Component;

@Component
public class CrawlTaskExecutor {

    private final ExecutorService executorService;

    public CrawlTaskExecutor(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public void submit(Runnable task) {
        executorService.submit(task);
    }

    public void shutdown() {
        executorService.shutdown();
    }
}
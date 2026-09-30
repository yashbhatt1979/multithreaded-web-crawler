
package com.crawler.concurrency;

import java.util.concurrent.ExecutorService;

import org.springframework.stereotype.Component;

@Component
public class CrawlTaskExecutor {

    private final ExecutorService executorService;

    public CrawlTaskExecutor(
            ExecutorService executorService
    ) {
        this.executorService = executorService;
    }

    // =========================================================
    // SUBMIT TASK
    // =========================================================

    public void submit(Runnable task) {

        if (task == null) {
            return;
        }

        if (executorService.isShutdown()) {

            throw new IllegalStateException(
                    "Crawler worker executor has been shut down."
            );
        }

        executorService.submit(task);
    }

    // =========================================================
    // STATUS
    // =========================================================

    public boolean isShutdown() {

        return executorService.isShutdown();
    }

    public boolean isTerminated() {

        return executorService.isTerminated();
    }

    // =========================================================
    // APPLICATION SHUTDOWN
    // =========================================================

    /*
     * This should only be used when the entire application
     * is shutting down.
     *
     * Normal crawler /stop should NOT call this.
     */
    public void shutdown() {

        if (!executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
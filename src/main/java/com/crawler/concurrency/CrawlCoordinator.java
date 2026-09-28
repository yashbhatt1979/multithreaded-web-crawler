package com.crawler.concurrency;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

import com.crawler.crawler.CrawlQueue;
import com.crawler.crawler.CrawlTask;
import com.crawler.crawler.Worker;
import com.crawler.repository.CrawledPageRepository;
import com.crawler.service.ScraperService;
import com.crawler.service.UrlFetcherService;

@Component
public class CrawlCoordinator {

    private final CrawlQueue crawlQueue;
    private final CrawlTaskExecutor taskExecutor;

    private final UrlFetcherService urlFetcherService;
    private final ScraperService scraperService;
    private final CrawledPageRepository crawledPageRepository;

    /*
     * URLs that have already been submitted.
     *
     * ConcurrentHashMap makes this set thread-safe.
     */
    private final Set<String> visitedUrls =
            ConcurrentHashMap.newKeySet();

    /*
     * Number of tasks that are either:
     *
     * 1. Waiting in the queue
     * 2. Currently being processed by a worker
     *
     * This is important because the queue can temporarily
     * become empty while workers are still crawling.
     */
    private final AtomicInteger activeTasks =
            new AtomicInteger(0);

    public CrawlCoordinator(
            CrawlQueue crawlQueue,
            CrawlTaskExecutor taskExecutor,
            UrlFetcherService urlFetcherService,
            ScraperService scraperService,
            CrawledPageRepository crawledPageRepository
    ) {
        this.crawlQueue = crawlQueue;
        this.taskExecutor = taskExecutor;
        this.urlFetcherService = urlFetcherService;
        this.scraperService = scraperService;
        this.crawledPageRepository = crawledPageRepository;
    }

    /*
     * Submit a URL for crawling.
     *
     * Only the first submission of a URL is accepted.
     */
    public void submitTask(CrawlTask task) {

        String url = task.getUrl();

        /*
         * add() is atomic.
         *
         * true  -> first time seeing this URL
         * false -> URL was already submitted
         */
        if (visitedUrls.add(url)) {

            /*
             * Increase active task count BEFORE putting
             * the task into the queue.
             */
            activeTasks.incrementAndGet();

            crawlQueue.add(task);

            System.out.println(
                    "Queued URL: " + url
            );
        }
    }

    /*
     * Start processing tasks.
     *
     * Important:
     * We cannot simply check queue.isEmpty()
     * because workers may still be processing URLs
     * and may discover new URLs later.
     */
    public void start() {

        while (activeTasks.get() > 0) {

            CrawlTask task = crawlQueue.poll();

            /*
             * Queue is temporarily empty.
             *
             * This means existing workers are probably
             * still crawling pages.
             */
            if (task == null) {

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {

                    Thread.currentThread().interrupt();

                    break;
                }

                continue;
            }

            Worker worker = new Worker(
                    task,
                    crawlQueue,
                    this,
                    urlFetcherService,
                    scraperService,
                    crawledPageRepository
            );

            try {

                taskExecutor.submit(() -> {

                    try {

                        worker.run();

                    } finally {

                        /*
                         * Worker has completely finished.
                         *
                         * This happens AFTER the worker has
                         * discovered and submitted new URLs.
                         */
                        activeTasks.decrementAndGet();
                    }
                });

            } catch (RejectedExecutionException e) {

                /*
                 * If the executor rejects the task,
                 * compensate for the increment done
                 * inside submitTask().
                 */
                activeTasks.decrementAndGet();

                System.err.println(
                        "Task rejected for URL: "
                                + task.getUrl()
                );
            }
        }

        System.out.println(
                "Crawling completed. Total URLs submitted: "
                        + visitedUrls.size()
        );
    }

    /*
     * Stop the crawler.
     */
    public void shutdown() {

        taskExecutor.shutdown();
    }
}
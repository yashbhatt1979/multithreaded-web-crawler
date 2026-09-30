package com.crawler.crawler;

import java.time.LocalDateTime;
import java.util.Set;

import org.jsoup.nodes.Document;

import com.crawler.concurrency.CrawlCoordinator;
import com.crawler.model.CrawledPage;
import com.crawler.model.UrlStatus;
import com.crawler.repository.CrawledPageRepository;
import com.crawler.service.ScraperService;
import com.crawler.service.UrlFetcherService;

public class Worker implements Runnable {

    private final CrawlTask task;
    private final CrawlCoordinator crawlCoordinator;
    private final UrlFetcherService urlFetcherService;
    private final ScraperService scraperService;
    private final CrawledPageRepository crawledPageRepository;

    public Worker(
            CrawlTask task,
            CrawlCoordinator crawlCoordinator,
            UrlFetcherService urlFetcherService,
            ScraperService scraperService,
            CrawledPageRepository crawledPageRepository
    ) {
        this.task = task;
        this.crawlCoordinator = crawlCoordinator;
        this.urlFetcherService = urlFetcherService;
        this.scraperService = scraperService;
        this.crawledPageRepository = crawledPageRepository;
    }

    @Override
    public void run() {

        String url = task.getUrl();
        int depth = task.getDepth();

        String threadName =
                Thread.currentThread().getName();

        /*
         * If the crawler has already been stopped before
         * this worker starts, do not process the task.
         *
         * The Redis message is intentionally NOT ACKed.
         * It can remain pending for recovery.
         */
        if (!crawlCoordinator.isRunning()) {

            System.out.println(
                    threadName
                            + " skipping task because crawler is stopped: "
                            + url
            );

            return;
        }

        System.out.println(
                threadName
                        + " visiting: "
                        + url
                        + " | depth="
                        + depth
        );

        try {

            // =================================================
            // FETCH
            // =================================================

            UrlFetcherService.FetchResult fetchResult =
                    urlFetcherService.fetch(url);

            String html =
                    fetchResult.getHtml();

            int statusCode =
                    fetchResult.getStatusCode();

            // =================================================
            // SCRAPE
            // =================================================

            ScraperService.ScrapingResult result =
                    scraperService.scrape(
                            html,
                            url
                    );

            Document document =
                    result.getDocument();

            // =================================================
            // SAVE PAGE
            // =================================================

            CrawledPage page =
                    new CrawledPage();

            page.setUrl(url);

            page.setStatusCode(statusCode);

            page.setTitle(
                    document.title()
            );

            page.setDescription(
                    document
                            .select("meta[name=description]")
                            .attr("content")
            );

            page.setContent(
                    document.text()
            );

            page.setDepth(depth);

            page.setStatus(
                    UrlStatus.CRAWLED
            );

            page.setCrawledAt(
                    LocalDateTime.now()
            );

            crawledPageRepository.save(page);

            // =================================================
            // DISCOVER LINKS
            // =================================================

            Set<String> links =
                    result.getLinks();

            System.out.println(
                    threadName
                            + " discovered "
                            + links.size()
                            + " links from "
                            + url
            );

            // =================================================
            // SUBMIT DISCOVERED LINKS
            // =================================================

            /*
             * Check the crawler state before generating
             * additional work.
             *
             * If /stop was called while this page was being
             * processed, the current page can finish but
             * newly discovered URLs will NOT be submitted.
             */
            if (crawlCoordinator.isRunning()) {

                for (String discoveredUrl : links) {

                    /*
                     * Check again for every URL.
                     *
                     * This prevents a large batch of links
                     * from continuing to enter Redis after
                     * /stop is called.
                     */
                    if (!crawlCoordinator.isRunning()) {

                        System.out.println(
                                threadName
                                        + " stopping link submission because "
                                        + "crawler was stopped."
                        );

                        break;
                    }

                    if (discoveredUrl == null ||
                            discoveredUrl.isBlank()) {

                        continue;
                    }

                    CrawlTask newTask =
                            new CrawlTask(
                                    discoveredUrl,
                                    depth + 1
                            );

                    crawlCoordinator.submitTask(
                            newTask
                    );
                }

            } else {

                System.out.println(
                        threadName
                                + " crawler stopped. "
                                + "No new links submitted from: "
                                + url
                );
            }

            // =================================================
            // ACK REDIS MESSAGE
            // =================================================

            /*
             * The current page was successfully:
             *
             * 1. fetched
             * 2. scraped
             * 3. saved
             *
             * Therefore the original Redis task can be ACKed.
             *
             * If the crawler was stopped after the page was
             * successfully processed, we still ACK this
             * completed task.
             */
            crawlCoordinator.acknowledgeTask(
                    task
            );

            System.out.println(
                    threadName
                            + " completed: "
                            + url
            );

        } catch (Exception e) {

            /*
             * DO NOT ACK failed tasks.
             *
             * The Redis Stream message remains in the
             * Pending Entries List and can be recovered
             * after the configured idle period.
             */
            System.err.println(
                    threadName
                            + " failed to crawl URL: "
                            + url
            );

            System.err.println(
                    "Reason: "
                            + e.getMessage()
            );
        }
    }
}
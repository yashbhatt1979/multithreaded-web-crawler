package com.crawler.crawler;

import com.crawler.concurrency.CrawlCoordinator;
import com.crawler.model.CrawledPage;
import com.crawler.model.UrlStatus;
import com.crawler.repository.CrawledPageRepository;
import com.crawler.service.ScraperService;
import com.crawler.service.UrlFetcherService;

public class Worker implements Runnable {

    private final CrawlTask task;
    private final CrawlQueue crawlQueue;
    private final CrawlCoordinator crawlCoordinator;

    private final UrlFetcherService urlFetcherService;
    private final ScraperService scraperService;
    private final CrawledPageRepository crawledPageRepository;

    public Worker(
            CrawlTask task,
            CrawlQueue crawlQueue,
            CrawlCoordinator crawlCoordinator,
            UrlFetcherService urlFetcherService,
            ScraperService scraperService,
            CrawledPageRepository crawledPageRepository
    ) {
        this.task = task;
        this.crawlQueue = crawlQueue;
        this.crawlCoordinator = crawlCoordinator;
        this.urlFetcherService = urlFetcherService;
        this.scraperService = scraperService;
        this.crawledPageRepository = crawledPageRepository;
    }

    @Override
    public void run() {

        String url = task.getUrl();

        try {

            // 1. Print URL being visited
            System.out.println(
                    Thread.currentThread().getName()
                            + " visiting: " + url
            );

            // 2. Fetch webpage
            UrlFetcherService.FetchResult fetchResult =
                    urlFetcherService.fetch(url);

            String html = fetchResult.getHtml();

            int statusCode = fetchResult.getStatusCode();

            // 3. Scrape webpage
            ScraperService.ScrapingResult crawlResult =
                    scraperService.scrape(html, url);

            // 4. Extract page information
            String title =
                    crawlResult.getDocument().title();

            String description =
                    crawlResult.getDocument()
                            .select("meta[name=description]")
                            .attr("content");

            String content =
                    crawlResult.getDocument().html();

            // 5. Create CrawledPage
            CrawledPage crawledPage =
                    new CrawledPage(
                            url,
                            statusCode,
                            title,
                            content,
                            0,
                            UrlStatus.CRAWLED
                    );

            crawledPage.setDescription(description);

            // 6. Save page to database
            crawledPageRepository.save(crawledPage);

            System.out.println(
                    Thread.currentThread().getName()
                            + " saved: " + url
            );

            // 7. Submit discovered URLs
            for (String discoveredUrl : crawlResult.getLinks()) {

                crawlCoordinator.submitTask(
                        new CrawlTask(discoveredUrl)
                );
            }

        } catch (Exception e) {

            System.err.println(
                    Thread.currentThread().getName()
                            + " failed to crawl URL: " + url
            );

            System.err.println(
                    "Reason: " + e.getMessage()
            );
        }
    }
}
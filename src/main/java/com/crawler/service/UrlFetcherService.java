package com.crawler.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.springframework.stereotype.Service;

import com.crawler.config.CrawlerConfig;
import com.crawler.exception.FetchFailedException;

@Service
public class UrlFetcherService {

    private final HttpClient httpClient;
    private final CrawlerConfig crawlerConfig;

    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_SECONDS = 2;

    public UrlFetcherService(CrawlerConfig crawlerConfig) {

        this.crawlerConfig = crawlerConfig;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(
                        crawlerConfig.getRequestTimeoutSeconds()
                ))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public FetchResult fetch(String url) {

        int attempt = 0;

        while (attempt <= MAX_RETRIES) {

            try {

                attempt++;

                System.out.println(
                        Thread.currentThread().getName()
                                + " fetching: "
                                + url
                                + " (attempt "
                                + attempt
                                + "/"
                                + (MAX_RETRIES + 1)
                                + ")"
                );

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(
                                crawlerConfig.getRequestTimeoutSeconds()
                        ))
                        .header(
                                "User-Agent",
                                crawlerConfig.getUserAgent()
                        )
                        .header(
                                "Accept",
                                "text/html,application/xhtml+xml,"
                                        + "application/xml;q=0.9,"
                                        + "*/*;q=0.8"
                        )
                        .header(
                                "Accept-Language",
                                "en-US,en;q=0.9"
                        )
                        .GET()
                        .build();

                HttpResponse<String> response =
                        httpClient.send(
                                request,
                                HttpResponse.BodyHandlers.ofString()
                        );

                int statusCode = response.statusCode();

                /*
                 * SUCCESS
                 */
                if (statusCode >= 200 && statusCode < 300) {

                    System.out.println(
                            Thread.currentThread().getName()
                                    + " successfully fetched: "
                                    + url
                                    + " ["
                                    + statusCode
                                    + "]"
                    );

                    return new FetchResult(
                            response.body(),
                            statusCode
                    );
                }

                /*
                 * RATE LIMITED - HTTP 429
                 */
                if (statusCode == 429) {

                    if (attempt > MAX_RETRIES) {

                        throw new FetchFailedException(
                                "Failed to fetch URL after "
                                        + MAX_RETRIES
                                        + " retries. HTTP status: 429"
                        );
                    }

                    long retryDelay =
                            calculateRetryDelay(
                                    response,
                                    attempt
                            );

                    System.out.println(
                            Thread.currentThread().getName()
                                    + " received HTTP 429 for: "
                                    + url
                    );

                    System.out.println(
                            "Waiting "
                                    + retryDelay
                                    + " seconds before retry..."
                    );

                    sleep(retryDelay);

                    continue;
                }

                /*
                 * SERVER ERROR - 5xx
                 */
                if (statusCode >= 500 && statusCode <= 599) {

                    if (attempt > MAX_RETRIES) {

                        throw new FetchFailedException(
                                "Failed to fetch URL after "
                                        + MAX_RETRIES
                                        + " retries. HTTP status: "
                                        + statusCode
                        );
                    }

                    long retryDelay =
                            calculateExponentialBackoff(attempt);

                    System.out.println(
                            Thread.currentThread().getName()
                                    + " received HTTP "
                                    + statusCode
                                    + " for: "
                                    + url
                    );

                    System.out.println(
                            "Waiting "
                                    + retryDelay
                                    + " seconds before retry..."
                    );

                    sleep(retryDelay);

                    continue;
                }

                /*
                 * Other HTTP errors
                 */
                throw new FetchFailedException(
                        "Failed to fetch URL. HTTP status: "
                                + statusCode
                );

            } catch (FetchFailedException e) {

                throw e;

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

                throw new FetchFailedException(
                        "Crawler thread interrupted while fetching: "
                                + url
                );

            } catch (Exception e) {

                if (attempt > MAX_RETRIES) {

                    throw new FetchFailedException(
                            "Failed to fetch URL: " + url
                    );
                }

                long retryDelay =
                        calculateExponentialBackoff(attempt);

                System.out.println(
                        Thread.currentThread().getName()
                                + " network error while fetching: "
                                + url
                );

                System.out.println(
                        "Waiting "
                                + retryDelay
                                + " seconds before retry..."
                );

                try {

                    sleep(retryDelay);

                } catch (InterruptedException interruptedException) {

                    Thread.currentThread().interrupt();

                    throw new FetchFailedException(
                            "Crawler thread interrupted while "
                                    + "waiting to retry: "
                                    + url
                    );
                }
            }
        }

        throw new FetchFailedException(
                "Failed to fetch URL: " + url
        );
    }

    private long calculateRetryDelay(
            HttpResponse<String> response,
            int attempt
    ) {

        String retryAfter =
                response.headers()
                        .firstValue("Retry-After")
                        .orElse(null);

        if (retryAfter != null) {

            try {

                long seconds =
                        Long.parseLong(retryAfter);

                if (seconds > 0) {
                    return seconds;
                }

            } catch (NumberFormatException ignored) {
                // Fall back to exponential backoff
            }
        }

        return calculateExponentialBackoff(attempt);
    }

    private long calculateExponentialBackoff(int attempt) {

        return INITIAL_BACKOFF_SECONDS
                * (1L << Math.max(0, attempt - 1));
    }

    private void sleep(long seconds)
            throws InterruptedException {

        Thread.sleep(seconds * 1000L);
    }

    public static class FetchResult {

        private final String html;
        private final int statusCode;

        public FetchResult(
                String html,
                int statusCode
        ) {
            this.html = html;
            this.statusCode = statusCode;
        }

        public String getHtml() {
            return html;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }
}
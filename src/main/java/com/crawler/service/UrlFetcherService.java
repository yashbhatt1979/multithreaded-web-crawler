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

    // Maximum number of attempts for one URL
    private static final int MAX_RETRIES = 3;

    // Initial delay before retrying
    private static final long INITIAL_BACKOFF_SECONDS = 2;

    public UrlFetcherService(CrawlerConfig crawlerConfig) {

        this.crawlerConfig = crawlerConfig;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
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

                HttpRequest request =
                        HttpRequest.newBuilder()
                                .uri(URI.create(url))
                                .timeout(Duration.ofSeconds(15))

                                .header(
                                        "User-Agent",
                                        "MultithreadedWebCrawler/1.0 "
                                                + "(educational project)"
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
                 *
                 * These errors can also be temporary,
                 * so retry them with backoff.
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
                 * Other HTTP errors such as:
                 *
                 * 400 Bad Request
                 * 401 Unauthorized
                 * 403 Forbidden
                 * 404 Not Found
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

                /*
                 * Network failures can sometimes be temporary.
                 * Retry them using exponential backoff.
                 */

                if (attempt > MAX_RETRIES) {

                    throw new FetchFailedException(
                            "Failed to fetch URL: "
                                    + url
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

    /*
     * Calculate retry delay.
     *
     * First check the server's Retry-After header.
     *
     * If it exists:
     *
     * Retry-After: 5
     *
     * then we wait 5 seconds.
     *
     * If it doesn't exist, use exponential backoff.
     */
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

    /*
     * Exponential backoff:
     *
     * attempt 1 -> 2 seconds
     * attempt 2 -> 4 seconds
     * attempt 3 -> 8 seconds
     */
    private long calculateExponentialBackoff(
            int attempt
    ) {

        return INITIAL_BACKOFF_SECONDS
                * (1L << Math.max(0, attempt - 1));
    }

    /*
     * Pause the current worker thread.
     */
    private void sleep(long seconds)
            throws InterruptedException {

        Thread.sleep(seconds * 1000L);
    }

    /*
     * Result returned by fetch().
     */
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
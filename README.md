Multithreaded Distributed Web Crawler & Scraper

A production-oriented multithreaded and horizontally scalable web
crawler and scraper built with Java and Spring Boot.

The project is designed to demonstrate practical concepts in
concurrency, distributed systems, Redis coordination, web scraping,
URL deduplication, content hashing, URL canonicalization, Docker-based
horizontal scaling, and persistent storage with MySQL.

🚀 Project Overview

The crawler accepts a seed URL, fetches the page, extracts useful
information and links, stores the crawled page in MySQL, and distributes
newly discovered URLs across multiple crawler instances.

The system combines:

Multithreaded crawling

Distributed URL coordination

Redis-backed task distribution

Horizontal scaling

URL canonicalization

Distributed URL deduplication

Content hashing

HTML parsing with JSoup

MySQL persistence

Flyway database migrations

Docker and Docker Compose

Redis Streams / consumer groups

Graceful start and stop controls

Crawl depth tracking

HTTP status tracking

🏗️ High-Level Architecture

                         Client / Postman
                                |
                                | POST /api/crawler/start
                                v
                    +-------------------------+
                    |    Crawler Service      |
                    +-------------------------+
                                |
                                | START command
                                v
                    +-------------------------+
                    |        Redis            |
                    |  Control + Coordination |
                    +-------------------------+
                                |
              +-----------------+-----------------+
              |                 |                 |
              v                 v                 v
        +-----------+     +-----------+     +-----------+
        | crawler-1 |     | crawler-2 |     | crawler-3 |
        +-----------+     +-----------+     +-----------+
              |                 |                 |
              +-----------------+-----------------+
                                |
                        Redis Task Distribution
                                |
                                v
                       +------------------+
                       |   Worker Pool    |
                       +------------------+
                                |
                                v
                       +------------------+
                       | URL Fetcher      |
                       | Java HttpClient  |
                       +------------------+
                                |
                                v
                       +------------------+
                       | JSoup Scraper    |
                       +------------------+
                                |
                 +--------------+--------------+
                 |                             |
                 v                             v
          Canonicalized URLs             Extracted Data
                 |                             |
                 v                             v
             Redis SET                    MySQL
          Deduplication                crawled_pages

✨ Key Features

1. Multithreaded Crawling

Each crawler instance uses a local worker pool to process multiple URLs
concurrently.

This allows multiple pages to be fetched and processed at the same time
instead of crawling sequentially.

Crawler Instance
      |
      +-- Worker 1
      +-- Worker 2
      +-- Worker 3
      +-- Worker 4
      +-- Worker 5
      ...

Thread-safe data structures and concurrent processing are used to
coordinate local crawling.

2. Horizontal Scaling

The crawler is designed to run multiple application instances
simultaneously.

Example:

                    Redis
                      |
        +-------------+-------------+
        |             |             |
        v             v             v
    crawler-1     crawler-2     crawler-3
        |             |             |
     workers       workers       workers

Additional crawler containers can be started without changing the
crawler's core logic.

The instances coordinate through Redis rather than maintaining
independent crawling state.

3. Distributed URL Deduplication

A Redis Set is used as the distributed visited-URL registry:

crawler:visited

When a URL is submitted:

URL
 |
 v
Redis SADD
 |
 +-- 1 → URL was new
 |
 +-- 0 → URL already exists

This prevents multiple crawler instances from unnecessarily processing
the same canonical URL.

4. URL Canonicalization

The project now canonicalizes URLs before distributed deduplication.

Examples:

HTTPS://Example.COM/page
        ↓
https://example.com/page

https://example.com/page#section
        ↓
https://example.com/page

https://example.com/page?utm_source=google
        ↓
https://example.com/page

Default ports are also normalized:

https://example.com:443/page
        ↓
https://example.com/page

Meaningful query parameters are preserved:

https://example.com/product?id=123

remains:

https://example.com/product?id=123

The crawler uses a dedicated UrlCanonicalizer service based on Java's
URI API.

Canonicalization flow

Discovered Link
      |
      v
UrlCanonicalizer
      |
      v
Canonical URL
      |
      v
Redis Deduplication
      |
      v
Crawl Queue

5. Content Hashing

The crawler stores a hash representing the crawled page content.

This provides a compact way to identify identical content and can be
used for future duplicate-content detection and change detection.

Conceptually:

HTML Content
     |
     v
Content Hash
     |
     v
MySQL

The database model contains a contentHash field associated with the
crawled page.

6. Redis-Based Distributed Task Processing

Redis is used as the coordination layer between crawler instances.

The architecture uses Redis Streams and consumer groups to distribute
crawling work.

                  Redis Stream
                       |
        +--------------+--------------+
        |              |              |
        v              v              v
     crawler-1      crawler-2      crawler-3
     consumer       consumer       consumer

This allows work to be distributed across multiple application
instances.

Redis also supports:

Distributed visited URL tracking

Control commands

Task distribution

Consumer recovery / pending task handling

7. Redis Consumer Threads

Each crawler instance runs multiple Redis consumer threads.

The current configuration uses:

3 Redis consumer threads / instance

With three crawler instances:

crawler-1 → 3 consumers
crawler-2 → 3 consumers
crawler-3 → 3 consumers

Total → 9 Redis consumers

8. HTML Scraping

JSoup is used for HTML parsing.

The scraper extracts:

Page title

Meta description

Page content

Links

HTTP status

Crawl depth

The general flow is:

HTML
 |
 v
JSoup Document
 |
 +----> Title
 |
 +----> Description
 |
 +----> Content
 |
 +----> Links

9. Crawl Depth Tracking

Each CrawlTask contains a depth value.

Example:

Seed URL
Depth 0
   |
   +-- Link A
   |    Depth 1
   |
   +-- Link B
        Depth 1
        |
        +-- Link C
             Depth 2

This allows the crawler to control how far it follows links from the
original seed.

10. Persistent Storage

Crawled page information is stored in MySQL.

The crawled_pages table stores information such as:

URL

Title

Description

Content

HTTP status

Crawl depth

Content hash

Crawl status

Crawl timestamp

A unique constraint on the URL provides an additional database-level
protection against duplicate records.

11. Flyway Database Migrations

Flyway is used to manage database schema changes.

This allows the database structure to evolve through versioned migration
files rather than manually modifying the database.

Example:

src/main/resources/db/migration/

V1__create_crawled_pages.sql
V2__...
V3__...

12. Dockerized Deployment

The project is designed to run using Docker Compose.

Typical services include:

crawler-1
crawler-2
crawler-3
crawler-mysql
crawler-redis

This makes it possible to reproduce the distributed architecture
locally.

🧰 Technology Stack

Technology        Purpose

Java 17           Core programming language
Spring Boot       Application framework
Maven             Build and dependency management
JSoup             HTML parsing and link extraction
Java HttpClient   HTTP requests
MySQL 8           Persistent storage
Redis             Distributed coordination and task distribution
Redis Streams     Distributed crawl task processing
Flyway            Database migrations
Docker            Containerization
Docker Compose    Multi-container deployment
Postman           API testing

📁 Project Structure

src/
└── main/
    ├── java/
    │   └── com/
    │       └── crawler/
    │           ├── config/
    │           │   ├── CrawlerControlListener.java
    │           │   ├── CrawlerControlPublisher.java
    │           │   └── ...
    │           │
    │           ├── controller/
    │           │
    │           ├── crawler/
    │           │   ├── CrawlQueue.java
    │           │   ├── CrawlTask.java
    │           │   ├── CrawlerEngine.java
    │           │   └── Worker.java
    │           │
    │           ├── concurrency/
    │           │   ├── CrawlCoordinator.java
    │           │   ├── CrawlTaskExecutor.java
    │           │   └── ThreadPoolConfig.java
    │           │
    │           ├── model/
    │           │   ├── CrawledPage.java
    │           │   └── UrlStatus.java
    │           │
    │           ├── repository/
    │           │   └── CrawledPageRepository.java
    │           │
    │           └── service/
    │               ├── CrawlerService.java
    │               ├── ScraperService.java
    │               ├── UrlCanonicalizer.java
    │               └── UrlFetcherService.java
    │
    └── resources/
        ├── application.properties
        └── db/
            └── migration/
                └── V1__create_crawled_pages.sql

🔄 Crawling Workflow

The complete crawling process is:

1. Client sends seed URL
             |
             v
2. CrawlerService receives request
             |
             v
3. Seed URL is canonicalized
             |
             v
4. START command is published through Redis
             |
             v
5. Crawler instances receive START
             |
             v
6. CrawlTask is created
             |
             v
7. CrawlCoordinator canonicalizes URL
             |
             v
8. Redis visited-set deduplication
             |
             v
9. URL enters distributed crawl queue
             |
             v
10. Worker fetches URL
             |
             v
11. HTML is parsed with JSoup
             |
             +------------------+
             |                  |
             v                  v
       Page saved          Links extracted
       to MySQL                  |
                                 v
                         URL canonicalization
                                 |
                                 v
                         Redis deduplication
                                 |
                                 v
                         New CrawlTask
                                 |
                                 v
                            Continue crawl

🔌 API

The crawler exposes control endpoints under:

/api/crawler

Start Crawling

POST /api/crawler/start?url=https://example.com

Example:

curl -X POST "http://localhost:8080/api/crawler/start?url=https://example.com"

Stop Crawling

POST /api/crawler/stop

Status

Use the project's status endpoint to inspect the current crawler state.

🐳 Running with Docker

Build the project:

mvn clean package -DskipTests

Build and start the Docker environment:

docker compose up -d --build

Check running containers:

docker compose ps

View crawler logs:

docker compose logs -f crawler-1

For all crawler instances:

docker compose logs -f crawler-1 crawler-2 crawler-3

Stop the environment:

docker compose down

🔍 Useful Redis Commands

Check the distributed visited URL set:

redis-cli SMEMBERS crawler:visited

Count visited URLs:

redis-cli SCARD crawler:visited

Clear the visited set for a fresh crawl:

redis-cli DEL crawler:visited

When Redis is running in Docker, use the Redis container name with
docker exec.

🗄️ Database

Example MySQL query to inspect crawled pages:

SELECT
    id,
    url,
    status,
    http_status,
    depth,
    crawled_at
FROM crawled_pages
ORDER BY crawled_at DESC;

Count crawled pages:

SELECT COUNT(*)
FROM crawled_pages;

Inspect content hashes:

SELECT
    url,
    content_hash
FROM crawled_pages;

🧪 Testing the Distributed Crawler

A useful test is to run multiple crawler instances:

crawler-1
crawler-2
crawler-3

Then start a crawl and inspect the logs:

crawler-1 → visiting URL
crawler-2 → visiting URL
crawler-3 → visiting URL

The purpose is to verify that:

All instances receive the control command.

Redis distributes crawling work.

Multiple workers process URLs concurrently.

Canonical URLs are used for deduplication.

Duplicate URLs are rejected by Redis.

Crawled records are persisted in MySQL.

Content hashes are stored with crawled pages.

🧠 System Design Concepts Demonstrated

This project is intended to demonstrate practical system-design concepts
rather than only basic web scraping.

Concurrency

Thread pools

Worker threads

Thread-safe collections

Concurrent task processing

Distributed Systems

Multiple crawler instances

Redis-based coordination

Distributed deduplication

Consumer groups

Message acknowledgement

Pending task recovery

Scalability

Horizontal scaling

Stateless crawler instances

Shared Redis coordination

Shared MySQL persistence

Data Consistency

Redis deduplication

Canonical URLs

Database uniqueness constraints

Content hashing

Reliability

Redis pending-task handling

Worker failure handling

Graceful crawler stop

Database persistence

🎯 Project Goals

The project was built to provide hands-on experience with:

Java concurrency

Spring Boot

Distributed task processing

Redis

MySQL

Docker

Web scraping

URL canonicalization

Content hashing

Horizontal scaling

Database design

Distributed system architecture

🔮 Possible Future Improvements

Potential future improvements include:

Retry and exponential backoff for HTTP failures

Per-domain rate limiting

Robots.txt support

Better HTTP status handling

Crawl prioritization

URL frontier prioritization

Metrics with Prometheus

Grafana dashboards

Distributed tracing

More advanced content-change detection

Database sharding

Improved failure recovery

Dynamic crawler-instance scaling

Load balancing with Nginx

📌 Project Summary

This project goes beyond a basic single-threaded crawler.

It combines:

                 Web Crawler
                     |
       +-------------+-------------+
       |             |             |
       v             v             v
  Multithreading  Redis        MySQL
       |             |             |
       v             v             v
   Workers       Distributed    Persistence
                 Coordination
       |             |
       +------+------+
              |
              v
      Horizontal Scaling
              |
              v
     URL Canonicalization
              |
              v
    Distributed Deduplication
              |
              v
       Content Hashing

The result is a Dockerized, distributed crawler architecture designed to
demonstrate real-world backend and system-design concepts.
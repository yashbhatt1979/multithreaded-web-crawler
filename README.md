# 🕷️ Multithreaded Web Crawler & Scraper

A **multithreaded web crawler and scraper** built using **Java 17 and Spring Boot**.

The system starts from a seed URL, fetches web pages, extracts links, manages discovered URLs through a thread-safe queue, prevents duplicate crawling using a visited set, and processes multiple URLs concurrently using a configurable thread pool.

The project also includes **HTML scraping, retry and backoff handling, concurrency control, persistence, and failure handling**.

---

## 🚀 Features

* 🌐 Crawl websites starting from a seed URL
* 🔗 Extract links from HTML pages
* 🕷️ Recursively discover and crawl new URLs
* ⚡ Multithreaded URL processing
* 🧵 Configurable thread pool
* 📋 Thread-safe crawl queue
* ✅ Visited URL tracking
* 🔒 Concurrency-safe URL processing
* 🔄 Retry mechanism for temporary failures
* ⏳ Backoff when servers return `429 Too Many Requests`
* 🧹 HTML parsing and text extraction using JSoup
* 💾 Store crawled pages in MySQL
* 📊 Track crawl status
* ❌ Handle failed URLs gracefully
* 🛑 Prevent duplicate crawling
* 🧩 Modular Spring Boot architecture
* 📈 Designed with scalability and concurrency in mind

---

# 🏗️ Architecture

```text
                         ┌─────────────────────┐
                         │      Seed URL       │
                         └──────────┬──────────┘
                                    │
                                    ▼
                         ┌─────────────────────┐
                         │  Crawl Coordinator  │
                         └──────────┬──────────┘
                                    │
                                    ▼
                         ┌─────────────────────┐
                         │    Crawl Queue      │
                         │  Thread-Safe Queue  │
                         └──────────┬──────────┘
                                    │
                    ┌───────────────┼───────────────┐
                    │               │               │
                    ▼               ▼               ▼
              ┌──────────┐    ┌──────────┐    ┌──────────┐
              │ Worker 1 │    │ Worker 2 │    │ Worker N │
              └─────┬────┘    └─────┬────┘    └─────┬────┘
                    │               │               │
                    └───────────────┼───────────────┘
                                    │
                                    ▼
                         ┌─────────────────────┐
                         │    UrlFetcher       │
                         │   HTTP Request      │
                         └──────────┬──────────┘
                                    │
                                    ▼
                         ┌─────────────────────┐
                         │      JSoup          │
                         │   HTML Parsing      │
                         └──────────┬──────────┘
                                    │
                       ┌────────────┴────────────┐
                       │                         │
                       ▼                         ▼
              ┌─────────────────┐       ┌─────────────────┐
              │ Extract Content  │       │ Extract Links   │
              └────────┬────────┘       └────────┬────────┘
                       │                         │
                       ▼                         ▼
              ┌─────────────────┐       ┌─────────────────┐
              │     MySQL       │       │  Crawl Queue    │
              │  Crawled Pages  │       │ New URLs        │
              └─────────────────┘       └─────────────────┘
```

---

# 🔄 Crawling Flow

The crawler follows this general workflow:

```text
Seed URL
   ↓
Add URL to Crawl Queue
   ↓
Worker takes URL
   ↓
Check Visited Set
   ↓
Fetch HTML
   ↓
HTTP Response
   ↓
 ┌───────────────┐
 │               │
 ▼               ▼
Success         Failure
 │               │
 ▼               ▼
Parse HTML     Retry
 │               │
 ├─────────┐     ▼
 │         │   Backoff
 ▼         ▼     │
Text     Links   ▼
 │         │   Retry
 │         │
 ▼         ▼
Save DB   Add new URLs
             │
             ▼
        Crawl Queue
```

---

# 🧵 Multithreading

The crawler uses multiple worker threads to process URLs concurrently.

Instead of:

```text
URL 1 → URL 2 → URL 3 → URL 4
```

the crawler can process:

```text
             ┌── URL 1 ── Worker 1
             │
Queue ───────┼── URL 2 ── Worker 2
             │
             ├── URL 3 ── Worker 3
             │
             └── URL 4 ── Worker 4
```

This allows multiple pages to be fetched and processed simultaneously.

The number of worker threads can be configured through the application configuration.

---

# 🔒 Concurrency Handling

Since multiple worker threads access shared resources, concurrency must be handled carefully.

The project handles shared state such as:

* Crawl Queue
* Visited URLs
* Crawl status
* Database operations
* Task submission

The goal is to prevent:

* Duplicate crawling
* Race conditions
* Inconsistent state
* Unsafe access to shared collections
* Multiple workers processing the same URL simultaneously

A URL should effectively transition through:

```text
DISCOVERED
     ↓
QUEUED
     ↓
CRAWLING
     ↓
CRAWLED
```

or:

```text
CRAWLING
    ↓
FAILED
```

---

# 🌐 URL Fetching

The `UrlFetcherService` is responsible for fetching web pages.

The basic flow is:

```text
URL
 ↓
HTTP Request
 ↓
HTTP Response
 ↓
HTML Content
```

The crawler handles common HTTP failures such as:

```text
429 Too Many Requests
403 Forbidden
4xx Client Errors
5xx Server Errors
Connection failures
Timeouts
```

Temporary failures can trigger retry and backoff logic.

---

# 🔄 Retry & Backoff

When a server temporarily rejects requests, such as:

```text
HTTP 429 Too Many Requests
```

the crawler does not immediately continue sending requests.

Instead:

```text
Request
   ↓
429
   ↓
Wait / Backoff
   ↓
Retry
   ↓
Success
   ↓
Parse HTML
```

This reduces unnecessary load on the target server and improves crawler reliability.

---

# 🧹 Web Scraping

The crawler uses **JSoup** to parse HTML.

The fetched HTML can be processed to extract:

### Page Content

```text
Title
Text
Metadata
```

### Links

```html
<a href="https://example.com/page">
```

The crawler extracts these links and converts them into URLs that can be added to the crawl queue.

---

# 🔗 Link Discovery

Suppose the seed URL is:

```text
https://example.com
```

and the page contains:

```text
/page1
/page2
/products
/about
```

The crawler discovers:

```text
https://example.com/page1
https://example.com/page2
https://example.com/products
https://example.com/about
```

These URLs are then added to the crawl queue.

The process continues recursively:

```text
Seed URL
   ↓
Discover Links
   ↓
Queue Links
   ↓
Workers Crawl Links
   ↓
Discover More Links
   ↓
Queue New Links
   ↓
Continue
```

---

# ✅ Visited URL Tracking

The crawler maintains a **visited set** to prevent processing the same URL multiple times.

For example:

```text
URL A
 ↓
URL B
 ↓
URL C
 ↓
URL A
```

Without a visited set, the crawler could repeatedly crawl:

```text
A → B → C → A → B → C → ...
```

With visited tracking:

```text
A → B → C
        ↓
      A already visited
        ↓
       Skip
```

This prevents duplicate crawling and unnecessary network requests.

---

# 💾 Database

The crawler uses **MySQL** to persist information about crawled pages.

Example information stored:

```text
URL
Page Title
Page Content
HTTP Status
Crawl Status
Crawled At
```

The database allows crawl results to survive application restarts and provides a persistent record of processed pages.

---

# 🗄️ Database Migration

Database schema management is handled using **Flyway**.

Migration files are stored under:

```text
src/main/resources/db/migration/
```

Example:

```text
V1__create_crawled_pages.sql
```

Flyway automatically applies database migrations when the application starts.

---

# 📁 Project Structure

```text
multithreaded-web-crawler/
│
├── pom.xml
├── README.md
│
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/
    │   │       └── crawler/
    │   │
    │   │       ├── config/
    │   │       │   └── CrawlerConfig.java
    │   │       │
    │   │       ├── concurrency/
    │   │       │   ├── ThreadPoolConfig.java
    │   │       │   ├── CrawlTaskExecutor.java
    │   │       │   └── CrawlCoordinator.java
    │   │       │
    │   │       ├── controller/
    │   │       │
    │   │       ├── crawler/
    │   │       │   ├── CrawlTask.java
    │   │       │   ├── CrawlQueue.java
    │   │       │   ├── Worker.java
    │   │       │   └── CrawlerEngine.java
    │   │       │
    │   │       ├── exception/
    │   │       │   └── FetchFailedException.java
    │   │       │
    │   │       ├── model/
    │   │       │   ├── CrawlResult.java
    │   │       │   ├── CrawledPage.java
    │   │       │   └── UrlStatus.java
    │   │       │
    │   │       ├── repository/
    │   │       │   └── CrawledPageRepository.java
    │   │       │
    │   │       ├── scraper/
    │   │       │   ├── HtmlParser.java
    │   │       │   ├── LinkExtractor.java
    │   │       │   └── DataExtractor.java
    │   │       │
    │   │       ├── service/
    │   │       │   └── UrlFetcherService.java
    │   │       │
    │   │       └── MultithreadedWebCrawlerApplication.java
    │   │
    │   └── resources/
    │       ├── application.properties
    │       └── db/
    │           └── migration/
    │               └── V1__create_crawled_pages.sql
    │
    └── test/
        └── java/
```

---

# 🧩 Core Components

## `CrawlerEngine`

Acts as the main crawling engine.

Responsible for:

* Starting the crawl
* Managing crawling lifecycle
* Submitting crawl tasks
* Coordinating workers

---

## `CrawlTask`

Represents a single unit of crawling work.

Conceptually:

```text
CrawlTask
    ↓
represents
    ↓
"Process this URL"
```

Example:

```text
CrawlTask
URL = https://example.com
```

It can then be submitted to the executor.

---

## `CrawlQueue`

Maintains URLs waiting to be processed.

```text
Discovered URLs
      ↓
CrawlQueue
      ↓
Workers
```

The queue must be thread-safe because multiple workers may access it concurrently.

---

## `Worker`

A worker processes crawling tasks.

Conceptually:

```text
Worker
  ↓
Take CrawlTask
  ↓
Fetch URL
  ↓
Parse HTML
  ↓
Extract links
  ↓
Persist result
  ↓
Queue new URLs
```

Multiple workers execute concurrently.

---

## `CrawlCoordinator`

Coordinates the interaction between:

```text
Crawler Engine
      ↓
Task Executor
      ↓
Workers
      ↓
Crawl Tasks
```

It helps control task submission and the overall crawling process.

---

## `UrlFetcherService`

Responsible for making HTTP requests.

```text
URL
 ↓
HTTP Client
 ↓
HTTP Response
 ↓
HTML
```

It also handles failures, retries and backoff behavior.

---

## `HtmlParser`

Responsible for parsing the downloaded HTML using JSoup.

```text
HTML
 ↓
JSoup
 ↓
DOM
```

---

## `LinkExtractor`

Extracts hyperlinks from the parsed HTML.

```text
HTML
 ↓
JSoup
 ↓
<a href="...">
 ↓
URL
```

---

## `DataExtractor`

Extracts useful information from a web page, such as:

```text
Title
Text
Metadata
```

---

## `CrawledPageRepository`

Responsible for database operations related to crawled pages.

It communicates with MySQL through Spring Data JPA.

---

# ⚙️ Technology Stack

| Technology      | Purpose                   |
| --------------- | ------------------------- |
| Java 17         | Programming language      |
| Spring Boot     | Application framework     |
| Maven           | Dependency management     |
| JSoup           | HTML parsing and scraping |
| Spring Data JPA | Database access           |
| MySQL           | Persistent storage        |
| Flyway          | Database migrations       |
| JUnit           | Testing                   |
| Git             | Version control           |

---

# 🛠️ Prerequisites

Make sure the following are installed:

* Java 17+
* Maven
* MySQL 8+
* Git

Verify Java:

```bash
java -version
```

Verify Maven:

```bash
mvn -version
```

---

# 🗄️ Database Setup

Create the database:

```sql
CREATE DATABASE web_crawler;
```

Configure the database in:

```text
src/main/resources/application.properties
```

Example:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/web_crawler
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD
```

Flyway will automatically execute the migration scripts.

---

# ▶️ Running the Application

Clone the repository:

```bash
git clone <repository-url>
```

Navigate into the project:

```bash
cd multithreaded-web-crawler
```

Build the project:

```bash
mvn clean install
```

Run the application:

```bash
mvn spring-boot:run
```

The application will start on:

```text
http://localhost:8080
```

---

# ⚙️ Configuration

Crawler configuration can be controlled through:

```text
application.properties
```

Example:

```properties
server.port=8080

crawler.thread-count=5
crawler.max-retries=3
crawler.connection-timeout=10
crawler.read-timeout=10
```

The exact configuration depends on the current implementation.

---

# 📊 Example Crawling Scenario

Suppose the seed URL is:

```text
https://example.com
```

The crawler starts:

```text
Seed URL
   ↓
https://example.com
```

The page contains:

```text
/page1
/page2
/page3
```

The queue becomes:

```text
Queue
 ├── /page1
 ├── /page2
 └── /page3
```

Three workers can process them concurrently:

```text
Worker 1 → /page1
Worker 2 → /page2
Worker 3 → /page3
```

Suppose `/page1` contains:

```text
/page4
/page5
```

The queue becomes:

```text
Queue
 ├── /page4
 └── /page5
```

The process continues until there are no more URLs to crawl or the configured crawl limits are reached.

---

# 🧵 Concurrency Model

The crawler follows a producer-consumer style architecture.

```text
                Producer
                   │
                   ▼
          ┌─────────────────┐
          │   Crawl Queue   │
          └────────┬────────┘
                   │
       ┌───────────┼───────────┐
       ▼           ▼           ▼
    Worker 1    Worker 2    Worker 3
       │           │           │
       ▼           ▼           ▼
     Fetch       Fetch       Fetch
       │           │           │
       └───────────┼───────────┘
                   ▼
              Process HTML
```

This architecture allows the crawler to process many URLs concurrently while maintaining control over shared resources.

---

# ⚠️ Error Handling

The crawler handles failures such as:

### HTTP Errors

```text
403 Forbidden
404 Not Found
429 Too Many Requests
500 Internal Server Error
503 Service Unavailable
```

### Network Errors

```text
Connection timeout
Read timeout
Connection refused
DNS failure
```

### Application Errors

```text
Invalid URL
HTML parsing failure
Database failure
```

Failed URLs are handled without terminating the entire crawling process.

---

# 🔐 Responsible Crawling

The crawler should be used responsibly.

When crawling real websites:

* Respect `robots.txt`
* Respect website terms of service
* Avoid excessive request rates
* Implement appropriate delays
* Handle `429` responses
* Avoid crawling private or restricted resources
* Use a meaningful User-Agent
* Do not overload target servers

---

# 🧪 Testing

Run the test suite using:

```bash
mvn test
```

Testing can cover:

* URL validation
* Link extraction
* HTML parsing
* Queue behavior
* Concurrent task execution
* Retry behavior
* HTTP failure handling
* Database persistence

---

# 📈 Future Improvements

The project can be extended with:

* [ ] Distributed crawling across multiple machines
* [ ] Redis-based distributed queue
* [ ] Distributed visited-set
* [ ] URL prioritization
* [ ] Crawl depth limits
* [ ] Domain restrictions
* [ ] Robots.txt support
* [ ] Crawl rate limiting
* [ ] Proxy support
* [ ] Metrics and monitoring
* [ ] Prometheus + Grafana
* [ ] Dockerization
* [ ] Kubernetes deployment
* [ ] Kafka-based task distribution
* [ ] Distributed database architecture
* [ ] Elasticsearch for indexed page content
* [ ] REST API for starting and monitoring crawls
* [ ] Web dashboard for crawl statistics

---

# 🧠 System Design Concepts Demonstrated

This project demonstrates several important system-design and backend concepts:

### 1. Multithreading

Multiple workers process URLs concurrently.

### 2. Thread Pools

A bounded number of worker threads prevents uncontrolled thread creation.

### 3. Producer-Consumer Pattern

URL discovery produces tasks while workers consume them.

### 4. Concurrency Control

Shared state is protected from race conditions.

### 5. Thread-Safe Data Structures

Concurrent collections can be used for shared crawler state.

### 6. Retry & Backoff

Temporary failures are handled without immediately giving up.

### 7. Rate Limiting

Requests can be controlled to avoid overwhelming target servers.

### 8. Persistence

Crawl results are stored in a relational database.

### 9. Fault Tolerance

Individual URL failures do not necessarily stop the entire crawler.

### 10. Scalability

The worker count can be increased to process more URLs concurrently.

### 11. Separation of Concerns

Fetching, parsing, crawling, persistence and concurrency are separated into different components.

### 12. Producer-Consumer Architecture

The crawl queue decouples URL discovery from URL processing.

---

# 📌 Key Learning Outcomes

Through this project, the following concepts are practiced:

```text
Java Multithreading
        ↓
ExecutorService / Thread Pools
        ↓
Concurrent Data Structures
        ↓
Producer-Consumer Pattern
        ↓
HTTP Networking
        ↓
HTML Parsing
        ↓
Web Scraping
        ↓
Database Persistence
        ↓
Concurrency Handling
        ↓
Retry & Backoff
        ↓
Fault Tolerance
        ↓
System Scalability
```

---

# 🎯 Project Goal

The primary goal of this project is to understand how a real-world crawler can be designed using:

```text
Multithreading
+
Concurrency Control
+
HTTP Networking
+
HTML Scraping
+
Persistent Storage
+
Fault Handling
```

rather than implementing a simple single-threaded recursive crawler.

---

# 👨‍💻 Author

**Yash Bhatt**

B.Tech — Computer Science & Engineering

Poornima University

---

## ⭐ If you find this project useful

Give the repository a ⭐ and feel free to explore the implementation.

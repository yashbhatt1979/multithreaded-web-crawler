# Multithreaded Web Crawler

A Java-based multithreaded web crawler built using Spring Boot.

## Tech Stack

* Java 17
* Spring Boot
* Maven
* JSoup
* MySQL
* Git & GitHub

## Features

* REST API for starting a crawl
* Webpage fetching using JSoup
* HTML parsing
* URL extraction
* Continuous webpage crawling
* Duplicate URL detection
* Thread-safe URL management
* Multithreaded web crawling
* MySQL data storage

## Current Progress

* [x] Spring Boot project setup
* [x] `CrawlerApplication` created
* [x] `CrawlerController` created
* [x] `CrawlerService` created
* [x] Controller → Service communication tested successfully
* [x] `CrawlerEngine` created
* [x] Webpage fetching using JSoup
* [x] HTML parsing
* [x] URL extraction
* [ ] URL queue
* [ ] Continuous crawling
* [ ] Duplicate URL detection
* [ ] Crawler workers
* [ ] Multithreading
* [ ] Thread-safe URL management
* [ ] MySQL integration
* [ ] Complete crawler workflow

## Project Structure

```text
multithreaded-web-crawler/
│
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── example/
│       │           └── crawler/
│       │               │
│       │               ├── CrawlerApplication.java
│       │               │
│       │               ├── controller/
│       │               │   └── CrawlerController.java
│       │               │
│       │               ├── service/
│       │               │   └── CrawlerService.java
│       │               │
│       │               ├── engine/
│       │               │   └── CrawlerEngine.java
│       │               │
│       │               ├── queue/
│       │               │   └── UrlQueue.java
│       │               │
│       │               ├── worker/
│       │               │   └── CrawlerWorker.java
│       │               │
│       │               ├── repository/
│       │               │   └── ...
│       │               │
│       │               ├── model/
│       │               │   └── ...
│       │               │
│       │               └── config/
│       │                   └── ...
│       │
│       └── resources/
│           └── application.properties
│
├── pom.xml
├── mvnw
├── mvnw.cmd
├── .gitignore
└── README.md
```

> The project structure will be expanded as new crawler components are implemented.

## Current Request Flow

At the current stage, the crawler works as follows:

```text
Postman
   ↓
HTTP Request
   ↓
Tomcat
   ↓
CrawlerController
   ↓
CrawlerService
   ↓
CrawlerEngine
   ↓
JSoup
   ↓
Website
   ↓
HTML Response
   ↓
HTML Parsing
   ↓
URL Extraction
   ↓
List of Discovered URLs
```

The current implementation **does not recursively crawl the discovered URLs yet**.

## Planned Crawler Flow

The complete crawler will eventually work approximately like this:

```text
Postman
   ↓
CrawlerController
   ↓
CrawlerService
   ↓
CrawlerEngine
   ↓
URL Queue
   ↓
Crawler Workers
   ↓
Multiple Threads
   ↓
JSoup
   ↓
Fetch Webpage
   ↓
Extract URLs
   ↓
Duplicate URL Check
   ↓
New URLs → URL Queue
   ↓
Continue Crawling
   ↓
Store Data in MySQL
```

## API

### Start Crawling

**Endpoint:**

```text
POST /crawl
```

**Example:**

```text
POST http://localhost:8080/crawl?url=https://example.com
```

The request is received by `CrawlerController`, which passes the URL to `CrawlerService`.

`CrawlerService` delegates the webpage crawling operation to `CrawlerEngine`.

`CrawlerEngine` currently uses JSoup to:

1. Connect to the provided URL
2. Download the webpage HTML
3. Parse the HTML
4. Find `<a href="">` elements
5. Extract absolute URLs
6. Return the discovered URLs

### Example Response

```json
[
    "https://example.com/about",
    "https://example.com/contact",
    "https://example.com/products"
]
```

## Running the Project

Make sure Java 17 and Maven are installed.

Run the application using:

```bash
mvn spring-boot:run
```

The application runs by default on:

```text
http://localhost:8080
```

## Development Status

The basic Spring Boot REST layer and initial crawling functionality have been completed and tested successfully.

The current `CrawlerEngine` can fetch a webpage using JSoup, parse its HTML, and extract URLs from `<a href="">` elements.

The crawler currently **only discovers URLs from the provided webpage**. It does not yet visit those discovered URLs.

The next development stage is to introduce a **URL queue and continuous crawling mechanism**. After that, duplicate URL detection, crawler workers, multithreading, thread-safe URL management, and MySQL integration will be implemented.

The final goal is to build a complete **multithreaded web crawler** capable of continuously discovering, managing, crawling, and storing information from webpages.

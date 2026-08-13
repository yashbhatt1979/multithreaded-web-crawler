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
* Multithreaded web crawling
* Thread-safe URL management
* HTML parsing
* Duplicate URL detection
* MySQL data storage

## Current Progress

* [x] Spring Boot project setup
* [x] `CrawlerApplication` created
* [x] `CrawlerController` created
* [x] `CrawlerService` created
* [x] Controller → Service communication tested successfully
* [ ] Crawler Engine
* [ ] Webpage fetching using JSoup
* [ ] HTML parsing
* [ ] URL extraction
* [ ] Duplicate URL detection
* [ ] Multithreading
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
│       │               ├── CrawlerApplication.java
│       │               │
│       │               ├── controller/
│       │               │   └── CrawlerController.java
│       │               │
│       │               └── service/
│       │                   └── CrawlerService.java
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

## Current Request Flow

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
Crawler Engine
   ↓
JSoup
   ↓
MySQL
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

The request is currently handled by `CrawlerController`, which passes the URL to `CrawlerService`.

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

The basic Spring Boot REST layer has been completed and tested successfully.

The next stage is implementing the `CrawlerEngine`, which will handle webpage fetching and crawling logic.

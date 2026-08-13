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

* Multithreaded web crawling
* Thread-safe URL management
* HTML parsing
* Duplicate URL detection
* MySQL data storage
* REST API

## Current API

### POST /crawl

Accepts a URL as a request parameter.

Example:

POST /crawl?url=https://example.com

Current response:

Received URL : https://example.com

> The endpoint currently verifies that the URL is successfully received. Actual crawling functionality will be implemented in the next stages.

## Project Status

🚧 Under Development

### Completed

* Initial Maven project setup
* Spring Boot application configuration
* REST Controller implementation
* `/crawl` POST endpoint
* URL request parameter handling

### Coming Next

* Crawler Service
* Webpage fetching
* JSoup HTML parsing
* URL extraction
* Duplicate URL detection
* Multithreaded crawling
* Thread-safe URL management
* MySQL database integration

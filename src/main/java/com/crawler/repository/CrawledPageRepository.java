package com.crawler.repository;

import com.crawler.model.CrawledPage;
import com.crawler.model.UrlStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CrawledPageRepository
        extends JpaRepository<CrawledPage, Long> {

    Optional<CrawledPage> findByUrl(String url);

    boolean existsByUrl(String url);

    Optional<CrawledPage> findByUrlAndStatus(
            String url,
            UrlStatus status
    );
}
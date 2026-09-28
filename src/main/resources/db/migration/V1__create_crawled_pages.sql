CREATE TABLE crawled_pages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    url VARCHAR(2048) NOT NULL,

    title VARCHAR(500),

    description TEXT,

    content LONGTEXT,

    http_status INT,

    depth INT NOT NULL,

    content_hash VARCHAR(64),

    url_hash CHAR(64) NOT NULL,

    status VARCHAR(50) NOT NULL,

    crawled_at TIMESTAMP,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uk_crawled_pages_url_hash UNIQUE (url_hash),

    INDEX idx_crawled_pages_status (status),

    INDEX idx_crawled_pages_depth (depth),

    INDEX idx_crawled_pages_content_hash (content_hash)
);
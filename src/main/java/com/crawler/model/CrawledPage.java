package com.crawler.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "crawled_pages")
public class CrawledPage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "url", nullable = false, length = 2048, unique = true)
    private String url;

    @Column(name = "url_hash", nullable = false, length = 64)
    private String urlHash;

    @Column(name = "http_status")
    private Integer statusCode;

    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "description")
    private String description;

    @Lob
    @Column(name = "content", columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "depth", nullable = false)
    private Integer depth;

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private UrlStatus status;

    @Column(name = "crawled_at")
    private LocalDateTime crawledAt;

    @Column(
            name = "created_at",
            nullable = false,
            insertable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false,
            insertable = false,
            updatable = false
    )
    private LocalDateTime updatedAt;

    public CrawledPage() {
    }

    public CrawledPage(
            String url,
            Integer statusCode,
            String title,
            String content,
            Integer depth,
            UrlStatus status
    ) {
        this.url = url;
        this.urlHash = generateSha256(url);
        this.statusCode = statusCode;
        this.title = title;
        this.content = content;
        this.depth = depth;
        this.status = status;
        this.crawledAt = LocalDateTime.now();
    }

    private String generateSha256(String value) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(
                            value.getBytes(StandardCharsets.UTF_8)
                    );

            StringBuilder hexString =
                    new StringBuilder();

            for (byte b : hash) {

                String hex =
                        Integer.toHexString(0xff & b);

                if (hex.length() == 1) {
                    hexString.append('0');
                }

                hexString.append(hex);
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {

            throw new IllegalStateException(
                    "SHA-256 algorithm not available",
                    e
            );
        }
    }

    public Long getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
        this.urlHash = generateSha256(url);
    }

    public String getUrlHash() {
        return urlHash;
    }

    public void setUrlHash(String urlHash) {
        this.urlHash = urlHash;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getDepth() {
        return depth;
    }

    public void setDepth(Integer depth) {
        this.depth = depth;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public UrlStatus getStatus() {
        return status;
    }

    public void setStatus(UrlStatus status) {
        this.status = status;
    }

    public LocalDateTime getCrawledAt() {
        return crawledAt;
    }

    public void setCrawledAt(LocalDateTime crawledAt) {
        this.crawledAt = crawledAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
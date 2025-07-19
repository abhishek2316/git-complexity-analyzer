package com.gitanalyzer.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "url_search_logs")
@Data                    // Generates getters, setters, toString, equals, hashCode
@NoArgsConstructor       // Generates no-argument constructor
public class UrlSearchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "github_url", length = 1000, nullable = false)
    @NotBlank(message = "GitHub URL is required")
    @Size(max = 1000, message = "URL too long")
    private String githubUrl;

    @Column(name = "search_type", length = 50)
    private String searchType;

    @Column(name = "extracted_username", length = 100)
    private String extractedUsername;

    @Column(name = "extracted_repo", length = 255)
    private String extractedRepo;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "search_timestamp", nullable = false)
    private LocalDateTime searchTimestamp;

    @Column(name = "response_status", length = 20)
    private String responseStatus;

    @Column(name = "processing_time_ms")
    private Integer processingTimeMs;

    // Custom constructor for common use case
    public UrlSearchLog(String githubUrl, String ipAddress, String userAgent) {
        this.githubUrl = githubUrl;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.searchTimestamp = LocalDateTime.now();
    }

    // PrePersist to ensure timestamp is set
    @PrePersist
    private void prePersist() {

    }


    public void setResponseStatus(String pending) {
    }
}

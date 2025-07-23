package com.gitanalyzer.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonBackReference;
import java.time.LocalDateTime;

@Entity
@Table(name = "commits")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Commit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Foreign key relationship with Repository
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    @JsonBackReference
    private Repository repository;

    @Column(name = "commit_sha", length = 40, unique = true, nullable = false)
    @NotBlank(message = "Commit SHA is required")
    @Size(max = 40, message = "Commit SHA must be 40 characters")
    private String commitSha;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "author_name", length = 255)
    private String authorName;

    @Column(name = "author_email", length = 255)
    private String authorEmail;

    @Column(name = "author_date")
    private LocalDateTime authorDate;

    @Column(name = "committer_name", length = 255)
    private String committerName;

    @Column(name = "committer_email", length = 255)
    private String committerEmail;

    @Column(name = "committer_date")
    private LocalDateTime committerDate;

    @Column(name = "additions")
    private Integer additions;

    @Column(name = "deletions")
    private Integer deletions;

    @Column(name = "changed_files")
    private Integer changedFiles;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Custom constructor for API data
    public Commit(Repository repository, String commitSha, String message, String authorName) {
        this.repository = repository;
        this.commitSha = commitSha;
        this.message = message;
        this.authorName = authorName;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    private void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

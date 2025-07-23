package com.gitanalyzer.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "repositories")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Repository {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Foreign key relationship with User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference
    private User user;

    @Column(name = "repo_name", length = 255, nullable = false)
    @NotBlank(message = "Repository name is required")
    @Size(max = 255, message = "Repository name too long")
    private String repoName;

    @Column(name = "full_name", length = 255)
    private String fullName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "language", length = 100)
    private String language;

    @Column(name = "stars_count")
    private Integer starsCount;

    @Column(name = "forks_count")
    private Integer forksCount;

    @Column(name = "watchers_count")
    private Integer watchersCount;

    @Column(name = "size_kb")
    private Integer sizeKb;

    @Column(name = "default_branch", length = 100)
    private String defaultBranch;

    @Column(name = "is_private")
    private Boolean isPrivate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_push_at")
    private LocalDateTime lastPushAt;

    @Column(name = "analyzed_at")
    private LocalDateTime analyzedAt;

    // One-to-many relationships
    @OneToMany(mappedBy = "repository", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Commit> commits;

    @OneToMany(mappedBy = "repository", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Contributor> contributors;

    // Custom constructor for API data
    public Repository(User user, String repoName, String fullName, String language) {
        this.user = user;
        this.repoName = repoName;
        this.fullName = fullName;
        this.language = language;
        this.analyzedAt = LocalDateTime.now();
    }

    @PrePersist
    private void prePersist() {
        if (analyzedAt == null) {
            analyzedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    private void preUpdate() {
        analyzedAt = LocalDateTime.now();
    }
}

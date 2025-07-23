package com.gitanalyzer.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor

public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "github_username", length = 100, unique = true, nullable = false)
    @NotBlank(message = "GitHub username is required")
    @Size(max = 100, message = "Username too long")
    private String githubUsername;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "company", length = 255)
    private String company;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "public_repos")
    private Integer publicRepos;

    @Column(name = "followers")
    private Integer followers;

    @Column(name = "following")
    private Integer following;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // One-to-many relationship with repositories
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Repository> repositories;

    // Custom constructor for API data
    public User(String githubUsername, String name, String avatarUrl) {
        this.githubUsername = githubUsername;
        this.name = name;
        this.avatarUrl = avatarUrl;
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    private void prePersist() {
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    private void preUpdate() {
        updatedAt = LocalDateTime.now();
    }


}

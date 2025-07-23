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
@Table(name = "contributors")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Contributor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Foreign key relationship with Repository
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    @JsonBackReference
    private Repository repository;

    @Column(name = "contributor_name", length = 255)
    private String contributorName;

    @Column(name = "contributor_email", length = 255)
    private String contributorEmail;

    @Column(name = "contribution_count")
    private Integer contributionCount;

    @Column(name = "first_contribution_date")
    private LocalDateTime firstContributionDate;

    @Column(name = "last_contribution_date")
    private LocalDateTime lastContributionDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Custom constructor for API data
    public Contributor(Repository repository, String contributorName, String contributorEmail, Integer contributionCount) {
        this.repository = repository;
        this.contributorName = contributorName;
        this.contributorEmail = contributorEmail;
        this.contributionCount = contributionCount;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    private void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
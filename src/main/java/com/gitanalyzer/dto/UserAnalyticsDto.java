package com.gitanalyzer.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAnalyticsDto {

    // Basic user information
    private String githubUsername;
    private String name;
    private String avatarUrl;
    private String bio;
    private String location;
    private String company;
    private String email;

    // GitHub statistics
    private Integer publicRepos;
    private Integer followers;
    private Integer following;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // Analytics data
    private RepositoryStats repositoryStats;
    private ContributionStats contributionStats;
    private ActivityStats activityStats;
    private List<TopRepositoryDto> topRepositories;
    private List<LanguageStatsDto> languageBreakdown;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RepositoryStats {
        private Integer totalRepositories;
        private Integer totalStars;
        private Integer totalForks;
        private Integer totalWatchers;
        private Long totalSizeKb;
        private Double averageStars;
        private String mostUsedLanguage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContributionStats {
        private Integer totalCommits;
        private Integer totalAdditions;
        private Integer totalDeletions;
        private Integer totalChangedFiles;
        private Double averageCommitsPerRepo;
        private String mostActiveRepository;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityStats {
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime lastActivity;

        private Integer commitsLastMonth;
        private Integer commitsLastWeek;
        private List<DailyActivityDto> dailyActivity;
        private Map<Integer, Integer> hourlyActivity; // hour -> commit count
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopRepositoryDto {
        private String repoName;
        private String fullName;
        private String description;
        private String language;
        private Integer starsCount;
        private Integer forksCount;
        private Integer commitsCount;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime lastPushAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LanguageStatsDto {
        private String language;
        private Integer repositoryCount;
        private Integer totalStars;
        private Double percentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyActivityDto {
        @JsonFormat(pattern = "yyyy-MM-dd")
        private String date;
        private Integer commits;
        private Integer additions;
        private Integer deletions;
    }
}
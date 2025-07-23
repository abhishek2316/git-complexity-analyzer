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
public class RepoAnalyticsDto {

    // Basic repository information
    private String repoName;
    private String fullName;
    private String description;
    private String language;
    private String defaultBranch;
    private Boolean isPrivate;

    // Repository statistics
    private Integer starsCount;
    private Integer forksCount;
    private Integer watchersCount;
    private Integer sizeKb;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastPushAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime analyzedAt;

    // Owner information
    private OwnerDto owner;

    // Analytics data
    private CommitAnalytics commitAnalytics;
    private ContributorAnalytics contributorAnalytics;
    private CodeAnalytics codeAnalytics;
    private ActivityAnalytics activityAnalytics;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OwnerDto {
        private String githubUsername;
        private String name;
        private String avatarUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommitAnalytics {
        private Integer totalCommits;
        private Integer totalAdditions;
        private Integer totalDeletions;
        private Integer totalChangedFiles;
        private Double averageAdditionsPerCommit;
        private Double averageDeletionsPerCommit;
        private Double averageFilesChangedPerCommit;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime firstCommit;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime lastCommit;

        private List<CommitTimelineDto> commitTimeline;
        private Map<Integer, Integer> commitsByHour;
        private List<TopCommitDto> topCommits;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContributorAnalytics {
        private Integer totalContributors;
        private Integer activeContributors; // contributors with activity in last 3 months
        private List<TopContributorDto> topContributors;
        private List<ContributorTimelineDto> contributorTimeline;
        private Map<String, Integer> contributionDistribution;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CodeAnalytics {
        private Integer totalLines; // additions - deletions
        private Double codeChurnRate; // (additions + deletions) / total commits
        private List<String> mainFileTypes;
        private Map<String, Integer> fileTypeDistribution;
        private Integer averageCommitSize;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityAnalytics {
        private Boolean isActive; // has commits in last 30 days
        private Integer commitsLastWeek;
        private Integer commitsLastMonth;
        private Integer commitsLastYear;
        private Double weeklyAverageCommits;
        private Double monthlyAverageCommits;
        private List<String> busiestDays; // days of week with most commits
        private List<String> busiestHours; // hours with most commits
    }

    // Nested DTOs for detailed analytics
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommitTimelineDto {
        @JsonFormat(pattern = "yyyy-MM-dd")
        private String date;
        private Integer commits;
        private Integer additions;
        private Integer deletions;
        private Integer uniqueContributors;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopCommitDto {
        private String commitSha;
        private String message;
        private String authorName;
        private Integer additions;
        private Integer deletions;
        private Integer changedFiles;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime authorDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopContributorDto {
        private String contributorName;
        private String contributorEmail;
        private Integer contributionCount;
        private Integer commitsCount;
        private Integer additionsCount;
        private Integer deletionsCount;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime firstContribution;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime lastContribution;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContributorTimelineDto {
        @JsonFormat(pattern = "yyyy-MM-dd")
        private String date;
        private Integer newContributors;
        private Integer activeContributors;
        private Integer totalContributors;
    }
}

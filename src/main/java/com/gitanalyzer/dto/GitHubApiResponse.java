package com.gitanalyzer.dto;

import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;

// GitHub API Response DTOs for external API calls
public class GitHubApiResponse {

    // GitHub User API Response
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GitHubUser {
        private String login;
        private Long id;
        private String name;

        @JsonProperty("avatar_url")
        private String avatarUrl;

        private String bio;
        private String location;
        private String company;
        private String email;

        @JsonProperty("public_repos")
        private Integer publicRepos;

        private Integer followers;
        private Integer following;

        @JsonProperty("created_at")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        private LocalDateTime createdAt;

        @JsonProperty("updated_at")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        private LocalDateTime updatedAt;
    }

    // GitHub Repository API Response
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GitHubRepository {
        private Long id;
        private String name;

        @JsonProperty("full_name")
        private String fullName;

        private String description;
        private String language;

        @JsonProperty("private")
        private Boolean isPrivate;

        @JsonProperty("default_branch")
        private String defaultBranch;

        @JsonProperty("stargazers_count")
        private Integer stargazersCount;

        @JsonProperty("forks_count")
        private Integer forksCount;

        @JsonProperty("watchers_count")
        private Integer watchersCount;

        private Integer size; // in KB

        @JsonProperty("created_at")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        private LocalDateTime createdAt;

        @JsonProperty("updated_at")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        private LocalDateTime updatedAt;

        @JsonProperty("pushed_at")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        private LocalDateTime pushedAt;

        private GitHubUser owner;

    }

    // GitHub Commit API Response
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GitHubCommit {
        private String sha;
        private CommitDetails commit;
        private GitHubCommitAuthor author;
        private GitHubCommitAuthor committer;
        private CommitStats stats;

        @JsonIgnoreProperties(ignoreUnknown = true)
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class CommitDetails {
            private String message;
            private CommitAuthorDetails author;
            private CommitAuthorDetails committer;

            @Data
            @NoArgsConstructor
            @AllArgsConstructor
            public static class CommitAuthorDetails {
                private String name;
                private String email;

                @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
                private LocalDateTime date;
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class GitHubCommitAuthor {
            private String login;
            private Long id;

            @JsonProperty("avatar_url")
            private String avatarUrl;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class CommitStats {
            private Integer additions;
            private Integer deletions;
            private Integer total;
        }
    }

    // GitHub Contributor API Response
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GitHubContributor {
        private String login;
        private Long id;

        @JsonProperty("avatar_url")
        private String avatarUrl;

        private Integer contributions;
        private String type;
    }

    // Generic API Response Wrapper
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ApiResponseWrapper<T> {
        private boolean success;
        private String message;
        private T data;
        private ApiError error;
        private ApiMetadata metadata;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ApiError {
            private String code;
            private String message;
            private String details;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        @Getter
        @Setter
        public static class ApiMetadata {
            private Integer totalCount;
            private Integer pageSize;
            private Integer currentPage;
            private Integer totalPages;
            private Boolean hasNext;
            private Boolean hasPrevious;
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            private LocalDateTime timestamp;
            private Long processingTimeMs;
        }
    }
}
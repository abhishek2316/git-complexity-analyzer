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
public class SearchAnalyticsDto {

    // Search statistics
    private Long totalSearches;
    private Long successfulSearches;
    private Long failedSearches;
    private Double successRate;

    // Time-based analytics
    private List<DailySearchDto> dailySearches;
    private Map<Integer, Integer> hourlyDistribution;
    private List<TopSearchedUserDto> topSearchedUsers;
    private List<TopSearchedRepoDto> topSearchedRepositories;

    // Performance metrics
    private Double averageProcessingTime;
    private Integer maxProcessingTime;
    private Integer minProcessingTime;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailySearchDto {
        @JsonFormat(pattern = "yyyy-MM-dd")
        private String date;
        private Integer totalSearches;
        private Integer successfulSearches;
        private Integer failedSearches;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopSearchedUserDto {
        private String username;
        private Integer searchCount;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime lastSearched;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopSearchedRepoDto {
        private String repositoryName;
        private String ownerUsername;
        private Integer searchCount;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime lastSearched;
    }
}

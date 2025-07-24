package com.gitanalyzer.service;

import com.gitanalyzer.dto.SearchAnalyticsDto;
import com.gitanalyzer.model.UrlSearchLog;
import com.gitanalyzer.repository.UrlSearchLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SearchLogService {

    @Autowired
    private UrlSearchLogRepository urlSearchLogRepository;

    // GitHub URL patterns
    private static final String USER_PATTERN = "https://github\\.com/([^/]+)/?$";
    private static final String REPO_PATTERN = "https://github\\.com/([^/]+)/([^/]+)/?$";

    public UrlSearchLog logSearch(String githubUrl, String ipAddress, String userAgent) {

        UrlSearchLog log = new UrlSearchLog(githubUrl, ipAddress, userAgent);

        // Parse URL and extract information
        parseGitHubUrl(log, githubUrl);

        // Set initial status
        log.setResponseStatus("PENDING");

        // Save to database
        return urlSearchLogRepository.save(log);
    }

    private void parseGitHubUrl(UrlSearchLog log, String url) {
        try {
            // Check if it's a repository URL
            Pattern repoPattern = Pattern.compile(REPO_PATTERN);
            Matcher repoMatcher = repoPattern.matcher(url);

            if (repoMatcher.matches()) {
                log.setSearchType("REPOSITORY");
                log.setExtractedUsername(repoMatcher.group(1));
                log.setExtractedRepo(repoMatcher.group(2));
                return;
            }

            // Check if it's a user profile URL
            Pattern userPattern = Pattern.compile(USER_PATTERN);
            Matcher userMatcher = userPattern.matcher(url);

            if (userMatcher.matches()) {
                log.setSearchType("USER_PROFILE");
                log.setExtractedUsername(userMatcher.group(1));
                return;
            }

            // If no pattern matches
            log.setSearchType("UNKNOWN");

        } catch (Exception e) {
            log.setSearchType("ERROR");
        }
    }

    public void updateLogStatus(Long logId, String status, Integer processingTime) {
        UrlSearchLog log = urlSearchLogRepository.findById(logId).orElse(null);
        if (log != null) {
            log.setResponseStatus(status);
            log.setProcessingTimeMs(processingTime);
            urlSearchLogRepository.save(log);
        }
    }

    /**
     * Get search analytics for the specified number of days
     * @param days Number of days to analyze
     * @return SearchAnalyticsDto with comprehensive analytics
     */
    public SearchAnalyticsDto getSearchAnalytics(int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);

        // Get all search logs within the date range
//        List<UrlSearchLog> searchLogs = urlSearchLogRepository.findByCreatedAtAfter(startDate);
        List<UrlSearchLog> searchLogs = urlSearchLogRepository.findBySearchTimestampAfter(startDate);

        // Calculate basic statistics
        long totalSearches = searchLogs.size();
        long successfulSearches = searchLogs.stream()
                .filter(log -> "SUCCESS".equals(log.getResponseStatus()))
                .count();
        long failedSearches = totalSearches - successfulSearches;
        double successRate = totalSearches > 0 ? (double) successfulSearches / totalSearches * 100 : 0.0;

        // Calculate performance metrics
        OptionalDouble avgProcessingTime = searchLogs.stream()
                .filter(log -> log.getProcessingTimeMs() != null)
                .mapToInt(UrlSearchLog::getProcessingTimeMs)
                .average();

        Optional<Integer> maxProcessingTime = searchLogs.stream()
                .filter(log -> log.getProcessingTimeMs() != null)
                .map(UrlSearchLog::getProcessingTimeMs)
                .max(Integer::compareTo);

        Optional<Integer> minProcessingTime = searchLogs.stream()
                .filter(log -> log.getProcessingTimeMs() != null)
                .map(UrlSearchLog::getProcessingTimeMs)
                .min(Integer::compareTo);

        // Group by date for daily analytics
        Map<String, List<UrlSearchLog>> dailyGroups = searchLogs.stream()
                .collect(Collectors.groupingBy(log ->
                        log.getSearchTimestamp().toLocalDate().toString())); // Changed from getCreatedAt()

        List<SearchAnalyticsDto.DailySearchDto> dailySearches = dailyGroups.entrySet().stream()
                .map(entry -> {
                    List<UrlSearchLog> dayLogs = entry.getValue();
                    int dayTotal = dayLogs.size();
                    int daySuccessful = (int) dayLogs.stream()
                            .filter(log -> "SUCCESS".equals(log.getResponseStatus()))
                            .count();
                    int dayFailed = dayTotal - daySuccessful;

                    return SearchAnalyticsDto.DailySearchDto.builder()
                            .date(entry.getKey())
                            .totalSearches(dayTotal)
                            .successfulSearches(daySuccessful)
                            .failedSearches(dayFailed)
                            .build();
                })
                .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                .collect(Collectors.toList());

        // Group by hour for hourly distribution
        Map<Integer, Integer> hourlyDistribution = searchLogs.stream()
                .collect(Collectors.groupingBy(
                        log -> log.getSearchTimestamp().getHour(), // Changed from getCreatedAt()
                        Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
                ));

        // Top searched users
        List<SearchAnalyticsDto.TopSearchedUserDto> topSearchedUsers = searchLogs.stream()
                .filter(log -> "USER_PROFILE".equals(log.getSearchType()))
                .filter(log -> log.getExtractedUsername() != null)
                .collect(Collectors.groupingBy(UrlSearchLog::getExtractedUsername))
                .entrySet().stream()
                .map(entry -> {
                    String username = entry.getKey();
                    List<UrlSearchLog> userLogs = entry.getValue();
                    LocalDateTime lastSearched = userLogs.stream()
                            .map(UrlSearchLog::getSearchTimestamp) // Changed from getCreatedAt()
                            .max(LocalDateTime::compareTo)
                            .orElse(null);

                    return SearchAnalyticsDto.TopSearchedUserDto.builder()
                            .username(username)
                            .searchCount(userLogs.size())
                            .lastSearched(lastSearched)
                            .build();
                })
                .sorted((a, b) -> b.getSearchCount().compareTo(a.getSearchCount()))
                .limit(10)
                .collect(Collectors.toList());

        // Top searched repositories
        List<SearchAnalyticsDto.TopSearchedRepoDto> topSearchedRepositories = searchLogs.stream()
                .filter(log -> "REPOSITORY".equals(log.getSearchType()))
                .filter(log -> log.getExtractedRepo() != null && log.getExtractedUsername() != null)
                .collect(Collectors.groupingBy(log -> log.getExtractedUsername() + "/" + log.getExtractedRepo()))
                .entrySet().stream()
                .map(entry -> {
                    String repoKey = entry.getKey();
                    List<UrlSearchLog> repoLogs = entry.getValue();
                    String[] parts = repoKey.split("/");
                    LocalDateTime lastSearched = repoLogs.stream()
                            .map(UrlSearchLog::getSearchTimestamp) // Changed from getCreatedAt()
                            .max(LocalDateTime::compareTo)
                            .orElse(null);

                    return SearchAnalyticsDto.TopSearchedRepoDto.builder()
                            .repositoryName(parts.length > 1 ? parts[1] : "")
                            .ownerUsername(parts.length > 0 ? parts[0] : "")
                            .searchCount(repoLogs.size())
                            .lastSearched(lastSearched)
                            .build();
                })
                .sorted((a, b) -> b.getSearchCount().compareTo(a.getSearchCount()))
                .limit(10)
                .collect(Collectors.toList());

        return SearchAnalyticsDto.builder()
                .totalSearches(totalSearches)
                .successfulSearches(successfulSearches)
                .failedSearches(failedSearches)
                .successRate(successRate)
                .dailySearches(dailySearches)
                .hourlyDistribution(hourlyDistribution)
                .topSearchedUsers(topSearchedUsers)
                .topSearchedRepositories(topSearchedRepositories)
                .averageProcessingTime(avgProcessingTime.orElse(0.0))
                .maxProcessingTime(maxProcessingTime.orElse(0))
                .minProcessingTime(minProcessingTime.orElse(0))
                .build();
    }



}

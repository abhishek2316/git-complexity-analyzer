package com.gitanalyzer.controller;

import com.gitanalyzer.dto.ApiResponseDto;
import com.gitanalyzer.dto.RepoAnalyticsDto;
import com.gitanalyzer.dto.SearchAnalyticsDto;
import com.gitanalyzer.dto.UserAnalyticsDto;
import com.gitanalyzer.model.Repository;
import com.gitanalyzer.service.RepoAnalyticsService;
import com.gitanalyzer.service.SearchLogService;
import com.gitanalyzer.service.UserAnalyticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for Analytics operations
 * Handles all analytics-related endpoints including user analytics, repository analytics, and search analytics
 */
@Slf4j
@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "*")
public class AnalyticsController {

    @Autowired
    private UserAnalyticsService userAnalyticsService;

    @Autowired
    private RepoAnalyticsService repoAnalyticsService;

    @Autowired
    private SearchLogService searchLogService;

    // ======================== USER ANALYTICS ENDPOINTS ========================

    /**
     * Get comprehensive analytics for a GitHub user
     * GET /api/analytics/user/{username}
     */
    @GetMapping("/user/{username}")
    public ResponseEntity<ApiResponseDto<UserAnalyticsDto>> getUserAnalytics(@PathVariable String username) {
        long startTime = System.currentTimeMillis();

        try {
            log.info("Fetching analytics for user: {}", username);

            Optional<UserAnalyticsDto> analytics = userAnalyticsService.getUserAnalytics(username);

            long processingTime = System.currentTimeMillis() - startTime;

            if (analytics.isPresent()) {
                ApiResponseDto<UserAnalyticsDto> response = ApiResponseDto.success(
                        analytics.get(),
                        "User analytics retrieved successfully"
                );
                response.setProcessingTimeMs(processingTime);

                log.info("Successfully retrieved analytics for user: {} in {}ms", username, processingTime);
                return ResponseEntity.ok(response);
            } else {
                ApiResponseDto<UserAnalyticsDto> response = ApiResponseDto.error(
                        "USER_NOT_FOUND",
                        "User not found or analytics could not be generated: " + username,
                        "The specified GitHub user does not exist or could not be analyzed"
                );
                response.setProcessingTimeMs(processingTime);

                log.warn("User analytics not found for: {}", username);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("Error retrieving user analytics for {}: {}", username, e.getMessage(), e);

            ApiResponseDto<UserAnalyticsDto> response = ApiResponseDto.error(
                    "ANALYTICS_ERROR",
                    "Error retrieving user analytics",
                    e.getMessage()
            );
            response.setProcessingTimeMs(processingTime);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Refresh/update user analytics by fetching latest data from GitHub
     * POST /api/analytics/user/{username}/refresh
     */
    @PostMapping("/user/{username}/refresh")
    public ResponseEntity<ApiResponseDto<UserAnalyticsDto>> refreshUserAnalytics(@PathVariable String username) {
        long startTime = System.currentTimeMillis();

        try {
            log.info("Refreshing analytics for user: {}", username);

            Optional<UserAnalyticsDto> analytics = userAnalyticsService.refreshUserAnalytics(username);

            long processingTime = System.currentTimeMillis() - startTime;

            if (analytics.isPresent()) {
                ApiResponseDto<UserAnalyticsDto> response = ApiResponseDto.success(
                        analytics.get(),
                        "User analytics refreshed successfully"
                );
                response.setProcessingTimeMs(processingTime);

                log.info("Successfully refreshed analytics for user: {} in {}ms", username, processingTime);
                return ResponseEntity.ok(response);
            } else {
                ApiResponseDto<UserAnalyticsDto> response = ApiResponseDto.error(
                        "REFRESH_FAILED",
                        "Failed to refresh analytics for user: " + username,
                        "Could not fetch latest data from GitHub or user does not exist"
                );
                response.setProcessingTimeMs(processingTime);

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("Error refreshing user analytics for {}: {}", username, e.getMessage(), e);

            ApiResponseDto<UserAnalyticsDto> response = ApiResponseDto.error(
                    "REFRESH_ERROR",
                    "Error refreshing user analytics",
                    e.getMessage()
            );
            response.setProcessingTimeMs(processingTime);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get user's repositories with basic analytics
     * GET /api/analytics/user/{username}/repositories
     */
    @GetMapping("/user/{username}/repositories")
    public ResponseEntity<ApiResponseDto<List<Repository>>> getUserRepositories(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "starsCount") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        long startTime = System.currentTimeMillis();

        try {
            log.info("Fetching repositories for user: {} (page: {}, size: {}, sort: {} {})",
                    username, page, size, sortBy, sortDir);

            Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ?
                    Sort.Direction.DESC : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

            List<Repository> repositories = repoAnalyticsService.getUserRepositories(username, pageable);

            long processingTime = System.currentTimeMillis() - startTime;

            ApiResponseDto<List<Repository>> response = ApiResponseDto.success(
                    repositories,
                    String.format("Retrieved %d repositories for user %s", repositories.size(), username)
            );
            response.setProcessingTimeMs(processingTime);

            log.info("Successfully retrieved {} repositories for user: {} in {}ms",
                    repositories.size(), username, processingTime);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("Error retrieving repositories for user {}: {}", username, e.getMessage(), e);

            ApiResponseDto<List<Repository>> response = ApiResponseDto.error(
                    "REPOSITORIES_ERROR",
                    "Error retrieving user repositories",
                    e.getMessage()
            );
            response.setProcessingTimeMs(processingTime);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ======================== REPOSITORY ANALYTICS ENDPOINTS ========================

    /**
     * Get comprehensive analytics for a specific repository
     * GET /api/analytics/repository/{owner}/{repoName}
     */
    @GetMapping("/repository/{owner}/{repoName}")
    public ResponseEntity<ApiResponseDto<RepoAnalyticsDto>> getRepositoryAnalytics(
            @PathVariable String owner,
            @PathVariable String repoName) {

        long startTime = System.currentTimeMillis();

        try {
            log.info("Fetching analytics for repository: {}/{}", owner, repoName);

            Optional<RepoAnalyticsDto> analytics = repoAnalyticsService.getRepositoryAnalytics(owner, repoName);

            long processingTime = System.currentTimeMillis() - startTime;

            if (analytics.isPresent()) {
                ApiResponseDto<RepoAnalyticsDto> response = ApiResponseDto.success(
                        analytics.get(),
                        "Repository analytics retrieved successfully"
                );
                response.setProcessingTimeMs(processingTime);

                log.info("Successfully retrieved analytics for repository: {}/{} in {}ms",
                        owner, repoName, processingTime);
                return ResponseEntity.ok(response);
            } else {
                ApiResponseDto<RepoAnalyticsDto> response = ApiResponseDto.error(
                        "REPOSITORY_NOT_FOUND",
                        "Repository not found or analytics could not be generated: " + owner + "/" + repoName,
                        "The specified repository does not exist or could not be analyzed"
                );
                response.setProcessingTimeMs(processingTime);

                log.warn("Repository analytics not found for: {}/{}", owner, repoName);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("Error retrieving repository analytics for {}/{}: {}", owner, repoName, e.getMessage(), e);

            ApiResponseDto<RepoAnalyticsDto> response = ApiResponseDto.error(
                    "ANALYTICS_ERROR",
                    "Error retrieving repository analytics",
                    e.getMessage()
            );
            response.setProcessingTimeMs(processingTime);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Refresh/update repository analytics by fetching latest data from GitHub
     * POST /api/analytics/repository/{owner}/{repoName}/refresh
     */
    @PostMapping("/repository/{owner}/{repoName}/refresh")
    public ResponseEntity<ApiResponseDto<RepoAnalyticsDto>> refreshRepositoryAnalytics(
            @PathVariable String owner,
            @PathVariable String repoName) {

        long startTime = System.currentTimeMillis();

        try {
            log.info("Refreshing analytics for repository: {}/{}", owner, repoName);

            Optional<RepoAnalyticsDto> analytics = repoAnalyticsService.refreshRepositoryAnalytics(owner, repoName);

            long processingTime = System.currentTimeMillis() - startTime;

            if (analytics.isPresent()) {
                ApiResponseDto<RepoAnalyticsDto> response = ApiResponseDto.success(
                        analytics.get(),
                        "Repository analytics refreshed successfully"
                );
                response.setProcessingTimeMs(processingTime);

                log.info("Successfully refreshed analytics for repository: {}/{} in {}ms",
                        owner, repoName, processingTime);
                return ResponseEntity.ok(response);
            } else {
                ApiResponseDto<RepoAnalyticsDto> response = ApiResponseDto.error(
                        "REFRESH_FAILED",
                        "Failed to refresh analytics for repository: " + owner + "/" + repoName,
                        "Could not fetch latest data from GitHub or repository does not exist"
                );
                response.setProcessingTimeMs(processingTime);

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("Error refreshing repository analytics for {}/{}: {}", owner, repoName, e.getMessage(), e);

            ApiResponseDto<RepoAnalyticsDto> response = ApiResponseDto.error(
                    "REFRESH_ERROR",
                    "Error refreshing repository analytics",
                    e.getMessage()
            );
            response.setProcessingTimeMs(processingTime);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get trending repositories based on recent activity and stars
     * GET /api/analytics/repositories/trending
     */
    @GetMapping("/repositories/trending")
    public ResponseEntity<ApiResponseDto<List<Repository>>> getTrendingRepositories(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "5") int minStars,
            @RequestParam(defaultValue = "20") int limit) {

        long startTime = System.currentTimeMillis();

        try {
            log.info("Fetching trending repositories: days={}, minStars={}, limit={}", days, minStars, limit);

            List<Repository> repositories = repoAnalyticsService.getTrendingRepositories(days, minStars, limit);

            long processingTime = System.currentTimeMillis() - startTime;

            ApiResponseDto<List<Repository>> response = ApiResponseDto.success(
                    repositories,
                    String.format("Retrieved %d trending repositories", repositories.size())
            );
            response.setProcessingTimeMs(processingTime);

            log.info("Successfully retrieved {} trending repositories in {}ms", repositories.size(), processingTime);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("Error retrieving trending repositories: {}", e.getMessage(), e);

            ApiResponseDto<List<Repository>> response = ApiResponseDto.error(
                    "TRENDING_ERROR",
                    "Error retrieving trending repositories",
                    e.getMessage()
            );
            response.setProcessingTimeMs(processingTime);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get repositories by programming language with analytics
     * GET /api/analytics/repositories/language/{language}
     */
    @GetMapping("/repositories/language/{language}")
    public ResponseEntity<ApiResponseDto<List<Repository>>> getRepositoriesByLanguage(
            @PathVariable String language,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        long startTime = System.currentTimeMillis();

        try {
            log.info("Fetching repositories by language: {} (page: {}, size: {})", language, page, size);

            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "starsCount"));
            List<Repository> repositories = repoAnalyticsService.getRepositoriesByLanguage(language, pageable);

            long processingTime = System.currentTimeMillis() - startTime;

            ApiResponseDto<List<Repository>> response = ApiResponseDto.success(
                    repositories,
                    String.format("Retrieved %d repositories for language %s", repositories.size(), language)
            );
            response.setProcessingTimeMs(processingTime);

            log.info("Successfully retrieved {} repositories for language {} in {}ms",
                    repositories.size(), language, processingTime);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("Error retrieving repositories by language {}: {}", language, e.getMessage(), e);

            ApiResponseDto<List<Repository>> response = ApiResponseDto.error(
                    "LANGUAGE_REPOSITORIES_ERROR",
                    "Error retrieving repositories by language",
                    e.getMessage()
            );
            response.setProcessingTimeMs(processingTime);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ======================== SEARCH ANALYTICS ENDPOINTS ========================

    /**
     * Get search analytics and statistics
     * GET /api/analytics/search
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponseDto<SearchAnalyticsDto>> getSearchAnalytics(
            @RequestParam(defaultValue = "30") int days) {

        long startTime = System.currentTimeMillis();

        try {
            log.info("Fetching search analytics for last {} days", days);

            SearchAnalyticsDto analytics = searchLogService.getSearchAnalytics(days);

            long processingTime = System.currentTimeMillis() - startTime;

            ApiResponseDto<SearchAnalyticsDto> response = ApiResponseDto.success(
                    analytics,
                    String.format("Retrieved search analytics for last %d days", days)
            );
            response.setProcessingTimeMs(processingTime);

            log.info("Successfully retrieved search analytics in {}ms", processingTime);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("Error retrieving search analytics: {}", e.getMessage(), e);

            ApiResponseDto<SearchAnalyticsDto> response = ApiResponseDto.error(
                    "SEARCH_ANALYTICS_ERROR",
                    "Error retrieving search analytics",
                    e.getMessage()
            );
            response.setProcessingTimeMs(processingTime);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ======================== COMPARATIVE ANALYTICS ENDPOINTS ========================

    /**
     * Compare multiple users' analytics
     * POST /api/analytics/users/compare
     */
    @PostMapping("/users/compare")
    public ResponseEntity<ApiResponseDto<Map<String, UserAnalyticsDto>>> compareUsers(
            @RequestBody List<String> usernames) {

        long startTime = System.currentTimeMillis();

        try {
            log.info("Comparing analytics for users: {}", usernames);

            if (usernames == null || usernames.isEmpty()) {
                ApiResponseDto<Map<String, UserAnalyticsDto>> response = ApiResponseDto.error(
                        "INVALID_INPUT",
                        "No usernames provided for comparison",
                        "Please provide a list of GitHub usernames to compare"
                );
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            if (usernames.size() > 10) {
                ApiResponseDto<Map<String, UserAnalyticsDto>> response = ApiResponseDto.error(
                        "TOO_MANY_USERS",
                        "Too many users for comparison",
                        "Maximum 10 users can be compared at once"
                );
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            Map<String, UserAnalyticsDto> comparisons = userAnalyticsService.compareUsers(usernames);

            long processingTime = System.currentTimeMillis() - startTime;

            ApiResponseDto<Map<String, UserAnalyticsDto>> response = ApiResponseDto.success(
                    comparisons,
                    String.format("Compared %d users successfully", comparisons.size())
            );
            response.setProcessingTimeMs(processingTime);

            log.info("Successfully compared {} users in {}ms", comparisons.size(), processingTime);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("Error comparing users: {}", e.getMessage(), e);

            ApiResponseDto<Map<String, UserAnalyticsDto>> response = ApiResponseDto.error(
                    "COMPARISON_ERROR",
                    "Error comparing users",
                    e.getMessage()
            );
            response.setProcessingTimeMs(processingTime);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Compare multiple repositories' analytics
     * POST /api/analytics/repositories/compare
     */
    @PostMapping("/repositories/compare")
    public ResponseEntity<ApiResponseDto<Map<String, RepoAnalyticsDto>>> compareRepositories(
            @RequestBody List<Map<String, String>> repositories) {

        long startTime = System.currentTimeMillis();

        try {
            log.info("Comparing analytics for {} repositories", repositories.size());

            if (repositories == null || repositories.isEmpty()) {
                ApiResponseDto<Map<String, RepoAnalyticsDto>> response = ApiResponseDto.error(
                        "INVALID_INPUT",
                        "No repositories provided for comparison",
                        "Please provide a list of repositories (owner/repo format) to compare"
                );
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            if (repositories.size() > 10) {
                ApiResponseDto<Map<String, RepoAnalyticsDto>> response = ApiResponseDto.error(
                        "TOO_MANY_REPOSITORIES",
                        "Too many repositories for comparison",
                        "Maximum 10 repositories can be compared at once"
                );
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            Map<String, RepoAnalyticsDto> comparisons = repoAnalyticsService.compareRepositories(repositories);

            long processingTime = System.currentTimeMillis() - startTime;

            ApiResponseDto<Map<String, RepoAnalyticsDto>> response = ApiResponseDto.success(
                    comparisons,
                    String.format("Compared %d repositories successfully", comparisons.size())
            );
            response.setProcessingTimeMs(processingTime);

            log.info("Successfully compared {} repositories in {}ms", comparisons.size(), processingTime);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("Error comparing repositories: {}", e.getMessage(), e);

            ApiResponseDto<Map<String, RepoAnalyticsDto>> response = ApiResponseDto.error(
                    "COMPARISON_ERROR",
                    "Error comparing repositories",
                    e.getMessage()
            );
            response.setProcessingTimeMs(processingTime);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ======================== ANALYTICS SUMMARY ENDPOINTS ========================

    /**
     * Get overall analytics summary/dashboard data
     * GET /api/analytics/dashboard
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> getDashboardAnalytics() {
        long startTime = System.currentTimeMillis();

        try {
            log.info("Fetching dashboard analytics");

            Map<String, Object> dashboard = repoAnalyticsService.getDashboardAnalytics();

            long processingTime = System.currentTimeMillis() - startTime;

            ApiResponseDto<Map<String, Object>> response = ApiResponseDto.success(
                    dashboard,
                    "Dashboard analytics retrieved successfully"
            );
            response.setProcessingTimeMs(processingTime);

            log.info("Successfully retrieved dashboard analytics in {}ms", processingTime);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("Error retrieving dashboard analytics: {}", e.getMessage(), e);

            ApiResponseDto<Map<String, Object>> response = ApiResponseDto.error(
                    "DASHBOARD_ERROR",
                    "Error retrieving dashboard analytics",
                    e.getMessage()
            );
            response.setProcessingTimeMs(processingTime);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get language statistics across all repositories
     * GET /api/analytics/languages/stats
     */
    @GetMapping("/languages/stats")
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> getLanguageStatistics() {
        long startTime = System.currentTimeMillis();

        try {
            log.info("Fetching language statistics");

            Map<String, Object> languageStats = repoAnalyticsService.getLanguageStatistics();

            long processingTime = System.currentTimeMillis() - startTime;

            ApiResponseDto<Map<String, Object>> response = ApiResponseDto.success(
                    languageStats,
                    "Language statistics retrieved successfully"
            );
            response.setProcessingTimeMs(processingTime);

            log.info("Successfully retrieved language statistics in {}ms", processingTime);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("Error retrieving language statistics: {}", e.getMessage(), e);

            ApiResponseDto<Map<String, Object>> response = ApiResponseDto.error(
                    "LANGUAGE_STATS_ERROR",
                    "Error retrieving language statistics",
                    e.getMessage()
            );
            response.setProcessingTimeMs(processingTime);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ======================== UTILITY ENDPOINTS ========================

    /**
     * Health check endpoint for analytics service
     * GET /api/analytics/health
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> healthCheck() {
        long startTime = System.currentTimeMillis();

        try {
            Map<String, Object> health = Map.of(
                    "status", "healthy",
                    "timestamp", LocalDateTime.now(),
                    "services", Map.of(
                            "userAnalyticsService", userAnalyticsService != null ? "available" : "unavailable",
                            "repoAnalyticsService", repoAnalyticsService != null ? "available" : "unavailable",
                            "searchLogService", searchLogService != null ? "available" : "unavailable"
                    )
            );

            long processingTime = System.currentTimeMillis() - startTime;

            ApiResponseDto<Map<String, Object>> response = ApiResponseDto.success(
                    health,
                    "Analytics service is healthy"
            );
            response.setProcessingTimeMs(processingTime);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;

            ApiResponseDto<Map<String, Object>> response = ApiResponseDto.error(
                    "HEALTH_CHECK_ERROR",
                    "Health check failed",
                    e.getMessage()
            );
            response.setProcessingTimeMs(processingTime);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
package com.gitanalyzer.controller;

import com.gitanalyzer.dto.ApiResponseDto;
import com.gitanalyzer.dto.GitHubApiResponse;
import com.gitanalyzer.dto.RepoAnalyticsDto;
import com.gitanalyzer.dto.UserAnalyticsDto;
import com.gitanalyzer.model.Commit;
import com.gitanalyzer.model.Contributor;
import com.gitanalyzer.model.Repository;
import com.gitanalyzer.repository.*;
import com.gitanalyzer.service.GitHubApiService;
import com.gitanalyzer.service.RepoAnalyticsService;
import com.gitanalyzer.service.UserAnalyticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*")
public class TestController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RepoAnalyticsService repoAnalyticsService;

    @Autowired
    private UserAnalyticsService userAnalyticsService;


    @Autowired
    private RepositoryRepository repositoryRepository;

    @Autowired
    private CommitRepository commitRepository;

    @Autowired
    private ContributorRepository contributorRepository;

    @Autowired
    private UrlSearchLogRepository urlSearchLogRepository;

    @Autowired
    private GitHubApiService gitHubApiService;

    @GetMapping("/repositories-status")
    public ResponseEntity<Map<String, Object>> testRepositories() {
        Map<String, Object> status = new HashMap<>();

        try {
            // Test each repository
            long userCount = userRepository.count();
            long repoCount = repositoryRepository.count();
            long commitCount = commitRepository.count();
            long contributorCount = contributorRepository.count();
            long logCount = urlSearchLogRepository.count();

            status.put("success", true);
            status.put("message", "All repositories are working");
            status.put("counts", Map.of(
                    "users", userCount,
                    "repositories", repoCount,
                    "commits", commitCount,
                    "contributors", contributorCount,
                    "searchLogs", logCount
            ));

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            status.put("success", false);
            status.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(status);
        }
    }

    @GetMapping("/dto-test")
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> testDTOs() {
        long startTime = System.currentTimeMillis();

        try {
            // Test UserAnalyticsDto
            UserAnalyticsDto userDto = UserAnalyticsDto.builder()
                    .githubUsername("testuser")
                    .name("Test User")
                    .publicRepos(10)
                    .followers(100)
                    .repositoryStats(UserAnalyticsDto.RepositoryStats.builder()
                            .totalRepositories(10)
                            .totalStars(500)
                            .averageStars(50.0)
                            .mostUsedLanguage("Java")
                            .build())
                    .build();

            // Test RepoAnalyticsDto
            RepoAnalyticsDto repoDto = RepoAnalyticsDto.builder()
                    .repoName("test-repo")
                    .fullName("testuser/test-repo")
                    .language("Java")
                    .starsCount(100)
                    .forksCount(25)
                    .commitAnalytics(RepoAnalyticsDto.CommitAnalytics.builder()
                            .totalCommits(150)
                            .totalAdditions(5000)
                            .averageAdditionsPerCommit(33.33)
                            .build())
                    .build();

            // Test GitHubApiResponse
            GitHubApiResponse.GitHubUser githubUser = GitHubApiResponse.GitHubUser.builder()
                    .login("testuser")
                    .name("Test User")
                    .publicRepos(10)
                    .followers(100)
                    .createdAt(LocalDateTime.now().minusYears(2))
                    .build();

            Map<String, Object> testData = Map.of(
                    "userAnalytics", userDto,
                    "repoAnalytics", repoDto,
                    "githubUser", githubUser,
                    "timestamp", LocalDateTime.now()
            );

            long processingTime = System.currentTimeMillis() - startTime;

            ApiResponseDto<Map<String, Object>> response = ApiResponseDto.success(testData, "DTOs are working correctly");
            response.setProcessingTimeMs(processingTime);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            ApiResponseDto<Map<String, Object>> errorResponse = ApiResponseDto.error("DTO_TEST_ERROR", "Error testing DTOs", e.getMessage());
            errorResponse.setProcessingTimeMs(processingTime);

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/dto-error-test")
    public ResponseEntity<ApiResponseDto<Object>> testErrorDto() {
        ApiResponseDto<Object> errorResponse = ApiResponseDto.error(
                "TEST_ERROR",
                "This is a test error",
                "Testing error DTO structure"
        );
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @GetMapping("/test-connection")
    public ResponseEntity<GitHubApiResponse.ApiResponseWrapper<String>> testConnection() {
        long startTime = System.currentTimeMillis();

        GitHubApiResponse.ApiResponseWrapper<String> result = gitHubApiService.testConnection();

        long processingTime = System.currentTimeMillis() - startTime;
        if (result.getMetadata() != null) {
            result.getMetadata().setProcessingTimeMs(processingTime);
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/rate-limit")
    public ResponseEntity<GitHubApiResponse.ApiResponseWrapper<Object>> checkRateLimit() {
        return ResponseEntity.ok(gitHubApiService.checkRateLimit());
    }

    @GetMapping("/user/{username}")
    public ResponseEntity<ApiResponseDto<GitHubApiResponse.GitHubUser>> getUser(@PathVariable String username) {
        long startTime = System.currentTimeMillis();

        try {
            Optional<GitHubApiResponse.GitHubUser> user = gitHubApiService.getUser(username);

            long processingTime = System.currentTimeMillis() - startTime;

            if (user.isPresent()) {
                ApiResponseDto<GitHubApiResponse.GitHubUser> response = ApiResponseDto.success(
                        user.get(),
                        "User fetched successfully"
                );
                response.setProcessingTimeMs(processingTime);
                return ResponseEntity.ok(response);
            } else {
                ApiResponseDto<GitHubApiResponse.GitHubUser> response = ApiResponseDto.error(
                        "USER_NOT_FOUND",
                        "User not found: " + username,
                        "The specified GitHub user does not exist or is not accessible"
                );
                response.setProcessingTimeMs(processingTime);
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
//            log.error("Error fetching user {}: {}", username, e.getMessage());

            ApiResponseDto<GitHubApiResponse.GitHubUser> response = ApiResponseDto.error(
                    "API_ERROR",
                    "Error fetching user data",
                    e.getMessage()
            );
            response.setProcessingTimeMs(processingTime);
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/user/{username}/repos")
    public ResponseEntity<ApiResponseDto<List<GitHubApiResponse.GitHubRepository>>> getUserRepositories(
            @PathVariable String username) {
        long startTime = System.currentTimeMillis();

        try {
            List<GitHubApiResponse.GitHubRepository> repos = gitHubApiService.getUserRepositories(username);

            long processingTime = System.currentTimeMillis() - startTime;

            ApiResponseDto<List<GitHubApiResponse.GitHubRepository>> response = ApiResponseDto.success(
                    repos,
                    "Repositories fetched successfully. Count: " + repos.size()
            );
            response.setProcessingTimeMs(processingTime);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
//            log.error("Error fetching repositories for user {}: {}", username, e.getMessage());

            ApiResponseDto<List<GitHubApiResponse.GitHubRepository>> response = ApiResponseDto.error(
                    "API_ERROR",
                    "Error fetching repositories",
                    e.getMessage()
            );
            response.setProcessingTimeMs(processingTime);
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/repo/{owner}/{repoName}")
    public ResponseEntity<ApiResponseDto<GitHubApiResponse.GitHubRepository>> getRepository(
            @PathVariable String owner,
            @PathVariable String repoName) {
        long startTime = System.currentTimeMillis();

        try {
            Optional<GitHubApiResponse.GitHubRepository> repo = gitHubApiService.getRepository(owner, repoName);

            long processingTime = System.currentTimeMillis() - startTime;

            if (repo.isPresent()) {
                ApiResponseDto<GitHubApiResponse.GitHubRepository> response = ApiResponseDto.success(
                        repo.get(),
                        "Repository fetched successfully"
                );
                response.setProcessingTimeMs(processingTime);
                return ResponseEntity.ok(response);
            } else {
                ApiResponseDto<GitHubApiResponse.GitHubRepository> response = ApiResponseDto.error(
                        "REPO_NOT_FOUND",
                        "Repository not found: " + owner + "/" + repoName,
                        "The specified repository does not exist or is not accessible"
                );
                response.setProcessingTimeMs(processingTime);
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
//            log.error("Error fetching repository {}/{}: {}", owner, repoName, e.getMessage());

            ApiResponseDto<GitHubApiResponse.GitHubRepository> response = ApiResponseDto.error(
                    "API_ERROR",
                    "Error fetching repository data",
                    e.getMessage()
            );
            response.setProcessingTimeMs(processingTime);
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/repo/{owner}/{repoName}/commits")
    public ResponseEntity<ApiResponseDto<List<GitHubApiResponse.GitHubCommit>>> getRepositoryCommits(
            @PathVariable String owner,
            @PathVariable String repoName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int perPage) {
        long startTime = System.currentTimeMillis();

        try {
            List<GitHubApiResponse.GitHubCommit> commits = gitHubApiService.getRepositoryCommits(owner, repoName, page, perPage);

            long processingTime = System.currentTimeMillis() - startTime;

            ApiResponseDto<List<GitHubApiResponse.GitHubCommit>> response = ApiResponseDto.success(
                    commits,
                    "Commits fetched successfully. Count: " + commits.size()
            );
            response.setProcessingTimeMs(processingTime);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
//            log.error("Error fetching commits for repository {}/{}: {}", owner, repoName, e.getMessage());

            ApiResponseDto<List<GitHubApiResponse.GitHubCommit>> response = ApiResponseDto.error(
                    "API_ERROR",
                    "Error fetching commits",
                    e.getMessage()
            );
            response.setProcessingTimeMs(processingTime);
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/repo/{owner}/{repoName}/contributors")
    public ResponseEntity<ApiResponseDto<List<GitHubApiResponse.GitHubContributor>>> getRepositoryContributors(
            @PathVariable String owner,
            @PathVariable String repoName) {
        long startTime = System.currentTimeMillis();

        try {
            List<GitHubApiResponse.GitHubContributor> contributors = gitHubApiService.getRepositoryContributors(owner, repoName);

            long processingTime = System.currentTimeMillis() - startTime;

            ApiResponseDto<List<GitHubApiResponse.GitHubContributor>> response = ApiResponseDto.success(
                    contributors,
                    "Contributors fetched successfully. Count: " + contributors.size()
            );
            response.setProcessingTimeMs(processingTime);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
//            log.error("Error fetching contributors for repository {}/{}: {}", owner, repoName, e.getMessage());

            ApiResponseDto<List<GitHubApiResponse.GitHubContributor>> response = ApiResponseDto.error(
                    "API_ERROR",
                    "Error fetching contributors",
                    e.getMessage()
            );
            response.setProcessingTimeMs(processingTime);
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/user-analytics/{username}")
    public ResponseEntity<ApiResponseDto<UserAnalyticsDto>> testUserAnalytics(@PathVariable String username) {
        long startTime = System.currentTimeMillis();

        try {
            Optional<UserAnalyticsDto> analytics = userAnalyticsService.getUserAnalytics(username);

            long processingTime = System.currentTimeMillis() - startTime;

            if (analytics.isPresent()) {
                ApiResponseDto<UserAnalyticsDto> response = ApiResponseDto.success(
                        analytics.get(),
                        "User analytics generated successfully"
                );
                response.setProcessingTimeMs(processingTime);
                return ResponseEntity.ok(response);
            } else {
                ApiResponseDto<UserAnalyticsDto> response = ApiResponseDto.error(
                        "USER_ANALYTICS_ERROR",
                        "Failed to generate analytics for user: " + username,
                        "User not found or error occurred during analytics generation"
                );
                response.setProcessingTimeMs(processingTime);
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
//            log.error("Error testing user analytics for {}: {}", username, e.getMessage());

            ApiResponseDto<UserAnalyticsDto> response = ApiResponseDto.error(
                    "ANALYTICS_ERROR",
                    "Error generating user analytics",
                    e.getMessage()
            );
            response.setProcessingTimeMs(processingTime);
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Test user analytics refresh (force update)
     * URL: GET /api/test/user-analytics/{username}/refresh
     */
    @GetMapping("/user-analytics/{username}/refresh")
    public ResponseEntity<ApiResponseDto<UserAnalyticsDto>> testRefreshUserAnalytics(@PathVariable String username) {
        long startTime = System.currentTimeMillis();

        try {
            Optional<UserAnalyticsDto> analytics = userAnalyticsService.refreshUserAnalytics(username);

            long processingTime = System.currentTimeMillis() - startTime;

            if (analytics.isPresent()) {
                ApiResponseDto<UserAnalyticsDto> response = ApiResponseDto.success(
                        analytics.get(),
                        "User analytics refreshed successfully"
                );
                response.setProcessingTimeMs(processingTime);
                return ResponseEntity.ok(response);
            } else {
                ApiResponseDto<UserAnalyticsDto> response = ApiResponseDto.error(
                        "USER_ANALYTICS_REFRESH_ERROR",
                        "Failed to refresh analytics for user: " + username,
                        "User not found or error occurred during analytics refresh"
                );
                response.setProcessingTimeMs(processingTime);
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
//            log.error("Error refreshing user analytics for {}: {}", username, e.getMessage());

            ApiResponseDto<UserAnalyticsDto> response = ApiResponseDto.error(
                    "ANALYTICS_REFRESH_ERROR",
                    "Error refreshing user analytics",
                    e.getMessage()
            );
            response.setProcessingTimeMs(processingTime);
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Test analytics performance with multiple users
     * URL: GET /api/test/analytics-performance
     */
    @GetMapping("/analytics-performance")
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> testAnalyticsPerformance() {
        long startTime = System.currentTimeMillis();

        try {
            // Test with some popular GitHub users
            String[] testUsers = {"octocat", "torvalds", "mojombo", "defunkt", "pjhyett"};
            List<Map<String, Object>> results = new ArrayList<>();

            for (String username : testUsers) {
                long userStartTime = System.currentTimeMillis();

                Optional<UserAnalyticsDto> analytics = userAnalyticsService.getUserAnalytics(username);

                long userProcessingTime = System.currentTimeMillis() - userStartTime;

                Map<String, Object> userResult = new HashMap<>();
                userResult.put("username", username);
                userResult.put("success", analytics.isPresent());
                userResult.put("processingTimeMs", userProcessingTime);

                if (analytics.isPresent()) {
                    UserAnalyticsDto dto = analytics.get();
                    userResult.put("repositoriesCount", dto.getRepositoryStats().getTotalRepositories());
                    userResult.put("totalStars", dto.getRepositoryStats().getTotalStars());
                    userResult.put("followers", dto.getFollowers());
                }

                results.add(userResult);
            }

            long totalProcessingTime = System.currentTimeMillis() - startTime;

            Map<String, Object> response = new HashMap<>();
            response.put("testUsers", testUsers.length);
            response.put("results", results);
            response.put("totalProcessingTimeMs", totalProcessingTime);
            response.put("averageProcessingTimeMs", totalProcessingTime / testUsers.length);

            long successCount = results.stream().mapToLong(r -> (Boolean) r.get("success") ? 1 : 0).sum();
            response.put("successRate", (double) successCount / testUsers.length * 100);

            ApiResponseDto<Map<String, Object>> apiResponse = ApiResponseDto.success(
                    response,
                    "Analytics performance test completed"
            );
            apiResponse.setProcessingTimeMs(totalProcessingTime);

            return ResponseEntity.ok(apiResponse);

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
//            log.error("Error in analytics performance test: {}", e.getMessage());

            ApiResponseDto<Map<String, Object>> response = ApiResponseDto.error(
                    "PERFORMANCE_TEST_ERROR",
                    "Error in analytics performance test",
                    e.getMessage()
            );
            response.setProcessingTimeMs(processingTime);
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/repo-analytics/{owner}/{repoName}")
    public ResponseEntity<?> testGetRepositoryAnalytics(
            @PathVariable String owner,
            @PathVariable String repoName) {
        try {
//            log.info("Testing repository analytics for: {}/{}", owner, repoName);

            Optional<RepoAnalyticsDto> analytics = repoAnalyticsService.getRepositoryAnalytics(owner, repoName);

            if (analytics.isPresent()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Repository analytics retrieved successfully",
                        "data", analytics.get()
                ));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
//            log.error("Error testing repository analytics: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/repo-analytics/{owner}/{repoName}/refresh")
    public ResponseEntity<?> testRefreshRepositoryAnalytics(
            @PathVariable String owner,
            @PathVariable String repoName) {
        try {
//            log.info("Testing refresh repository analytics for: {}/{}", owner, repoName);

            Optional<RepoAnalyticsDto> analytics = repoAnalyticsService.refreshRepositoryAnalytics(owner, repoName);

            if (analytics.isPresent()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Repository analytics refreshed successfully",
                        "data", analytics.get()
                ));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
//            log.error("Error refreshing repository analytics: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

//    @GetMapping("/repositories/user/{username}")
//    public ResponseEntity<?> testGetRepositoriesByUser(@PathVariable String username) {
//        try {
////            log.info("Testing get repositories for user: {}", username);
//
//            List<Repository> repositories = repoAnalyticsService.getRepositoriesByUser(username);
//
//            return ResponseEntity.ok(Map.of(
//                    "success", true,
//                    "message", "Repositories retrieved successfully",
//                    "count", repositories.size(),
//                    "data", repositories
//            ));
//        } catch (Exception e) {
////            log.error("Error getting repositories for user: {}", e.getMessage());
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(Map.of("success", false, "error", e.getMessage()));
//        }
//    }

    @GetMapping("/repositories/trending")
    public ResponseEntity<?> testGetTrendingRepositories(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "5") int minStars) {
        try {
//            log.info("Testing get trending repositories: days={}, minStars={}", days, minStars);

            List<Repository> repositories = repoAnalyticsService.getTrendingRepositories(days, minStars);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Trending repositories retrieved successfully",
                    "count", repositories.size(),
                    "filters", Map.of("days", days, "minStars", minStars),
                    "data", repositories
            ));
        } catch (Exception e) {
//            log.error("Error getting trending repositories: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/repository/{repoId}/commits")
    public ResponseEntity<?> testGetRepositoryCommits(@PathVariable Long repoId) {
        try {
//            log.info("Testing get commits for repository ID: {}", repoId);

            List<Commit> commits = commitRepository.findByRepositoryIdOrderByAuthorDateDesc(repoId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Repository commits retrieved successfully",
                    "repositoryId", repoId,
                    "count", commits.size(),
                    "data", commits
            ));
        } catch (Exception e) {
//            log.error("Error getting repository commits: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/repository/{repoId}/contributors")
    public ResponseEntity<?> testGetRepositoryContributors(@PathVariable Long repoId) {
        try {
//            log.info("Testing get contributors for repository ID: {}", repoId);

            List<Contributor> contributors = contributorRepository.findByRepositoryIdOrderByContributionCountDesc(repoId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Repository contributors retrieved successfully",
                    "repositoryId", repoId,
                    "count", contributors.size(),
                    "data", contributors
            ));
        } catch (Exception e) {
//            log.error("Error getting repository contributors: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/repository/{repoId}/stats")
    public ResponseEntity<?> testGetRepositoryStats(@PathVariable Long repoId) {
        try {
//            log.info("Testing get repository statistics for ID: {}", repoId);

            // Get commit stats
            Object[] commitStats = commitRepository.getRepositoryCommitStats(repoId);

            // Get contributor stats
            Object[] contributorStats = contributorRepository.getRepositoryContributorStats(repoId);

            // Get repository details
            Optional<Repository> repository = repositoryRepository.findById(repoId);

            Map<String, Object> stats = new HashMap<>();

            if (commitStats != null && commitStats.length >= 4) {
                stats.put("commitStats", Map.of(
                        "totalCommits", commitStats[0] != null ? commitStats[0] : 0,
                        "totalAdditions", commitStats[1] != null ? commitStats[1] : 0,
                        "totalDeletions", commitStats[2] != null ? commitStats[2] : 0,
                        "totalChangedFiles", commitStats[3] != null ? commitStats[3] : 0
                ));
            }

            if (contributorStats != null && contributorStats.length >= 3) {
                stats.put("contributorStats", Map.of(
                        "totalContributors", contributorStats[0] != null ? contributorStats[0] : 0,
                        "averageContributions", contributorStats[1] != null ? contributorStats[1] : 0,
                        "totalContributions", contributorStats[2] != null ? contributorStats[2] : 0
                ));
            }

            if (repository.isPresent()) {
                stats.put("repositoryInfo", repository.get());
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Repository statistics retrieved successfully",
                    "repositoryId", repoId,
                    "data", stats
            ));
        } catch (Exception e) {
//            log.error("Error getting repository statistics: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/github/repository/{owner}/{repoName}")
    public ResponseEntity<?> testGitHubRepositoryFetch(
            @PathVariable String owner,
            @PathVariable String repoName) {
        try {
            log.info("Testing GitHub API repository fetch: {}/{}", owner, repoName);

            Optional<GitHubApiResponse.GitHubRepository> githubRepo =
                    gitHubApiService.getRepository(owner, repoName);

            if (githubRepo.isPresent()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "GitHub repository data fetched successfully",
                        "data", githubRepo.get()
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "Repository not found on GitHub"));
            }
        } catch (Exception e) {
            log.error("Error fetching GitHub repository: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/github/commits/{owner}/{repoName}")
    public ResponseEntity<?> testGitHubCommitsFetch(
            @PathVariable String owner,
            @PathVariable String repoName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int perPage) {
        try {
            log.info("Testing GitHub API commits fetch: {}/{} (page: {}, perPage: {})",
                    owner, repoName, page, perPage);

            List<GitHubApiResponse.GitHubCommit> commits =
                    gitHubApiService.getRepositoryCommits(owner, repoName, page, perPage);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "GitHub commits data fetched successfully",
                    "count", commits.size(),
                    "pagination", Map.of("page", page, "perPage", perPage),
                    "data", commits
            ));
        } catch (Exception e) {
            log.error("Error fetching GitHub commits: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/github/contributors/{owner}/{repoName}")
    public ResponseEntity<?> testGitHubContributorsFetch(
            @PathVariable String owner,
            @PathVariable String repoName) {
        try {
            log.info("Testing GitHub API contributors fetch: {}/{}", owner, repoName);

            List<GitHubApiResponse.GitHubContributor> contributors =
                    gitHubApiService.getRepositoryContributors(owner, repoName);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "GitHub contributors data fetched successfully",
                    "count", contributors.size(),
                    "data", contributors
            ));
        } catch (Exception e) {
            log.error("Error fetching GitHub contributors: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/repository/{repoId}/activity-timeline")
    public ResponseEntity<?> testGetRepositoryActivityTimeline(@PathVariable Long repoId) {
        try {
            log.info("Testing get repository activity timeline for ID: {}", repoId);

            LocalDateTime since = LocalDateTime.now().minusDays(30);
            List<Object[]> commitActivity = commitRepository.getCommitActivityByDate(repoId, since);

            List<Map<String, Object>> timeline = commitActivity.stream()
                    .map(row -> Map.of(
                            "date", row[0].toString(),
                            "commits", row[1]
                    ))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Repository activity timeline retrieved successfully",
                    "repositoryId", repoId,
                    "period", "Last 30 days",
                    "data", timeline
            ));
        } catch (Exception e) {
            log.error("Error getting repository activity timeline: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/repository/{repoId}/top-contributors")
    public ResponseEntity<?> testGetTopContributors(@PathVariable Long repoId) {
        try {
            log.info("Testing get top contributors for repository ID: {}", repoId);

            List<Object[]> topContributors = commitRepository.getMostActiveContributors(repoId);

            List<Map<String, Object>> contributors = topContributors.stream()
                    .map(row -> Map.of(
                            "authorName", row[0],
                            "commitCount", row[1]
                    ))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Top contributors retrieved successfully",
                    "repositoryId", repoId,
                    "data", contributors
            ));
        } catch (Exception e) {
            log.error("Error getting top contributors: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

}
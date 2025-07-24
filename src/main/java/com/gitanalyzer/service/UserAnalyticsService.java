package com.gitanalyzer.service;

import com.gitanalyzer.dto.GitHubApiResponse;
import com.gitanalyzer.dto.UserAnalyticsDto;
import com.gitanalyzer.model.User;
import com.gitanalyzer.model.Repository;
import com.gitanalyzer.model.Commit;
import com.gitanalyzer.repository.UserRepository;
import com.gitanalyzer.repository.RepositoryRepository;
import com.gitanalyzer.repository.CommitRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserAnalyticsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RepositoryRepository repositoryRepository;

    @Autowired
    private CommitRepository commitRepository;

    @Autowired
    private GitHubApiService gitHubApiService;

    /**
     * Get comprehensive user analytics
     */
    @Transactional
    public Optional<UserAnalyticsDto> getUserAnalytics(String username) {
        try {
            log.info("Generating analytics for user: {}", username);

            // First, fetch and save user data if not exists or outdated
            User user = fetchAndSaveUserData(username);
            if (user == null) {
                log.warn("User not found: {}", username);
                return Optional.empty();
            }

            // Fetch user repositories
            List<Repository> repositories = repositoryRepository.findByUserGithubUsernameOrderByStarsCountDesc(username);

            // Build analytics DTO
            UserAnalyticsDto analytics = buildUserAnalytics(user, repositories);

            log.info("Successfully generated analytics for user: {}", username);
            return Optional.of(analytics);

        } catch (Exception e) {
            log.error("Error generating analytics for user {}: {}", username, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Fetch and save user data from GitHub API
     */
    private User fetchAndSaveUserData(String username) {
        try {
            // Check if user exists and is recent (within 24 hours)
            Optional<User> existingUser = userRepository.findByGithubUsername(username);
            if (existingUser.isPresent() &&
                    existingUser.get().getUpdatedAt().isAfter(LocalDateTime.now().minusHours(24))) {
                return existingUser.get();
            }

            // Fetch from GitHub API
            Optional<GitHubApiResponse.GitHubUser> githubUser = gitHubApiService.getUser(username);
            if (githubUser.isEmpty()) {
                return null;
            }

            // Convert and save
            User user = gitHubApiService.convertToUserEntity(githubUser.get());
            if (existingUser.isPresent()) {
                user.setId(existingUser.get().getId()); // 🔑 Set ID to trigger update instead of insert
            }
            user = userRepository.save(user);

            // Fetch and save repositories
            fetchAndSaveUserRepositories(user);

            return user;

        } catch (Exception e) {
            log.error("Error fetching user data for {}: {}", username, e.getMessage());
            return null;
        }
    }

    /**
     * Fetch and save user repositories
     */
    private void fetchAndSaveUserRepositories(User user) {
        try {
            List<GitHubApiResponse.GitHubRepository> githubRepos =
                    gitHubApiService.getUserRepositories(user.getGithubUsername());

            for (GitHubApiResponse.GitHubRepository githubRepo : githubRepos) {
                // Check if repository already exists
                Optional<Repository> existingRepo = repositoryRepository
                        .findByUserGithubUsernameAndRepoName(user.getGithubUsername(), githubRepo.getName());

                if (existingRepo.isEmpty() ||
                        existingRepo.get().getAnalyzedAt().isBefore(LocalDateTime.now().minusHours(6))) {

                    Repository repository = gitHubApiService.convertToRepositoryEntity(githubRepo, user);
                    repository = repositoryRepository.save(repository);

                    // Fetch some recent commits for analytics
                    fetchRecentCommits(repository);
                }
            }

        } catch (Exception e) {
            log.error("Error fetching repositories for user {}: {}", user.getGithubUsername(), e.getMessage());
        }
    }

    /**
     * Fetch recent commits for a repository
     */
    private void fetchRecentCommits(Repository repository) {
        try {
            String[] parts = repository.getFullName().split("/");
            if (parts.length != 2) return;

            List<GitHubApiResponse.GitHubCommit> githubCommits =
                    gitHubApiService.getRepositoryCommits(parts[0], parts[1], 1, 30);

            for (GitHubApiResponse.GitHubCommit githubCommit : githubCommits) {
                // Only save if not already exists
                if (!commitRepository.existsByCommitSha(githubCommit.getSha())) {
                    Commit commit = gitHubApiService.convertToCommitEntity(githubCommit, repository);
                    commitRepository.save(commit);
                }
            }

        } catch (Exception e) {
            log.error("Error fetching commits for repository {}: {}", repository.getFullName(), e.getMessage());
        }
    }

    /**
     * Build user analytics DTO
     */
    private UserAnalyticsDto buildUserAnalytics(User user, List<Repository> repositories) {
        return UserAnalyticsDto.builder()
                .githubUsername(user.getGithubUsername())
                .name(user.getName())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .location(user.getLocation())
                .company(user.getCompany())
                .email(user.getEmail())
                .publicRepos(user.getPublicRepos())
                .followers(user.getFollowers())
                .following(user.getFollowing())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .repositoryStats(buildRepositoryStats(repositories))
                .contributionStats(buildContributionStats(user.getGithubUsername()))
                .activityStats(buildActivityStats(user.getGithubUsername()))
                .topRepositories(buildTopRepositories(repositories))
                .languageBreakdown(buildLanguageBreakdown(repositories))
                .build();
    }

    /**
     * Build repository statistics
     */
    private UserAnalyticsDto.RepositoryStats buildRepositoryStats(List<Repository> repositories) {
        if (repositories.isEmpty()) {
            return UserAnalyticsDto.RepositoryStats.builder()
                    .totalRepositories(0)
                    .totalStars(0)
                    .totalForks(0)
                    .totalWatchers(0)
                    .totalSizeKb(0L)
                    .averageStars(0.0)
                    .mostUsedLanguage("N/A")
                    .build();
        }

        int totalStars = repositories.stream().mapToInt(repo -> repo.getStarsCount() != null ? repo.getStarsCount() : 0).sum();
        int totalForks = repositories.stream().mapToInt(repo -> repo.getForksCount() != null ? repo.getForksCount() : 0).sum();
        int totalWatchers = repositories.stream().mapToInt(repo -> repo.getWatchersCount() != null ? repo.getWatchersCount() : 0).sum();
        long totalSize = repositories.stream().mapToLong(repo -> repo.getSizeKb() != null ? repo.getSizeKb() : 0).sum();

        double averageStars = repositories.isEmpty() ? 0.0 : (double) totalStars / repositories.size();

        // Find most used language
        String mostUsedLanguage = repositories.stream()
                .filter(repo -> repo.getLanguage() != null)
                .collect(Collectors.groupingBy(Repository::getLanguage, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        return UserAnalyticsDto.RepositoryStats.builder()
                .totalRepositories(repositories.size())
                .totalStars(totalStars)
                .totalForks(totalForks)
                .totalWatchers(totalWatchers)
                .totalSizeKb(totalSize)
                .averageStars(averageStars)
                .mostUsedLanguage(mostUsedLanguage)
                .build();
    }

    /**
     * Build contribution statistics
     */
    private UserAnalyticsDto.ContributionStats buildContributionStats(String username) {
        try {
            List<Commit> userCommits = commitRepository.findByRepository_User_GithubUsernameOrderByCommitterDateDesc(username);

            if (userCommits.isEmpty()) {
                return UserAnalyticsDto.ContributionStats.builder()
                        .totalCommits(0)
                        .totalAdditions(0)
                        .totalDeletions(0)
                        .totalChangedFiles(0)
                        .averageCommitsPerRepo(0.0)
                        .mostActiveRepository("N/A")
                        .build();
            }

            int totalCommits = userCommits.size();
            int totalAdditions = userCommits.stream().mapToInt(commit -> commit.getAdditions() != null ? commit.getAdditions() : 0).sum();
            int totalDeletions = userCommits.stream().mapToInt(commit -> commit.getDeletions() != null ? commit.getDeletions() : 0).sum();
            int totalChangedFiles = userCommits.stream().mapToInt(commit -> commit.getChangedFiles() != null ? commit.getChangedFiles() : 0).sum();

            // Calculate average commits per repository
            long uniqueRepos = userCommits.stream()
                    .map(commit -> commit.getRepository().getId())
                    .distinct()
                    .count();
            double averageCommitsPerRepo = uniqueRepos > 0 ? (double) totalCommits / uniqueRepos : 0.0;

            // Find most active repository
            String mostActiveRepository = userCommits.stream()
                    .collect(Collectors.groupingBy(commit -> commit.getRepository().getRepoName(), Collectors.counting()))
                    .entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("N/A");

            return UserAnalyticsDto.ContributionStats.builder()
                    .totalCommits(totalCommits)
                    .totalAdditions(totalAdditions)
                    .totalDeletions(totalDeletions)
                    .totalChangedFiles(totalChangedFiles)
                    .averageCommitsPerRepo(averageCommitsPerRepo)
                    .mostActiveRepository(mostActiveRepository)
                    .build();

        } catch (Exception e) {
            log.error("Error building contribution stats for {}: {}", username, e.getMessage());
            return UserAnalyticsDto.ContributionStats.builder().build();
        }
    }

    /**
     * Build activity statistics
     */
    private UserAnalyticsDto.ActivityStats buildActivityStats(String username) {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime lastMonth = now.minusMonths(1);
            LocalDateTime lastWeek = now.minusWeeks(1);

            List<Commit> recentCommits = commitRepository.findByRepository_User_GithubUsernameAndCommitterDateAfterOrderByCommitterDateDesc(username, lastMonth);

            if (recentCommits.isEmpty()) {
                return UserAnalyticsDto.ActivityStats.builder()
                        .lastActivity(null)
                        .commitsLastMonth(0)
                        .commitsLastWeek(0)
                        .dailyActivity(new ArrayList<>())
                        .hourlyActivity(new HashMap<>())
                        .build();
            }

            // Last activity
            LocalDateTime lastActivity = recentCommits.stream()
                    .map(Commit::getCommitterDate)
                    .filter(Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);

            // Commits last month and week
            int commitsLastMonth = recentCommits.size();
            int commitsLastWeek = (int) recentCommits.stream()
                    .filter(commit -> commit.getCommitterDate() != null && commit.getCommitterDate().isAfter(lastWeek))
                    .count();

            // Daily activity (last 30 days)
            List<UserAnalyticsDto.DailyActivityDto> dailyActivity = buildDailyActivity(recentCommits);

            // Hourly activity distribution
            Map<Integer, Integer> hourlyActivity = recentCommits.stream()
                    .filter(commit -> commit.getCommitterDate() != null)
                    .collect(Collectors.groupingBy(
                            commit -> commit.getCommitterDate().getHour(),
                            Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
                    ));

            return UserAnalyticsDto.ActivityStats.builder()
                    .lastActivity(lastActivity)
                    .commitsLastMonth(commitsLastMonth)
                    .commitsLastWeek(commitsLastWeek)
                    .dailyActivity(dailyActivity)
                    .hourlyActivity(hourlyActivity)
                    .build();

        } catch (Exception e) {
            log.error("Error building activity stats for {}: {}", username, e.getMessage());
            return UserAnalyticsDto.ActivityStats.builder().build();
        }
    }

    /**
     * Build daily activity data
     */
    private List<UserAnalyticsDto.DailyActivityDto> buildDailyActivity(List<Commit> commits) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        return commits.stream()
                .filter(commit -> commit.getCommitterDate() != null)
                .collect(Collectors.groupingBy(
                        commit -> commit.getCommitterDate().toLocalDate().format(formatter),
                        Collectors.toList()
                ))
                .entrySet().stream()
                .map(entry -> {
                    List<Commit> dayCommits = entry.getValue();
                    int additions = dayCommits.stream().mapToInt(c -> c.getAdditions() != null ? c.getAdditions() : 0).sum();
                    int deletions = dayCommits.stream().mapToInt(c -> c.getDeletions() != null ? c.getDeletions() : 0).sum();

                    return UserAnalyticsDto.DailyActivityDto.builder()
                            .date(entry.getKey())
                            .commits(dayCommits.size())
                            .additions(additions)
                            .deletions(deletions)
                            .build();
                })
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .collect(Collectors.toList());
    }

    /**
     * Build top repositories list
     */
    private List<UserAnalyticsDto.TopRepositoryDto> buildTopRepositories(List<Repository> repositories) {
        return repositories.stream()
                .sorted((a, b) -> Integer.compare(
                        b.getStarsCount() != null ? b.getStarsCount() : 0,
                        a.getStarsCount() != null ? a.getStarsCount() : 0
                ))
                .limit(10)
                .map(repo -> {
                    int commitsCount = commitRepository.countByRepositoryId(repo.getId());

                    return UserAnalyticsDto.TopRepositoryDto.builder()
                            .repoName(repo.getRepoName())
                            .fullName(repo.getFullName())
                            .description(repo.getDescription())
                            .language(repo.getLanguage())
                            .starsCount(repo.getStarsCount())
                            .forksCount(repo.getForksCount())
                            .commitsCount(commitsCount)
                            .lastPushAt(repo.getLastPushAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Build language breakdown
     */
    private List<UserAnalyticsDto.LanguageStatsDto> buildLanguageBreakdown(List<Repository> repositories) {
        if (repositories.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, List<Repository>> languageGroups = repositories.stream()
                .filter(repo -> repo.getLanguage() != null)
                .collect(Collectors.groupingBy(Repository::getLanguage));

        int totalRepos = repositories.size();

        return languageGroups.entrySet().stream()
                .map(entry -> {
                    String language = entry.getKey();
                    List<Repository> langRepos = entry.getValue();
                    int totalStars = langRepos.stream().mapToInt(repo -> repo.getStarsCount() != null ? repo.getStarsCount() : 0).sum();
                    double percentage = (double) langRepos.size() / totalRepos * 100;

                    return UserAnalyticsDto.LanguageStatsDto.builder()
                            .language(language)
                            .repositoryCount(langRepos.size())
                            .totalStars(totalStars)
                            .percentage(Math.round(percentage * 100.0) / 100.0)
                            .build();
                })
                .sorted((a, b) -> Integer.compare(b.getRepositoryCount(), a.getRepositoryCount()))
                .collect(Collectors.toList());
    }

    /**
     * Refresh user analytics (force update from GitHub)
     */
    @Transactional
    public Optional<UserAnalyticsDto> refreshUserAnalytics(String username) {
        try {
            log.info("Refreshing analytics for user: {}", username);

            // Delete existing data to force refresh
            Optional<User> existingUser = userRepository.findByGithubUsername(username);
            if (existingUser.isPresent()) {
                // Update the user's updated_at to force refresh
                User user = existingUser.get();
                user.setUpdatedAt(LocalDateTime.now().minusDays(2)); // Force it to be old
                userRepository.save(user);
            }

            return getUserAnalytics(username);

        } catch (Exception e) {
            log.error("Error refreshing analytics for user {}: {}", username, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Compare multiple users
     */
    public Map<String, UserAnalyticsDto> compareUsers(List<String> usernames) {
        Map<String, UserAnalyticsDto> comparisons = new HashMap<>();

        for (String username : usernames) {
            Optional<UserAnalyticsDto> analytics = getUserAnalytics(username);
            if (analytics.isPresent()) {
                comparisons.put(username, analytics.get());
            }
        }

        return comparisons;
    }
}
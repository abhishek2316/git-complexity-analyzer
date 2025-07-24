package com.gitanalyzer.service;

import com.gitanalyzer.dto.GitHubApiResponse;
import com.gitanalyzer.dto.RepoAnalyticsDto;
import com.gitanalyzer.model.Repository;
import com.gitanalyzer.model.Commit;
import com.gitanalyzer.model.Contributor;
import com.gitanalyzer.model.User;
import com.gitanalyzer.repository.RepositoryRepository;
import com.gitanalyzer.repository.CommitRepository;
import com.gitanalyzer.repository.ContributorRepository;
import com.gitanalyzer.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;
//import java.awt.print.Pageable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RepoAnalyticsService {

    @Autowired
    private RepositoryRepository repositoryRepository;

    @Autowired
    private CommitRepository commitRepository;

    @Autowired
    private ContributorRepository contributorRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GitHubApiService gitHubApiService;

    /**
     * Get comprehensive repository analytics
     */
    @Transactional
    public Optional<RepoAnalyticsDto> getRepositoryAnalytics(String owner, String repoName) {
        try {
            log.info("Generating analytics for repository: {}/{}", owner, repoName);

            // Fetch and save repository data if not exists or outdated
            Repository repository = fetchAndSaveRepositoryData(owner, repoName);
            if (repository == null) {
                log.warn("Repository not found: {}/{}", owner, repoName);
                return Optional.empty();
            }

            // Build analytics DTO
            RepoAnalyticsDto analytics = buildRepositoryAnalytics(repository);

            log.info("Successfully generated analytics for repository: {}/{}", owner, repoName);
            return Optional.of(analytics);

        } catch (Exception e) {
            log.error("Error generating analytics for repository {}/{}: {}", owner, repoName, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Fetch and save repository data from GitHub API
     */
    private Repository fetchAndSaveRepositoryData(String owner, String repoName) {
        try {
            // Check if repository exists and is recent (within 6 hours)
            Optional<Repository> existingRepo = repositoryRepository
                    .findByUserGithubUsernameAndRepoName(owner, repoName);

            if (existingRepo.isPresent() &&
                    existingRepo.get().getAnalyzedAt() != null &&
                    existingRepo.get().getAnalyzedAt().isAfter(LocalDateTime.now().minusHours(6))) {
                return existingRepo.get();
            }

            // Ensure user exists
            User user = ensureUserExists(owner);
            if (user == null) {
                return null;
            }

            // Fetch repository from GitHub API
            Optional<GitHubApiResponse.GitHubRepository> githubRepo =
                    gitHubApiService.getRepository(owner, repoName);

            if (githubRepo.isEmpty()) {
                return null;
            }

            // Convert and save repository
            Repository repository = gitHubApiService.convertToRepositoryEntity(githubRepo.get(), user);
            repository = repositoryRepository.save(repository);

            // Fetch detailed data
            fetchRepositoryCommits(repository);
            fetchRepositoryContributors(repository);

            return repository;

        } catch (Exception e) {
            log.error("Error fetching repository data for {}/{}: {}", owner, repoName, e.getMessage());
            return null;
        }
    }

    /**
     * Ensure user exists in database
     */
    private User ensureUserExists(String username) {
        try {
            Optional<User> existingUser = userRepository.findByGithubUsername(username);
            if (existingUser.isPresent()) {
                return existingUser.get();
            }

            // Fetch user from GitHub API
            Optional<GitHubApiResponse.GitHubUser> githubUser = gitHubApiService.getUser(username);
            if (githubUser.isEmpty()) {
                return null;
            }

            // Convert and save user
            User user = gitHubApiService.convertToUserEntity(githubUser.get());
            return userRepository.save(user);

        } catch (Exception e) {
            log.error("Error ensuring user exists {}: {}", username, e.getMessage());
            return null;
        }
    }

    /**
     * Fetch repository commits
     */
    private void fetchRepositoryCommits(Repository repository) {
        try {
            String[] parts = repository.getFullName().split("/");
            if (parts.length != 2) return;

            // Fetch commits in batches
            int page = 1;
            int perPage = 100;
            boolean hasMore = true;

            while (hasMore && page <= 5) { // Limit to 5 pages (500 commits max)
                List<GitHubApiResponse.GitHubCommit> githubCommits =
                        gitHubApiService.getRepositoryCommits(parts[0], parts[1], page, perPage);

                if (githubCommits.isEmpty()) {
                    hasMore = false;
                } else {
                    for (GitHubApiResponse.GitHubCommit githubCommit : githubCommits) {
                        // Only save if not already exists
                        if (!commitRepository.existsByCommitSha(githubCommit.getSha())) {
                            Commit commit = gitHubApiService.convertToCommitEntity(githubCommit, repository);
                            commitRepository.save(commit);
                        }
                    }
                    page++;
                    hasMore = githubCommits.size() == perPage;
                }
            }

        } catch (Exception e) {
            log.error("Error fetching commits for repository {}: {}", repository.getFullName(), e.getMessage());
        }
    }

    /**
     * Fetch repository contributors
     */
    private void fetchRepositoryContributors(Repository repository) {
        try {
            String[] parts = repository.getFullName().split("/");
            if (parts.length != 2) return;

            List<GitHubApiResponse.GitHubContributor> githubContributors =
                    gitHubApiService.getRepositoryContributors(parts[0], parts[1]);

            for (GitHubApiResponse.GitHubContributor githubContributor : githubContributors) {
                // Check if contributor already exists
                if (!contributorRepository.existsByRepositoryIdAndContributorName(
                        repository.getId(), githubContributor.getLogin())) {

                    Contributor contributor = gitHubApiService.convertToContributorEntity(githubContributor, repository);
                    contributorRepository.save(contributor);
                }
            }

        } catch (Exception e) {
            log.error("Error fetching contributors for repository {}: {}", repository.getFullName(), e.getMessage());
        }
    }

    /**
     * Build repository analytics DTO
     */
    private RepoAnalyticsDto buildRepositoryAnalytics(Repository repository) {
        return RepoAnalyticsDto.builder()
                .repoName(repository.getRepoName())
                .fullName(repository.getFullName())
                .description(repository.getDescription())
                .language(repository.getLanguage())
                .defaultBranch(repository.getDefaultBranch())
                .isPrivate(repository.getIsPrivate())
                .starsCount(repository.getStarsCount())
                .forksCount(repository.getForksCount())
                .watchersCount(repository.getWatchersCount())
                .sizeKb(repository.getSizeKb())
                .createdAt(repository.getCreatedAt())
                .updatedAt(repository.getUpdatedAt())
                .lastPushAt(repository.getLastPushAt())
                .analyzedAt(repository.getAnalyzedAt())
                .owner(buildOwnerDto(repository.getUser()))
                .commitAnalytics(buildCommitAnalytics(repository))
                .contributorAnalytics(buildContributorAnalytics(repository))
                .codeAnalytics(buildCodeAnalytics(repository))
                .activityAnalytics(buildActivityAnalytics(repository))
                .build();
    }

    /**
     * Build owner DTO
     */
    private RepoAnalyticsDto.OwnerDto buildOwnerDto(User user) {
        return RepoAnalyticsDto.OwnerDto.builder()
                .githubUsername(user.getGithubUsername())
                .name(user.getName())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    /**
     * Build commit analytics
     */
    private RepoAnalyticsDto.CommitAnalytics buildCommitAnalytics(Repository repository) {
        try {
            List<Commit> commits = commitRepository.findByRepositoryIdOrderByAuthorDateDesc(repository.getId());

            if (commits.isEmpty()) {
                return RepoAnalyticsDto.CommitAnalytics.builder()
                        .totalCommits(0)
                        .totalAdditions(0)
                        .totalDeletions(0)
                        .totalChangedFiles(0)
                        .averageAdditionsPerCommit(0.0)
                        .averageDeletionsPerCommit(0.0)
                        .averageFilesChangedPerCommit(0.0)
                        .commitTimeline(new ArrayList<>())
                        .commitsByHour(new HashMap<>())
                        .topCommits(new ArrayList<>())
                        .build();
            }

            int totalCommits = commits.size();
            int totalAdditions = commits.stream().mapToInt(c -> c.getAdditions() != null ? c.getAdditions() : 0).sum();
            int totalDeletions = commits.stream().mapToInt(c -> c.getDeletions() != null ? c.getDeletions() : 0).sum();
            int totalChangedFiles = commits.stream().mapToInt(c -> c.getChangedFiles() != null ? c.getChangedFiles() : 0).sum();

            double avgAdditions = totalCommits > 0 ? (double) totalAdditions / totalCommits : 0.0;
            double avgDeletions = totalCommits > 0 ? (double) totalDeletions / totalCommits : 0.0;
            double avgFiles = totalCommits > 0 ? (double) totalChangedFiles / totalCommits : 0.0;

            // First and last commit dates
            LocalDateTime firstCommit = commits.stream()
                    .map(Commit::getAuthorDate)
                    .filter(Objects::nonNull)
                    .min(LocalDateTime::compareTo)
                    .orElse(null);

            LocalDateTime lastCommit = commits.stream()
                    .map(Commit::getAuthorDate)
                    .filter(Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);

            // Build timeline
            List<RepoAnalyticsDto.CommitTimelineDto> timeline = buildCommitTimeline(commits);

            // Commits by hour
            Map<Integer, Integer> commitsByHour = commits.stream()
                    .filter(c -> c.getAuthorDate() != null)
                    .collect(Collectors.groupingBy(
                            c -> c.getAuthorDate().getHour(),
                            Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
                    ));

            // Top commits (by impact)
            List<RepoAnalyticsDto.TopCommitDto> topCommits = buildTopCommits(commits);

            return RepoAnalyticsDto.CommitAnalytics.builder()
                    .totalCommits(totalCommits)
                    .totalAdditions(totalAdditions)
                    .totalDeletions(totalDeletions)
                    .totalChangedFiles(totalChangedFiles)
                    .averageAdditionsPerCommit(Math.round(avgAdditions * 100.0) / 100.0)
                    .averageDeletionsPerCommit(Math.round(avgDeletions * 100.0) / 100.0)
                    .averageFilesChangedPerCommit(Math.round(avgFiles * 100.0) / 100.0)
                    .firstCommit(firstCommit)
                    .lastCommit(lastCommit)
                    .commitTimeline(timeline)
                    .commitsByHour(commitsByHour)
                    .topCommits(topCommits)
                    .build();

        } catch (Exception e) {
            log.error("Error building commit analytics for repository {}: {}", repository.getFullName(), e.getMessage());
            return RepoAnalyticsDto.CommitAnalytics.builder().build();
        }
    }

    /**
     * Build contributor analytics
     */
    private RepoAnalyticsDto.ContributorAnalytics buildContributorAnalytics(Repository repository) {
        try {
            List<Contributor> contributors = contributorRepository.findByRepositoryIdOrderByContributionCountDesc(repository.getId());

            if (contributors.isEmpty()) {
                return RepoAnalyticsDto.ContributorAnalytics.builder()
                        .totalContributors(0)
                        .activeContributors(0)
                        .topContributors(new ArrayList<>())
                        .contributorTimeline(new ArrayList<>())
                        .contributionDistribution(new HashMap<>())
                        .build();
            }

            int totalContributors = contributors.size();

            // Active contributors (those with activity in last 3 months)
            LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
            int activeContributors = (int) contributors.stream()
                    .filter(c -> c.getLastContributionDate() != null && c.getLastContributionDate().isAfter(threeMonthsAgo))
                    .count();

            // Top contributors
            List<RepoAnalyticsDto.TopContributorDto> topContributors = contributors.stream()
                    .limit(10)
                    .map(this::buildTopContributorDto)
                    .collect(Collectors.toList());

            // Contribution distribution
            Map<String, Integer> contributionDistribution = contributors.stream()
                    .collect(Collectors.toMap(
                            Contributor::getContributorName,
                            c -> c.getContributionCount() != null ? c.getContributionCount() : 0
                    ));

            // Contributor timeline
            List<RepoAnalyticsDto.ContributorTimelineDto> timeline = buildContributorTimeline(contributors);

            return RepoAnalyticsDto.ContributorAnalytics.builder()
                    .totalContributors(totalContributors)
                    .activeContributors(activeContributors)
                    .topContributors(topContributors)
                    .contributorTimeline(timeline)
                    .contributionDistribution(contributionDistribution)
                    .build();

        } catch (Exception e) {
            log.error("Error building contributor analytics for repository {}: {}", repository.getFullName(), e.getMessage());
            return RepoAnalyticsDto.ContributorAnalytics.builder().build();
        }
    }

    /**
     * Build code analytics
     */
    private RepoAnalyticsDto.CodeAnalytics buildCodeAnalytics(Repository repository) {
        try {
            List<Commit> commits = commitRepository.findByRepositoryIdOrderByAuthorDateDesc(repository.getId());

            if (commits.isEmpty()) {
                return RepoAnalyticsDto.CodeAnalytics.builder()
                        .totalLines(0)
                        .codeChurnRate(0.0)
                        .mainFileTypes(new ArrayList<>())
                        .fileTypeDistribution(new HashMap<>())
                        .averageCommitSize(0)
                        .build();
            }

            int totalAdditions = commits.stream().mapToInt(c -> c.getAdditions() != null ? c.getAdditions() : 0).sum();
            int totalDeletions = commits.stream().mapToInt(c -> c.getDeletions() != null ? c.getDeletions() : 0).sum();
            int totalLines = totalAdditions - totalDeletions;

            // Code churn rate
            double codeChurnRate = commits.size() > 0 ? (double) (totalAdditions + totalDeletions) / commits.size() : 0.0;

            // Average commit size
            int averageCommitSize = commits.size() > 0 ? (totalAdditions + totalDeletions) / commits.size() : 0;

            // For file types, we'd need additional data from GitHub API
            // For now, we'll use the repository language
            List<String> mainFileTypes = new ArrayList<>();
            Map<String, Integer> fileTypeDistribution = new HashMap<>();

            if (repository.getLanguage() != null) {
                mainFileTypes.add(repository.getLanguage());
                fileTypeDistribution.put(repository.getLanguage(), 100);
            }

            return RepoAnalyticsDto.CodeAnalytics.builder()
                    .totalLines(totalLines)
                    .codeChurnRate(Math.round(codeChurnRate * 100.0) / 100.0)
                    .mainFileTypes(mainFileTypes)
                    .fileTypeDistribution(fileTypeDistribution)
                    .averageCommitSize(averageCommitSize)
                    .build();

        } catch (Exception e) {
            log.error("Error building code analytics for repository {}: {}", repository.getFullName(), e.getMessage());
            return RepoAnalyticsDto.CodeAnalytics.builder().build();
        }
    }

    /**
     * Build activity analytics
     */
    private RepoAnalyticsDto.ActivityAnalytics buildActivityAnalytics(Repository repository) {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime lastWeek = now.minusWeeks(1);
            LocalDateTime lastMonth = now.minusMonths(1);
            LocalDateTime lastYear = now.minusYears(1);

            List<Commit> allCommits = commitRepository.findByRepositoryIdOrderByAuthorDateDesc(repository.getId());

            // Check if repository is active (has commits in last 30 days)
            boolean isActive = allCommits.stream()
                    .anyMatch(c -> c.getAuthorDate() != null && c.getAuthorDate().isAfter(lastMonth));

            // Count commits in different time periods
            int commitsLastWeek = (int) allCommits.stream()
                    .filter(c -> c.getAuthorDate() != null && c.getAuthorDate().isAfter(lastWeek))
                    .count();

            int commitsLastMonth = (int) allCommits.stream()
                    .filter(c -> c.getAuthorDate() != null && c.getAuthorDate().isAfter(lastMonth))
                    .count();

            int commitsLastYear = (int) allCommits.stream()
                    .filter(c -> c.getAuthorDate() != null && c.getAuthorDate().isAfter(lastYear))
                    .count();

            // Calculate averages
            double weeklyAverageCommits = commitsLastYear / 52.0;
            double monthlyAverageCommits = commitsLastYear / 12.0;

            // Busiest days and hours
            List<String> busiestDays = getBusiestDaysOfWeek(allCommits);
            List<String> busiestHours = getBusiestHours(allCommits);

            return RepoAnalyticsDto.ActivityAnalytics.builder()
                    .isActive(isActive)
                    .commitsLastWeek(commitsLastWeek)
                    .commitsLastMonth(commitsLastMonth)
                    .commitsLastYear(commitsLastYear)
                    .weeklyAverageCommits(Math.round(weeklyAverageCommits * 100.0) / 100.0)
                    .monthlyAverageCommits(Math.round(monthlyAverageCommits * 100.0) / 100.0)
                    .busiestDays(busiestDays)
                    .busiestHours(busiestHours)
                    .build();

        } catch (Exception e) {
            log.error("Error building activity analytics for repository {}: {}", repository.getFullName(), e.getMessage());
            return RepoAnalyticsDto.ActivityAnalytics.builder().build();
        }
    }

    /**
     * Build commit timeline
     */
    private List<RepoAnalyticsDto.CommitTimelineDto> buildCommitTimeline(List<Commit> commits) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        return commits.stream()
                .filter(c -> c.getAuthorDate() != null)
                .collect(Collectors.groupingBy(
                        c -> c.getAuthorDate().toLocalDate().format(formatter),
                        Collectors.toList()
                ))
                .entrySet().stream()
                .map(entry -> {
                    List<Commit> dayCommits = entry.getValue();
                    int additions = dayCommits.stream().mapToInt(c -> c.getAdditions() != null ? c.getAdditions() : 0).sum();
                    int deletions = dayCommits.stream().mapToInt(c -> c.getDeletions() != null ? c.getDeletions() : 0).sum();
                    int uniqueContributors = (int) dayCommits.stream()
                            .map(Commit::getAuthorName)
                            .filter(Objects::nonNull)
                            .distinct()
                            .count();

                    return RepoAnalyticsDto.CommitTimelineDto.builder()
                            .date(entry.getKey())
                            .commits(dayCommits.size())
                            .additions(additions)
                            .deletions(deletions)
                            .uniqueContributors(uniqueContributors)
                            .build();
                })
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .limit(30) // Last 30 days
                .collect(Collectors.toList());
    }

    /**
     * Build top commits
     */
    private List<RepoAnalyticsDto.TopCommitDto> buildTopCommits(List<Commit> commits) {
        return commits.stream()
                .filter(c -> c.getAdditions() != null && c.getDeletions() != null)
                .sorted((a, b) -> Integer.compare(
                        (b.getAdditions() + b.getDeletions()),
                        (a.getAdditions() + a.getDeletions())
                ))
                .limit(10)
                .map(commit -> RepoAnalyticsDto.TopCommitDto.builder()
                        .commitSha(commit.getCommitSha())
                        .message(commit.getMessage())
                        .authorName(commit.getAuthorName())
                        .additions(commit.getAdditions())
                        .deletions(commit.getDeletions())
                        .changedFiles(commit.getChangedFiles())
                        .authorDate(commit.getAuthorDate())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Build top contributor DTO
     */
    private RepoAnalyticsDto.TopContributorDto buildTopContributorDto(Contributor contributor) {
        // Get commit count for this contributor
        List<Commit> contributorCommits = commitRepository
                .findByRepositoryIdAndAuthorNameOrderByAuthorDateDesc(
                        contributor.getRepository().getId(),
                        contributor.getContributorName()
                );

        int commitsCount = contributorCommits.size();
        int additionsCount = contributorCommits.stream()
                .mapToInt(c -> c.getAdditions() != null ? c.getAdditions() : 0)
                .sum();
        int deletionsCount = contributorCommits.stream()
                .mapToInt(c -> c.getDeletions() != null ? c.getDeletions() : 0)
                .sum();

        return RepoAnalyticsDto.TopContributorDto.builder()
                .contributorName(contributor.getContributorName())
                .contributorEmail(contributor.getContributorEmail())
                .contributionCount(contributor.getContributionCount())
                .commitsCount(commitsCount)
                .additionsCount(additionsCount)
                .deletionsCount(deletionsCount)
                .firstContribution(contributor.getFirstContributionDate())
                .lastContribution(contributor.getLastContributionDate())
                .build();
    }

    /**
     * Build contributor timeline
     */
    private List<RepoAnalyticsDto.ContributorTimelineDto> buildContributorTimeline(List<Contributor> contributors) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        return contributors.stream()
                .filter(c -> c.getFirstContributionDate() != null)
                .collect(Collectors.groupingBy(
                        c -> c.getFirstContributionDate().toLocalDate().format(formatter),
                        Collectors.toList()
                ))
                .entrySet().stream()
                .map(entry -> {
                    List<Contributor> dayContributors = entry.getValue();

                    return RepoAnalyticsDto.ContributorTimelineDto.builder()
                            .date(entry.getKey())
                            .newContributors(dayContributors.size())
                            .activeContributors(dayContributors.size()) // Simplified
                            .totalContributors(dayContributors.size())   // Simplified
                            .build();
                })
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .limit(30)
                .collect(Collectors.toList());
    }

    /**
     * Get busiest days of week
     */
    private List<String> getBusiestDaysOfWeek(List<Commit> commits) {
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

        Map<Integer, Long> dayCount = commits.stream()
                .filter(c -> c.getAuthorDate() != null)
                .collect(Collectors.groupingBy(
                        c -> c.getAuthorDate().getDayOfWeek().getValue(),
                        Collectors.counting()
                ));

        return dayCount.entrySet().stream()
                .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed())
                .limit(3)
                .map(entry -> days[entry.getKey() - 1])
                .collect(Collectors.toList());
    }

    /**
     * Get busiest hours
     */
    private List<String> getBusiestHours(List<Commit> commits) {
        Map<Integer, Long> hourCount = commits.stream()
                .filter(c -> c.getAuthorDate() != null)
                .collect(Collectors.groupingBy(
                        c -> c.getAuthorDate().getHour(),
                        Collectors.counting()
                ));

        return hourCount.entrySet().stream()
                .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed())
                .limit(3)
                .map(entry -> String.format("%02d:00", entry.getKey()))
                .collect(Collectors.toList());
    }

    /**
     * Refresh repository analytics (force update from GitHub)
     */
    @Transactional
    public Optional<RepoAnalyticsDto> refreshRepositoryAnalytics(String owner, String repoName) {
        try {
            log.info("Refreshing analytics for repository: {}/{}", owner, repoName);

            // Force refresh by updating analyzed_at to old date
            Optional<Repository> existingRepo = repositoryRepository
                    .findByUserGithubUsernameAndRepoName(owner, repoName);

            if (existingRepo.isPresent()) {
                Repository repo = existingRepo.get();
                repo.setAnalyzedAt(LocalDateTime.now().minusDays(2)); // Force it to be old
                repositoryRepository.save(repo);
            }

            return getRepositoryAnalytics(owner, repoName);

        } catch (Exception e) {
            log.error("Error refreshing analytics for repository {}/{}: {}", owner, repoName, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Get repository list by user
     */
//    public List<Repository> getRepositoriesByUser(String username, Pageable pageable) {
//        try {
//            return repositoryRepository.findByUserGithubUsernameOrderByStarsCountDesc(username, pageable);
//        } catch (Exception e) {
//            log.error("Error fetching repositories for user {}: {}", username, e.getMessage());
//            return new ArrayList<>();
//        }
//    }
    public List<Repository> getRepositoriesByUser(String username, Pageable pageable) {
        try {
            log.info("Fetching repositories for user: {} with pagination", username);

            // Repository method returns Page<Repository>, so we need to extract the content
            Page<Repository> repositoryPage = repositoryRepository
                    .findByUserGithubUsernameOrderByStarsCountDesc(username, pageable);

            // Extract the List from the Page
            List<Repository> repositories = repositoryPage.getContent();

            log.info("Found {} repositories for user: {}", repositories.size(), username);
            return repositories;

        } catch (Exception e) {
            log.error("Error fetching repositories for user {}: {}", username, e.getMessage(), e);
            return new ArrayList<>();
        }
    }


    /**
     * Get trending repositories
     */
    public List<Repository> getTrendingRepositories(int days, int minStars) {
        try {
            LocalDateTime since = LocalDateTime.now().minusDays(days);
            return repositoryRepository.findTrendingRepositories(since, minStars);
        } catch (Exception e) {
            log.error("Error fetching trending repositories: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    // Add these methods to your RepoAnalyticsService class:

    /**
     * Get user repositories with pagination
     */
//    public List<Repository> getUserRepositories(String username, Pageable pageable) {
//        return repositoryRepository.findByUserGithubUsernameOrderByStarsCountDesc(username);
//    }
        public List<Repository> getUserRepositories(String username, Pageable pageable) {
            // Use the pageable parameter in your repository call
            Page<Repository> repositoryPage = repositoryRepository.findByUserGithubUsernameOrderByStarsCountDesc(username, pageable);
            return repositoryPage.getContent();
        }

    /**
     * Get trending repositories with limit
     */
    public List<Repository> getTrendingRepositories(int days, int minStars, int limit) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<Repository> trending = repositoryRepository.findTrendingRepositories(since, minStars);
        return trending.stream().limit(limit).collect(Collectors.toList());
    }

    /**
     * Get repositories by language with pagination
     */
    public List<Repository> getRepositoriesByLanguage(String language, Pageable pageable) {
        return repositoryRepository.findByLanguageOrderByStarsCountDesc(language);
    }

    /**
     * Compare multiple repositories
     */
    public Map<String, RepoAnalyticsDto> compareRepositories(List<Map<String, String>> repositories) {
        Map<String, RepoAnalyticsDto> comparisons = new HashMap<>();

        for (Map<String, String> repo : repositories) {
            String owner = repo.get("owner");
            String repoName = repo.get("repoName");
            String key = owner + "/" + repoName;

            Optional<RepoAnalyticsDto> analytics = getRepositoryAnalytics(owner, repoName);
            if (analytics.isPresent()) {
                comparisons.put(key, analytics.get());
            }
        }

        return comparisons;
    }

    /**
     * Get dashboard analytics
     */
    public Map<String, Object> getDashboardAnalytics() {
        Map<String, Object> dashboard = new HashMap<>();

        // Basic counts
        dashboard.put("totalUsers", userRepository.count());
        dashboard.put("totalRepositories", repositoryRepository.count());
        dashboard.put("totalCommits", commitRepository.count());
        dashboard.put("totalContributors", contributorRepository.count());

        // Language stats
        List<Object[]> languageStats = repositoryRepository.countRepositoriesByLanguage();
        dashboard.put("topLanguages", languageStats);

        // Recent activity
        LocalDateTime lastWeek = LocalDateTime.now().minusDays(7);
        List<Repository> recentRepos = repositoryRepository.findByLastPushAtAfterOrderByLastPushAtDesc(lastWeek);
        dashboard.put("recentActivity", recentRepos.size());

        return dashboard;
    }

    /**
     * Get language statistics
     */
    public Map<String, Object> getLanguageStatistics() {
        Map<String, Object> stats = new HashMap<>();

        List<Object[]> languageData = repositoryRepository.countRepositoriesByLanguage();

        List<Map<String, Object>> languages = languageData.stream()
                .map(row -> Map.of(
                        "language", row[0],
                        "repositoryCount", row[1]
                ))
                .collect(Collectors.toList());

        stats.put("languageBreakdown", languages);
        stats.put("totalLanguages", languages.size());

        return stats;
    }
}
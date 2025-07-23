package com.gitanalyzer.service;

import com.gitanalyzer.dto.GitHubApiResponse;
import com.gitanalyzer.model.User;
import com.gitanalyzer.model.Repository;
import com.gitanalyzer.model.Commit;
import com.gitanalyzer.model.Contributor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class GitHubApiService {

    private final RestTemplate restTemplate;

    @Value("${github.api.token}")
    private String githubToken;

    @Value("${github.api.base-url}")
    private String baseUrl;

    public GitHubApiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Get GitHub user information
     */
    public Optional<GitHubApiResponse.GitHubUser> getUser(String username) {
        try {
            String url = baseUrl + "/users/" + username;
            log.info("Fetching user data for: {}", username);

            HttpHeaders headers = createHeaders();
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<GitHubApiResponse.GitHubUser> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    GitHubApiResponse.GitHubUser.class
            );

            log.info("Successfully fetched user data for: {}", username);
            return Optional.ofNullable(response.getBody());

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("User not found: {}", username);
            } else {
                log.error("Client error fetching user {}: {} - {}", username, e.getStatusCode(), e.getMessage());
            }
            return Optional.empty();
        } catch (HttpServerErrorException e) {
            log.error("Server error fetching user {}: {} - {}", username, e.getStatusCode(), e.getMessage());
            return Optional.empty();
        } catch (ResourceAccessException e) {
            log.error("Network error fetching user {}: {}", username, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Unexpected error fetching user {}: {}", username, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Get user's repositories
     */
    public List<GitHubApiResponse.GitHubRepository> getUserRepositories(String username) {
        try {
            String url = baseUrl + "/users/" + username + "/repos?type=all&sort=updated&per_page=100";
            log.info("Fetching repositories for user: {}", username);

            HttpHeaders headers = createHeaders();
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<List<GitHubApiResponse.GitHubRepository>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<GitHubApiResponse.GitHubRepository>>() {}
            );

            List<GitHubApiResponse.GitHubRepository> repositories = response.getBody();
            log.info("Successfully fetched {} repositories for user: {}",
                    repositories != null ? repositories.size() : 0, username);

            return repositories != null ? repositories : List.of();

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("Repositories not found for user: {}", username);
            } else {
                log.error("Client error fetching repositories for {}: {} - {}", username, e.getStatusCode(), e.getMessage());
            }
            return List.of();
        } catch (Exception e) {
            log.error("Error fetching repositories for user {}: {}", username, e.getMessage());
            return List.of();
        }
    }

    /**
     * Get repository information
     */
    public Optional<GitHubApiResponse.GitHubRepository> getRepository(String owner, String repoName) {
        try {
            String url = baseUrl + "/repos/" + owner + "/" + repoName;
            log.info("Fetching repository data for: {}/{}", owner, repoName);

            HttpHeaders headers = createHeaders();
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<GitHubApiResponse.GitHubRepository> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    GitHubApiResponse.GitHubRepository.class
            );

            log.info("Successfully fetched repository data for: {}/{}", owner, repoName);
            return Optional.ofNullable(response.getBody());

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("Repository not found: {}/{}", owner, repoName);
            } else {
                log.error("Client error fetching repository {}/{}: {} - {}", owner, repoName, e.getStatusCode(), e.getMessage());
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error fetching repository {}/{}: {}", owner, repoName, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Get repository commits
     */
    public List<GitHubApiResponse.GitHubCommit> getRepositoryCommits(String owner, String repoName, int page, int perPage) {
        try {
            String url = baseUrl + "/repos/" + owner + "/" + repoName + "/commits?page=" + page + "&per_page=" + perPage;
            log.info("Fetching commits for repository: {}/{} (page: {}, per_page: {})", owner, repoName, page, perPage);

            HttpHeaders headers = createHeaders();
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<List<GitHubApiResponse.GitHubCommit>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<GitHubApiResponse.GitHubCommit>>() {}
            );

            List<GitHubApiResponse.GitHubCommit> commits = response.getBody();
            log.info("Successfully fetched {} commits for repository: {}/{}",
                    commits != null ? commits.size() : 0, owner, repoName);

            return commits != null ? commits : List.of();

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("Commits not found for repository: {}/{}", owner, repoName);
            } else {
                log.error("Client error fetching commits for {}/{}: {} - {}", owner, repoName, e.getStatusCode(), e.getMessage());
            }
            return List.of();
        } catch (Exception e) {
            log.error("Error fetching commits for repository {}/{}: {}", owner, repoName, e.getMessage());
            return List.of();
        }
    }

    /**
     * Get specific commit details
     */
    public Optional<GitHubApiResponse.GitHubCommit> getCommit(String owner, String repoName, String sha) {
        try {
            String url = baseUrl + "/repos/" + owner + "/" + repoName + "/commits/" + sha;
            log.info("Fetching commit details for: {}/{} - {}", owner, repoName, sha);

            HttpHeaders headers = createHeaders();
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<GitHubApiResponse.GitHubCommit> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    GitHubApiResponse.GitHubCommit.class
            );

            log.info("Successfully fetched commit details for: {}/{} - {}", owner, repoName, sha);
            return Optional.ofNullable(response.getBody());

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("Commit not found: {}/{} - {}", owner, repoName, sha);
            } else {
                log.error("Client error fetching commit {}/{} - {}: {} - {}", owner, repoName, sha, e.getStatusCode(), e.getMessage());
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error fetching commit {}/{} - {}: {}", owner, repoName, sha, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Get repository contributors
     */
    public List<GitHubApiResponse.GitHubContributor> getRepositoryContributors(String owner, String repoName) {
        try {
            String url = baseUrl + "/repos/" + owner + "/" + repoName + "/contributors?per_page=100";
            log.info("Fetching contributors for repository: {}/{}", owner, repoName);

            HttpHeaders headers = createHeaders();
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<List<GitHubApiResponse.GitHubContributor>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<GitHubApiResponse.GitHubContributor>>() {}
            );

            List<GitHubApiResponse.GitHubContributor> contributors = response.getBody();
            log.info("Successfully fetched {} contributors for repository: {}/{}",
                    contributors != null ? contributors.size() : 0, owner, repoName);

            return contributors != null ? contributors : List.of();

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("Contributors not found for repository: {}/{}", owner, repoName);
            } else {
                log.error("Client error fetching contributors for {}/{}: {} - {}", owner, repoName, e.getStatusCode(), e.getMessage());
            }
            return List.of();
        } catch (Exception e) {
            log.error("Error fetching contributors for repository {}/{}: {}", owner, repoName, e.getMessage());
            return List.of();
        }
    }

    /**
     * Convert GitHub API User to Entity
     */
    public User convertToUserEntity(GitHubApiResponse.GitHubUser githubUser) {
        User user = new User();
        user.setGithubUsername(githubUser.getLogin());
        user.setName(githubUser.getName());
        user.setAvatarUrl(githubUser.getAvatarUrl());
        user.setBio(githubUser.getBio());
        user.setLocation(githubUser.getLocation());
        user.setCompany(githubUser.getCompany());
        user.setEmail(githubUser.getEmail());
        user.setPublicRepos(githubUser.getPublicRepos());
        user.setFollowers(githubUser.getFollowers());
        user.setFollowing(githubUser.getFollowing());
        user.setCreatedAt(githubUser.getCreatedAt());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    /**
     * Convert GitHub API Repository to Entity
     */
    public Repository convertToRepositoryEntity(GitHubApiResponse.GitHubRepository githubRepo, User user) {
        Repository repository = new Repository();
        repository.setUser(user);
        repository.setRepoName(githubRepo.getName());
        repository.setFullName(githubRepo.getFullName());
        repository.setDescription(githubRepo.getDescription());
        repository.setLanguage(githubRepo.getLanguage());
        repository.setStarsCount(githubRepo.getStargazersCount());
        repository.setForksCount(githubRepo.getForksCount());
        repository.setWatchersCount(githubRepo.getWatchersCount());
        repository.setSizeKb(githubRepo.getSize());
        repository.setDefaultBranch(githubRepo.getDefaultBranch());
        // Fix for the private field - use appropriate getter method
        repository.setIsPrivate(githubRepo.getIsPrivate()); // or githubRepo.getPrivate() depending on your DTO
        repository.setCreatedAt(githubRepo.getCreatedAt());
        repository.setUpdatedAt(githubRepo.getUpdatedAt());
        repository.setLastPushAt(githubRepo.getPushedAt());
        repository.setAnalyzedAt(LocalDateTime.now());
        return repository;
    }

    /**
     * Convert GitHub API Commit to Entity
     */
    public Commit convertToCommitEntity(GitHubApiResponse.GitHubCommit githubCommit, Repository repository) {
        Commit commit = new Commit();
        commit.setRepository(repository);
        commit.setCommitSha(githubCommit.getSha());
        commit.setMessage(githubCommit.getCommit().getMessage());

        if (githubCommit.getCommit().getAuthor() != null) {
            commit.setAuthorName(githubCommit.getCommit().getAuthor().getName());
            commit.setAuthorEmail(githubCommit.getCommit().getAuthor().getEmail());
            commit.setAuthorDate(githubCommit.getCommit().getAuthor().getDate());
        }

        if (githubCommit.getCommit().getCommitter() != null) {
            commit.setCommitterName(githubCommit.getCommit().getCommitter().getName());
            commit.setCommitterEmail(githubCommit.getCommit().getCommitter().getEmail());
            commit.setCommitterDate(githubCommit.getCommit().getCommitter().getDate());
        }

        if (githubCommit.getStats() != null) {
            commit.setAdditions(githubCommit.getStats().getAdditions());
            commit.setDeletions(githubCommit.getStats().getDeletions());
            commit.setChangedFiles(githubCommit.getStats().getTotal());
        }

        commit.setCreatedAt(LocalDateTime.now());
        return commit;
    }

    /**
     * Convert GitHub API Contributor to Entity
     */
    public Contributor convertToContributorEntity(GitHubApiResponse.GitHubContributor githubContributor, Repository repository) {
        Contributor contributor = new Contributor();
        contributor.setRepository(repository);
        contributor.setContributorName(githubContributor.getLogin());
        contributor.setContributionCount(githubContributor.getContributions());
        contributor.setFirstContributionDate(LocalDateTime.now()); // This would need additional API call to get accurate date
        contributor.setLastContributionDate(LocalDateTime.now());   // This would need additional API call to get accurate date
        contributor.setCreatedAt(LocalDateTime.now());
        return contributor;
    }

    /**
     * Check API rate limit status
     */
    public GitHubApiResponse.ApiResponseWrapper<Object> checkRateLimit() {
        try {
            String url = baseUrl + "/rate_limit";
            log.info("Checking GitHub API rate limit");

            HttpHeaders headers = createHeaders();
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<Object> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Object.class
            );

            return GitHubApiResponse.ApiResponseWrapper.builder()
                    .success(true)
                    .message("Rate limit check successful")
                    .data(response.getBody())
                    .metadata(GitHubApiResponse.ApiResponseWrapper.ApiMetadata.builder()
                            .timestamp(LocalDateTime.now())
                            .build())
                    .build();

        } catch (Exception e) {
            log.error("Error checking rate limit: {}", e.getMessage());
            return GitHubApiResponse.ApiResponseWrapper.builder()
                    .success(false)
                    .message("Rate limit check failed")
                    .error(new GitHubApiResponse.ApiResponseWrapper.ApiError(
                            "RATE_LIMIT_ERROR",
                            e.getMessage(),
                            null
                    ))
                    .build();
        }
    }

    /**
     * Create HTTP headers with authentication
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "token " + githubToken);
        headers.set("Accept", "application/vnd.github.v3+json");
        headers.set("User-Agent", "GitAnalyzer-App");
        return headers;
    }

    /**
     * Test GitHub API connectivity
     */
    public GitHubApiResponse.ApiResponseWrapper<String> testConnection() {
        try {
            String url = baseUrl + "/user";
            log.info("Testing GitHub API connection");

            HttpHeaders headers = createHeaders();
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<Object> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Object.class
            );

            return GitHubApiResponse.ApiResponseWrapper.<String>builder()
                    .success(true)
                    .message("GitHub API connection successful")
                    .data("Connected to GitHub API successfully")
                    .metadata(GitHubApiResponse.ApiResponseWrapper.ApiMetadata.builder()
                            .timestamp(LocalDateTime.now())
                            .build())
                    .build();

        } catch (Exception e) {
            log.error("GitHub API connection test failed: {}", e.getMessage());
            return GitHubApiResponse.ApiResponseWrapper.<String>builder()
                    .success(false)
                    .message("GitHub API connection failed")
                    .error(new GitHubApiResponse.ApiResponseWrapper.ApiError(
                            "CONNECTION_ERROR",
                            e.getMessage(),
                            "Check your GitHub token and network connectivity"
                    ))
                    .build();
        }
    }
}
package com.gitanalyzer.repository;

import com.gitanalyzer.model.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

//@Repository
public interface RepositoryRepository extends JpaRepository<Repository, Long> {

    // Find repositories by user
//    List<Repository> findByUserGithubUsernameOrderByStarsCountDesc(String githubUsername);

    Page<Repository> findByUserGithubUsernameOrderByStarsCountDesc(String githubUsername, Pageable pageable);

    List<Repository> findByUserGithubUsernameOrderByStarsCountDesc(String githubUsername);

    // Find repository by user and repo name
    Optional<Repository> findByUserGithubUsernameAndRepoName(String githubUsername, String repoName);

    // Find repositories by programming language
    List<Repository> findByLanguageOrderByStarsCountDesc(String language);

    // Find most starred repositories
    @Query("SELECT r FROM Repository r ORDER BY r.starsCount DESC")
    List<Repository> findMostStarredRepositories();

    // Find repositories with stars greater than specified count
    List<Repository> findByStarsCountGreaterThanOrderByStarsCountDesc(Integer starsCount);

    // Find recently updated repositories
    List<Repository> findByLastPushAtAfterOrderByLastPushAtDesc(LocalDateTime after);

    // Find repositories by size range
    @Query("SELECT r FROM Repository r WHERE r.sizeKb BETWEEN :minSize AND :maxSize ORDER BY r.sizeKb DESC")
    List<Repository> findBySize(@Param("minSize") Integer minSize, @Param("maxSize") Integer maxSize);

    // Get repository with commits and contributors
    @Query("SELECT r FROM Repository r LEFT JOIN FETCH r.commits LEFT JOIN FETCH r.contributors WHERE r.id = :repoId")
    Optional<Repository> findByIdWithCommitsAndContributors(@Param("repoId") Long repoId);

    // Find trending repositories (recently created with good activity)
    @Query("SELECT r FROM Repository r WHERE r.createdAt > :since AND r.starsCount > :minStars ORDER BY r.starsCount DESC")
    List<Repository> findTrendingRepositories(@Param("since") LocalDateTime since, @Param("minStars") Integer minStars);

    // Count repositories by language
    @Query("SELECT r.language, COUNT(r) FROM Repository r WHERE r.language IS NOT NULL GROUP BY r.language ORDER BY COUNT(r) DESC")
    List<Object[]> countRepositoriesByLanguage();

    // Find repositories needing analysis (not analyzed recently)
    @Query("SELECT r FROM Repository r WHERE r.analyzedAt IS NULL OR r.analyzedAt < :before")
    List<Repository> findRepositoriesNeedingAnalysis(@Param("before") LocalDateTime before);

    // Get user's repository statistics
    @Query("SELECT COUNT(r), AVG(r.starsCount), SUM(r.forksCount) FROM Repository r WHERE r.user.githubUsername = :username")
    Object[] getUserRepositoryStats(@Param("username") String username);
}

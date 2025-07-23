package com.gitanalyzer.repository;

import com.gitanalyzer.model.Commit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;



@Repository
public interface CommitRepository extends JpaRepository<Commit, Long> {

    // Find commits by repository
    List<Commit> findByRepositoryIdOrderByAuthorDateDesc(Long repositoryId);

    // Find commit by SHA
    Optional<Commit> findByCommitSha(String commitSha);

    // Find commits by author
    List<Commit> findByAuthorNameOrderByAuthorDateDesc(String authorName);

    // Find commits in date range
    List<Commit> findByAuthorDateBetweenOrderByAuthorDateDesc(LocalDateTime startDate, LocalDateTime endDate);

    // Find commits by repository and author
    List<Commit> findByRepositoryIdAndAuthorNameOrderByAuthorDateDesc(Long repositoryId, String authorName);

    // Get commit statistics for repository
    @Query("SELECT COUNT(c), SUM(c.additions), SUM(c.deletions), SUM(c.changedFiles) FROM Commit c WHERE c.repository.id = :repoId")
    Object[] getRepositoryCommitStats(@Param("repoId") Long repoId);

    // Get commit activity by date (for charts)
    @Query("SELECT DATE(c.authorDate), COUNT(c) FROM Commit c WHERE c.repository.id = :repoId AND c.authorDate > :since GROUP BY DATE(c.authorDate) ORDER BY DATE(c.authorDate)")
    List<Object[]> getCommitActivityByDate(@Param("repoId") Long repoId, @Param("since") LocalDateTime since);

    // Find most active contributors by commit count
    @Query("SELECT c.authorName, COUNT(c) FROM Commit c WHERE c.repository.id = :repoId GROUP BY c.authorName ORDER BY COUNT(c) DESC")
    List<Object[]> getMostActiveContributors(@Param("repoId") Long repoId);

    // Get commits with high impact (many changes)
    @Query("SELECT c FROM Commit c WHERE c.repository.id = :repoId AND (c.additions + c.deletions) > :threshold ORDER BY (c.additions + c.deletions) DESC")
    List<Commit> getHighImpactCommits(@Param("repoId") Long repoId, @Param("threshold") Integer threshold);

    // Find recent commits across all repositories for a user
    @Query("SELECT c FROM Commit c WHERE c.repository.user.githubUsername = :username AND c.authorDate > :since ORDER BY c.authorDate DESC")
    List<Commit> getRecentCommitsForUser(@Param("username") String username, @Param("since") LocalDateTime since);

    // Get commit frequency by hour of day
    @Query("SELECT HOUR(c.authorDate), COUNT(c) FROM Commit c WHERE c.repository.id = :repoId GROUP BY HOUR(c.authorDate) ORDER BY HOUR(c.authorDate)")
    List<Object[]> getCommitsByHourOfDay(@Param("repoId") Long repoId);

    // Check if commit exists
    boolean existsByCommitSha(String commitSha);

    List<Commit> findByRepository_User_GithubUsernameAndCommitterDateAfterOrderByCommitterDateDesc(
            String githubUsername, LocalDateTime after);

    // Find all commits by user
    List<Commit> findByRepository_User_GithubUsernameOrderByCommitterDateDesc(String githubUsername);

    // Count commits by repository
    @Query("SELECT COUNT(c) FROM Commit c WHERE c.repository.id = :repositoryId")
    int countByRepositoryId(@Param("repositoryId") Long repositoryId);
}
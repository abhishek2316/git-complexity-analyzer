package com.gitanalyzer.repository;

import com.gitanalyzer.model.Contributor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContributorRepository extends JpaRepository<Contributor, Long> {

    // Find contributors by repository
    List<Contributor> findByRepositoryIdOrderByContributionCountDesc(Long repositoryId);

    // Find contributor by repository and name
    Optional<Contributor> findByRepositoryIdAndContributorName(Long repositoryId, String contributorName);

    // Find contributors by name across all repositories
    List<Contributor> findByContributorNameOrderByContributionCountDesc(String contributorName);

    // Find top contributors across all repositories
    @Query("SELECT c.contributorName, SUM(c.contributionCount) FROM Contributor c GROUP BY c.contributorName ORDER BY SUM(c.contributionCount) DESC")
    List<Object[]> findTopContributors();

    // Find contributors with contributions greater than specified count
    List<Contributor> findByContributionCountGreaterThanOrderByContributionCountDesc(Integer contributionCount);

    // Find active contributors (recent activity)
    List<Contributor> findByLastContributionDateAfterOrderByLastContributionDateDesc(LocalDateTime after);

    // Get contributor statistics for a repository
    @Query("SELECT COUNT(c), AVG(c.contributionCount), SUM(c.contributionCount) FROM Contributor c WHERE c.repository.id = :repoId")
    Object[] getRepositoryContributorStats(@Param("repoId") Long repoId);

    // Find contributors who contributed to multiple repositories
    @Query("SELECT c.contributorName, COUNT(DISTINCT c.repository.id) FROM Contributor c GROUP BY c.contributorName HAVING COUNT(DISTINCT c.repository.id) > 1 ORDER BY COUNT(DISTINCT c.repository.id) DESC")
    List<Object[]> findMultiRepoContributors();

    // Find new contributors (first contribution in date range)
    List<Contributor> findByFirstContributionDateBetweenOrderByFirstContributionDateDesc(LocalDateTime startDate, LocalDateTime endDate);

    // Get contribution timeline for repository
    @Query("SELECT DATE(c.firstContributionDate), COUNT(c) FROM Contributor c WHERE c.repository.id = :repoId GROUP BY DATE(c.firstContributionDate) ORDER BY DATE(c.firstContributionDate)")
    List<Object[]> getContributionTimeline(@Param("repoId") Long repoId);

    // Find contributors by email domain
    @Query("SELECT c FROM Contributor c WHERE c.contributorEmail LIKE %:domain% ORDER BY c.contributionCount DESC")
    List<Contributor> findByEmailDomain(@Param("domain") String domain);

    // Check if contributor exists in repository
    boolean existsByRepositoryIdAndContributorName(Long repositoryId, String contributorName);
}
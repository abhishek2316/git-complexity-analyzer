package com.gitanalyzer.repository;

import com.gitanalyzer.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Find user by GitHub username
    Optional<User> findByGithubUsername(String githubUsername);

    // Check if user exists by username
    boolean existsByGithubUsername(String githubUsername);

    // Find users with most followers
    @Query("SELECT u FROM User u ORDER BY u.followers DESC")
    List<User> findTopUsersByFollowers();

    // Find users by location
    List<User> findByLocationContainingIgnoreCase(String location);

    // Find users by company
    List<User> findByCompanyContainingIgnoreCase(String company);

    // Find recently updated users
    List<User> findByUpdatedAtAfterOrderByUpdatedAtDesc(LocalDateTime after);

    // Find users with repositories count greater than specified
    @Query("SELECT u FROM User u WHERE u.publicRepos > :repoCount")
    List<User> findUsersWithReposGreaterThan(@Param("repoCount") Integer repoCount);

    // Get user analytics summary
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.repositories WHERE u.githubUsername = :username")
    Optional<User> findByGithubUsernameWithRepositories(@Param("username") String username);

    // Find active users (users with recent activity)
    @Query("SELECT DISTINCT u FROM User u JOIN u.repositories r WHERE r.lastPushAt > :since")
    List<User> findActiveUsersSince(@Param("since") LocalDateTime since);

    // Count users by location
    @Query("SELECT u.location, COUNT(u) FROM User u WHERE u.location IS NOT NULL GROUP BY u.location")
    List<Object[]> countUsersByLocation();
}
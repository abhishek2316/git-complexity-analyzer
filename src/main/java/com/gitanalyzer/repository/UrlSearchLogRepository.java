package com.gitanalyzer.repository;

import com.gitanalyzer.model.UrlSearchLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UrlSearchLogRepository extends JpaRepository<UrlSearchLog, Long> {

//    List<UrlSearchLog> findByCreatedAtAfter(LocalDateTime startDate);

    List<UrlSearchLog> findBySearchTimestampAfter(LocalDateTime startDate);


    // Existing methods...
    List<UrlSearchLog> findByExtractedUsernameOrderBySearchTimestampDesc(String username);
    List<UrlSearchLog> findBySearchTimestampAfterOrderBySearchTimestampDesc(LocalDateTime after);

    @Query("SELECT COUNT(u) FROM UrlSearchLog u WHERE u.responseStatus = ?1")
    Long countByResponseStatus(String status);

    // Additional methods for analytics

    // Find searches by IP address
    List<UrlSearchLog> findByIpAddressOrderBySearchTimestampDesc(String ipAddress);

    // Get search analytics by date
    @Query("SELECT DATE(u.searchTimestamp), COUNT(u) FROM UrlSearchLog u WHERE u.searchTimestamp > :since GROUP BY DATE(u.searchTimestamp) ORDER BY DATE(u.searchTimestamp)")
    List<Object[]> getSearchAnalyticsByDate(@Param("since") LocalDateTime since);

    // Get most searched users
    @Query("SELECT u.extractedUsername, COUNT(u) FROM UrlSearchLog u WHERE u.extractedUsername IS NOT NULL GROUP BY u.extractedUsername ORDER BY COUNT(u) DESC")
    List<Object[]> getMostSearchedUsers();

    // Get search type statistics
    @Query("SELECT u.searchType, COUNT(u) FROM UrlSearchLog u GROUP BY u.searchType")
    List<Object[]> getSearchTypeStats();

    // Find failed searches
    List<UrlSearchLog> findByResponseStatusNotOrderBySearchTimestampDesc(String status);
}

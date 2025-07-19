package com.gitanalyzer.repository;

import com.gitanalyzer.model.UrlSearchLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UrlSearchLogRepository extends JpaRepository<UrlSearchLog, Long> {

    // Find logs by username
    List<UrlSearchLog> findByExtractedUsernameOrderBySearchTimestampDesc(String username);

    // Find recent logs
    List<UrlSearchLog> findBySearchTimestampAfterOrderBySearchTimestampDesc(LocalDateTime after);

    // Count searches by status
    @Query("SELECT COUNT(u) FROM UrlSearchLog u WHERE u.responseStatus = ?1")
    Long countByResponseStatus(String status);
}

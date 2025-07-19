package com.gitanalyzer.service;

import com.gitanalyzer.model.UrlSearchLog;
import com.gitanalyzer.repository.UrlSearchLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SearchLogService {

    @Autowired
    private UrlSearchLogRepository urlSearchLogRepository;

    // GitHub URL patterns
    private static final String USER_PATTERN = "https://github\\.com/([^/]+)/?$";
    private static final String REPO_PATTERN = "https://github\\.com/([^/]+)/([^/]+)/?$";

    public UrlSearchLog logSearch(String githubUrl, String ipAddress, String userAgent) {

        UrlSearchLog log = new UrlSearchLog(githubUrl, ipAddress, userAgent);

        // Parse URL and extract information
        parseGitHubUrl(log, githubUrl);

        // Set initial status
        log.setResponseStatus("PENDING");

        // Save to database
        return urlSearchLogRepository.save(log);
    }

    private void parseGitHubUrl(UrlSearchLog log, String url) {
        try {
            // Check if it's a repository URL
            Pattern repoPattern = Pattern.compile(REPO_PATTERN);
            Matcher repoMatcher = repoPattern.matcher(url);

            if (repoMatcher.matches()) {
                log.setSearchType("REPOSITORY");
                log.setExtractedUsername(repoMatcher.group(1));
                log.setExtractedRepo(repoMatcher.group(2));
                return;
            }

            // Check if it's a user profile URL
            Pattern userPattern = Pattern.compile(USER_PATTERN);
            Matcher userMatcher = userPattern.matcher(url);

            if (userMatcher.matches()) {
                log.setSearchType("USER_PROFILE");
                log.setExtractedUsername(userMatcher.group(1));
                return;
            }

            // If no pattern matches
            log.setSearchType("UNKNOWN");

        } catch (Exception e) {
            log.setSearchType("ERROR");
        }
    }

    public void updateLogStatus(Long logId, String status, Integer processingTime) {
        UrlSearchLog log = urlSearchLogRepository.findById(logId).orElse(null);
        if (log != null) {
            log.setResponseStatus(status);
            log.setProcessingTimeMs(processingTime);
            urlSearchLogRepository.save(log);
        }
    }
}

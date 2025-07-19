package com.gitanalyzer.controller;

import com.gitanalyzer.model.UrlSearchLog;
import com.gitanalyzer.service.SearchLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/search-log")
@CrossOrigin(origins = "*") // For frontend integration later
public class SearchLogController {

    @Autowired
    private SearchLogService searchLogService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> logGitHubSearch(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {

        long startTime = System.currentTimeMillis();

        try {
            String githubUrl = request.get("githubUrl");

            // Basic validation
            if (githubUrl == null || githubUrl.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("GitHub URL is required"));
            }

            if (!githubUrl.startsWith("https://github.com/")) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Invalid GitHub URL format"));
            }

            // Extract client info
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");

            // Log the search
            UrlSearchLog log = searchLogService.logSearch(githubUrl, ipAddress, userAgent);

            // Calculate processing time
            long processingTime = System.currentTimeMillis() - startTime;
            searchLogService.updateLogStatus(log.getId(), "SUCCESS", (int) processingTime);

            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Search logged successfully");
            response.put("logId", log.getId());
            response.put("searchType", log.getSearchType());
            response.put("extractedUsername", log.getExtractedUsername());
            response.put("extractedRepo", log.getExtractedRepo());
            response.put("processingTimeMs", processingTime);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;

            Map<String, Object> errorResponse = createErrorResponse("Internal server error: " + e.getMessage());
            errorResponse.put("processingTimeMs", processingTime);

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("X-Real-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return response;
    }
}
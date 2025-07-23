package com.gitanalyzer.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;

@Slf4j
public class GitHubApiErrorHandler implements ResponseErrorHandler {

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        HttpStatusCode statusCode = response.getStatusCode();
        return statusCode.is4xxClientError() || statusCode.is5xxServerError();
    }

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        HttpStatusCode statusCode = response.getStatusCode();

        // Cast to HttpStatus for switch statement
        if (statusCode instanceof HttpStatus httpStatus) {
            handleKnownStatusCode(httpStatus, response);
        } else {
            // Handle custom status codes that aren't in HttpStatus enum
            log.error("GitHub API error: {} - {}", statusCode.value(), response.getStatusText());
        }
    }

    private void handleKnownStatusCode(HttpStatus httpStatus, ClientHttpResponse response) throws IOException {
        switch (httpStatus) {
            case NOT_FOUND:
                log.warn("GitHub API: Resource not found (404)");
                break;
            case UNAUTHORIZED:
                log.error("GitHub API: Unauthorized access (401) - Check your token");
                break;
            case FORBIDDEN:
                log.error("GitHub API: Forbidden access (403) - Rate limit exceeded or insufficient permissions");
                break;
            case UNPROCESSABLE_ENTITY:
                log.error("GitHub API: Validation failed (422)");
                break;
            case TOO_MANY_REQUESTS:
                log.error("GitHub API: Rate limit exceeded (429) - Too many requests");
                break;
            case INTERNAL_SERVER_ERROR:
                log.error("GitHub API: Internal server error (500)");
                break;
            case BAD_GATEWAY:
                log.error("GitHub API: Bad gateway (502)");
                break;
            case SERVICE_UNAVAILABLE:
                log.error("GitHub API: Service unavailable (503)");
                break;
            default:
                if (httpStatus.is4xxClientError()) {
                    log.error("GitHub API client error: {} - {}", httpStatus.value(), response.getStatusText());
                } else if (httpStatus.is5xxServerError()) {
                    log.error("GitHub API server error: {} - {}", httpStatus.value(), response.getStatusText());
                } else {
                    log.error("GitHub API error: {} - {}", httpStatus.value(), response.getStatusText());
                }
        }
    }
}

package com.gitanalyzer.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller for serving the main application pages.
 * Maps root URLs to static HTML files and handles basic page routing.
 */
@Slf4j
@Controller
public class HomeController {

    /**
     * Serves the main search page (index.html).
     * This is the landing page where users can input GitHub URLs or usernames.
     * URL: GET /
     */
    @GetMapping("/")
    public String home() {
        log.info("Redirecting to static index.html");
        return "redirect:/index.html";
    }

    @GetMapping("/home")
    public String homeAlternative() {
        log.info("Redirecting to static index.html via /home route");
        return "redirect:/index.html";
    }
//    @GetMapping("/")
//    public String home() {
//        log.info("Serving home page (index.html)");
//        return "index"; // Corresponds to src/main/resources/static/index.html
//    }
//
//    /**
//     * Alternative mapping for the home page.
//     * URL: GET /home
//     */
//    @GetMapping("/home")
//    public String homeAlternative() {
//        log.info("Serving home page via /home route");
//        return "index";
//    }

    /**
     * Serves the analytics results page (analytics.html).
     * This page displays the GitHub analytics results with charts and graphs.
     * URL: GET /analytics
     */
    @GetMapping("/analytics")
    public String analyticsPage(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String repo,
            @RequestParam(required = false) String owner,
            Model model) {

        log.info("Serving analytics page with params - username: {}, repo: {}, owner: {}",
                username, repo, owner);

        // Add parameters to model for use in the HTML template if needed
        if (username != null) {
            model.addAttribute("username", username);
        }
        if (repo != null) {
            model.addAttribute("repo", repo);
        }
        if (owner != null) {
            model.addAttribute("owner", owner);
        }

        return "analytics"; // Corresponds to src/main/resources/static/analytics.html
    }

    /**
     * Serves analytics page for a specific user.
     * URL: GET /analytics/user/{username}
     */
    @GetMapping("/analytics/user/{username}")
    public String userAnalyticsPage(@PathVariable String username, Model model) {
        log.info("Serving user analytics page for: {}", username);
        model.addAttribute("username", username);
        model.addAttribute("type", "user");
        return "analytics";
    }

    /**
     * Serves analytics page for a specific repository.
     * URL: GET /analytics/repository/{owner}/{repo}
     */
    @GetMapping("/analytics/repository/{owner}/{repo}")
    public String repositoryAnalyticsPage(
            @PathVariable String owner,
            @PathVariable String repo,
            Model model) {

        log.info("Serving repository analytics page for: {}/{}", owner, repo);
        model.addAttribute("owner", owner);
        model.addAttribute("repo", repo);
        model.addAttribute("type", "repository");
        return "analytics";
    }

    /**
     * Serves a dashboard page showing overall statistics.
     * URL: GET /dashboard
     */
    @GetMapping("/dashboard")
    public String dashboardPage() {
        log.info("Serving dashboard page");
        return "dashboard"; // You can create this HTML file later if needed
    }

    /**
     * Serves an about page with information about the application.
     * URL: GET /about
     */
    @GetMapping("/about")
    public String aboutPage() {
        log.info("Serving about page");
        return "about"; // You can create this HTML file later if needed
    }

    /**
     * Health check endpoint for the web application.
     * URL: GET /health
     */
    @GetMapping("/health")
    public String healthPage() {
        log.info("Serving health check page");
        return "health"; // Simple health status page
    }

    /**
     * Error page handler (optional).
     * URL: GET /error
     */
    @GetMapping("/error")
    public String errorPage() {
        log.info("Serving error page");
        return "error"; // Error page template
    }
}
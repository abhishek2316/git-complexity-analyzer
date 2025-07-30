// GitHub Analytics - Main JavaScript File
class GitHubAnalytics {
    constructor() {
        this.baseURL = 'http://localhost:8080/api'; // Update with your backend URL
        this.currentSearchType = 'user';
        this.init();
    }

    init() {
        this.bindEvents();
        this.initializeToggles();
    }

    bindEvents() {
        // Search type toggle buttons
        document.getElementById('userSearchBtn').addEventListener('click', () => {
            this.switchSearchType('user');
        });

        document.getElementById('repoSearchBtn').addEventListener('click', () => {
            this.switchSearchType('repository');
        });

        // Search buttons
        document.getElementById('userSearchSubmit').addEventListener('click', () => {
            this.handleUserSearch();
        });

        document.getElementById('repoSearchSubmit').addEventListener('click', () => {
            this.handleRepoSearch();
        });

        document.getElementById('urlSearchSubmit').addEventListener('click', () => {
            this.handleUrlSearch();
        });

        // Enter key support for inputs
        document.getElementById('username').addEventListener('keypress', (e) => {
            if (e.key === 'Enter') this.handleUserSearch();
        });

        document.getElementById('repoOwner').addEventListener('keypress', (e) => {
            if (e.key === 'Enter') this.handleRepoSearch();
        });

        document.getElementById('repoName').addEventListener('keypress', (e) => {
            if (e.key === 'Enter') this.handleRepoSearch();
        });

        document.getElementById('githubUrl').addEventListener('keypress', (e) => {
            if (e.key === 'Enter') this.handleUrlSearch();
        });

        // Example cards
        document.querySelectorAll('.example-card').forEach(card => {
            card.addEventListener('click', () => {
                this.handleExampleClick(card);
            });
        });

        // Error retry button
        document.getElementById('errorRetry').addEventListener('click', () => {
            this.hideError();
        });

        // Real-time URL parsing
        document.getElementById('githubUrl').addEventListener('input', (e) => {
            this.parseGitHubUrl(e.target.value);
        });
    }

    initializeToggles() {
        this.switchSearchType('user');
    }

    switchSearchType(type) {
        this.currentSearchType = type;

        // Update toggle buttons
        document.querySelectorAll('.toggle-btn').forEach(btn => {
            btn.classList.remove('active');
        });

        if (type === 'user') {
            document.getElementById('userSearchBtn').classList.add('active');
            document.getElementById('userSearchForm').classList.add('active');
            document.getElementById('repoSearchForm').classList.remove('active');
        } else {
            document.getElementById('repoSearchBtn').classList.add('active');
            document.getElementById('repoSearchForm').classList.add('active');
            document.getElementById('userSearchForm').classList.remove('active');
        }

        // Clear any existing errors
        this.hideError();
    }

    async handleUserSearch() {
        const username = document.getElementById('username').value.trim();

        if (!this.validateUserInput(username)) {
            return;
        }

        // Check if user is trying to search a repository URL in user search
        if (this.isRepositoryUrl(username)) {
            this.showError('Invalid Input', 'You\'re trying to search for a repository in User Analytics. Please switch to Repository Analytics or use a username only.');
            return;
        }

        this.showLoading('Fetching user data from GitHub...');

        try {
            const userData = await this.fetchUserAnalytics(username);
            this.hideLoading();
            this.displayUserResults(userData);
        } catch (error) {
            this.hideLoading();
            this.showError('User Not Found', error.message || 'Unable to fetch user data. Please check the username and try again.');
        }
    }

    async handleRepoSearch() {
        const owner = document.getElementById('repoOwner').value.trim();
        const repo = document.getElementById('repoName').value.trim();

        if (!this.validateRepoInput(owner, repo)) {
            return;
        }

        // Check if user is trying to search a single username in repo search
        if (owner && !repo && !owner.includes('/')) {
            this.showError('Invalid Input', 'You\'re trying to search for a user in Repository Analytics. Please provide both owner and repository name, or switch to User Analytics.');
            return;
        }

        this.showLoading('Fetching repository data from GitHub...');

        try {
            const repoData = await this.fetchRepositoryAnalytics(owner, repo);
            this.hideLoading();
            this.displayRepoResults(repoData);
        } catch (error) {
            this.hideLoading();
            this.showError('Repository Not Found', error.message || 'Unable to fetch repository data. Please check the owner and repository name.');
        }
    }

    async handleUrlSearch() {
        const url = document.getElementById('githubUrl').value.trim();

        if (!this.validateUrlInput(url)) {
            return;
        }

        const parsed = this.parseGitHubUrl(url);
        if (!parsed.isValid) {
            this.showError('Invalid URL', 'Please enter a valid GitHub URL (e.g., https://github.com/username or https://github.com/owner/repository)');
            return;
        }

        if (parsed.type === 'user') {
            // Auto-fill user search and execute
            document.getElementById('username').value = parsed.username;
            this.switchSearchType('user');
            await this.handleUserSearch();
        } else if (parsed.type === 'repository') {
            // Auto-fill repo search and execute
            document.getElementById('repoOwner').value = parsed.owner;
            document.getElementById('repoName').value = parsed.repo;
            this.switchSearchType('repository');
            await this.handleRepoSearch();
        }
    }

    handleExampleClick(card) {
        const type = card.dataset.type;

        if (type === 'user') {
            const username = card.dataset.value;
            document.getElementById('username').value = username;
            this.switchSearchType('user');
            this.handleUserSearch();
        } else if (type === 'repository') {
            const owner = card.dataset.owner;
            const repo = card.dataset.repo;
            document.getElementById('repoOwner').value = owner;
            document.getElementById('repoName').value = repo;
            this.switchSearchType('repository');
            this.handleRepoSearch();
        }
    }

    // Validation Functions
    validateUserInput(username) {
        if (!username) {
            this.showError('Input Required', 'Please enter a GitHub username.');
            return false;
        }

        if (username.length > 39) {
            this.showError('Invalid Username', 'GitHub usernames cannot be longer than 39 characters.');
            return false;
        }

        if (!/^[a-zA-Z0-9]([a-zA-Z0-9\-]){0,38}$/.test(username)) {
            this.showError('Invalid Username', 'Username can only contain alphanumeric characters and hyphens, and cannot start or end with a hyphen.');
            return false;
        }

        return true;
    }

    validateRepoInput(owner, repo) {
        if (!owner || !repo) {
            this.showError('Input Required', 'Please enter both repository owner and repository name.');
            return false;
        }

        if (owner.length > 39 || repo.length > 100) {
            this.showError('Invalid Input', 'Owner name cannot exceed 39 characters and repository name cannot exceed 100 characters.');
            return false;
        }

        if (!/^[a-zA-Z0-9]([a-zA-Z0-9\-]){0,38}$/.test(owner)) {
            this.showError('Invalid Owner', 'Owner name can only contain alphanumeric characters and hyphens.');
            return false;
        }

        if (!/^[a-zA-Z0-9._\-]+$/.test(repo)) {
            this.showError('Invalid Repository Name', 'Repository name can only contain alphanumeric characters, hyphens, underscores, and dots.');
            return false;
        }

        return true;
    }

    validateUrlInput(url) {
        if (!url) {
            this.showError('Input Required', 'Please enter a GitHub URL.');
            return false;
        }

        if (!url.startsWith('https://github.com/') && !url.startsWith('http://github.com/')) {
            this.showError('Invalid URL', 'Please enter a valid GitHub URL starting with https://github.com/');
            return false;
        }

        return true;
    }

    isRepositoryUrl(input) {
        return input.includes('/') && !input.startsWith('https://') && !input.startsWith('http://');
    }

    // URL Parsing Function
    parseGitHubUrl(url) {
        const result = {
            isValid: false,
            type: null,
            username: null,
            owner: null,
            repo: null
        };

        if (!url) return result;

        try {
            // Remove trailing slash and normalize URL
            url = url.replace(/\/$/, '');

            // Match GitHub URL pattern
            const githubPattern = /^https?:\/\/github\.com\/([^\/]+)(?:\/([^\/]+))?/;
            const match = url.match(githubPattern);

            if (!match) return result;

            const [, firstPart, secondPart] = match;

            if (secondPart) {
                // Repository URL: https://github.com/owner/repo
                result.isValid = true;
                result.type = 'repository';
                result.owner = firstPart;
                result.repo = secondPart;
            } else {
                // User URL: https://github.com/username
                result.isValid = true;
                result.type = 'user';
                result.username = firstPart;
            }

            // Update UI to show parsed information
            this.updateUrlPreview(result);

        } catch (error) {
            console.error('Error parsing GitHub URL:', error);
        }

        return result;
    }

    updateUrlPreview(parsed) {
        const urlInput = document.getElementById('githubUrl');
        const helpText = urlInput.parentNode.nextElementSibling;

        if (parsed.isValid) {
            if (parsed.type === 'user') {
                helpText.innerHTML = `✅ Valid user URL detected: <strong>${parsed.username}</strong>`;
                helpText.style.color = 'var(--secondary-color)';
            } else if (parsed.type === 'repository') {
                helpText.innerHTML = `✅ Valid repository URL detected: <strong>${parsed.owner}/${parsed.repo}</strong>`;
                helpText.style.color = 'var(--secondary-color)';
            }
        } else {
            helpText.innerHTML = 'Supports: https://github.com/username or https://github.com/owner/repository';
            helpText.style.color = 'var(--text-muted)';
        }
    }

    // API Functions
    async fetchUserAnalytics(username) {
        try {
            const response = await fetch(`${this.baseURL}/analytics/user/${username}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                }
            });

            if (!response.ok) {
                if (response.status === 404) {
                    throw new Error(`User '${username}' not found on GitHub.`);
                } else if (response.status === 403) {
                    throw new Error('GitHub API rate limit exceeded. Please try again later.');
                } else if (response.status >= 500) {
                    throw new Error('Server error. Please try again later.');
                } else {
                    throw new Error(`Error fetching user data (${response.status})`);
                }
            }

            const result = await response.json();

            // Handle your API response structure
            if (result.success && result.data) {
                return result; // Return the full response object
            } else {
                throw new Error(result.message || 'Invalid response format');
            }

        } catch (error) {
            if (error.name === 'TypeError' && error.message.includes('fetch')) {
                throw new Error('Unable to connect to the server. Please check your internet connection.');
            }
            throw error;
        }
    }

//    async fetchRepositoryAnalytics(owner, repo) {
//        try {
//            const response = await fetch(`${this.baseURL}/analytics/repository/${owner}/${repo}`, {
//                method: 'GET',
//                headers: {
//                    'Content-Type': 'application/json',
//                    'Accept': 'application/json'
//                }
//            });
//
//            if (!response.ok) {
//                if (response.status === 404) {
//                    throw new Error(`Repository '${owner}/${repo}' not found on GitHub.`);
//                } else if (response.status === 403) {
//                    throw new Error('GitHub API rate limit exceeded. Please try again later.');
//                } else if (response.status >= 500) {
//                    throw new Error('Server error. Please try again later.');
//                } else {
//                    throw new Error(`Error fetching repository data (${response.status})`);
//                }
//            }
//
//            return await response.json();
//        } catch (error) {
//            if (error.name === 'TypeError' && error.message.includes('fetch')) {
//                throw new Error('Unable to connect to the server. Please check your internet connection.');
//            }
//            throw error;
//        }
//    }
    async fetchRepositoryAnalytics(owner, repo) {
        try {
            const response = await fetch(`${this.baseURL}/analytics/repository/${owner}/${repo}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                }
            });

            if (!response.ok) {
                if (response.status === 404) {
                    throw new Error(`Repository '${owner}/${repo}' not found on GitHub.`);
                } else if (response.status === 403) {
                    throw new Error('GitHub API rate limit exceeded. Please try again later.');
                } else if (response.status >= 500) {
                    throw new Error('Server error. Please try again later.');
                } else {
                    throw new Error(`Error fetching repository data (${response.status})`);
                }
            }

            const result = await response.json();

            // Handle your API response structure
            if (result.success && result.data) {
                return result; // Return the full response object
            } else {
                throw new Error(result.message || 'Invalid response format');
            }

        } catch (error) {
            if (error.name === 'TypeError' && error.message.includes('fetch')) {
                throw new Error('Unable to connect to the server. Please check your internet connection.');
            }
            throw error;
        }
    }

    // UI Helper Functions
    showLoading(message = 'Loading...') {
        const loadingIndicator = document.getElementById('loadingIndicator');
        const loadingText = document.getElementById('loadingText');
        const progressText = document.getElementById('progressText');
        const progressFill = document.querySelector('.progress-fill');

        loadingText.textContent = message;
        progressText.textContent = 'Initializing...';
        progressFill.style.width = '0%';

        this.hideError();
        loadingIndicator.classList.remove('hidden');

        // Simulate progress
        this.simulateProgress();
    }

    simulateProgress() {
        const progressFill = document.querySelector('.progress-fill');
        const progressText = document.getElementById('progressText');

        const steps = [
            { progress: 20, text: 'Connecting to GitHub API...' },
            { progress: 40, text: 'Fetching user/repository data...' },
            { progress: 60, text: 'Processing analytics...' },
            { progress: 80, text: 'Preparing results...' },
            { progress: 100, text: 'Complete!' }
        ];

        let currentStep = 0;

        const updateProgress = () => {
            if (currentStep < steps.length) {
                const step = steps[currentStep];
                progressFill.style.width = `${step.progress}%`;
                progressText.textContent = step.text;
                currentStep++;
                setTimeout(updateProgress, 800);
            }
        };

        updateProgress();
    }

    hideLoading() {
        document.getElementById('loadingIndicator').classList.add('hidden');
    }

    showError(title, message) {
        const errorDisplay = document.getElementById('errorDisplay');
        const errorMessage = document.getElementById('errorMessage');

        errorDisplay.querySelector('h3').textContent = title;
        errorMessage.textContent = message;

        this.hideLoading();
        errorDisplay.classList.remove('hidden');

        // Auto-hide error after 10 seconds
        setTimeout(() => {
            this.hideError();
        }, 10000);
    }

    hideError() {
        document.getElementById('errorDisplay').classList.add('hidden');
    }

    // Results Display Functions
    displayUserResults(userData) {
        // Clear existing content and redirect to results page
        this.redirectToResults('user', userData);
    }

    displayRepoResults(repoData) {
        // Clear existing content and redirect to results page
        this.redirectToResults('repository', repoData);
    }

    redirectToResults(type, data) {
        // Store data in sessionStorage for the results page
        sessionStorage.setItem('analyticsData', JSON.stringify({
            type: type,
            data: data,
            timestamp: Date.now()
        }));

        // Redirect to results page
        window.location.href = '/analytics.html';
    }

    // Utility Functions
    formatNumber(num) {
        if (num >= 1000000) {
            return (num / 1000000).toFixed(1) + 'M';
        } else if (num >= 1000) {
            return (num / 1000).toFixed(1) + 'K';
        }
        return num.toString();
    }

    formatDate(dateString) {
        const date = new Date(dateString);
        return date.toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric'
        });
    }

    copyToClipboard(text) {
        navigator.clipboard.writeText(text).then(() => {
            this.showTemporaryMessage('Copied to clipboard!');
        }).catch(err => {
            console.error('Failed to copy text: ', err);
        });
    }

    showTemporaryMessage(message, duration = 3000) {
        // Create temporary message element
        const messageEl = document.createElement('div');
        messageEl.textContent = message;
        messageEl.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            background: var(--secondary-color);
            color: white;
            padding: 12px 20px;
            border-radius: 6px;
            z-index: 1000;
            font-weight: 500;
            box-shadow: var(--shadow-lg);
            animation: slideInRight 0.3s ease-out;
        `;

        document.body.appendChild(messageEl);

        setTimeout(() => {
            messageEl.style.animation = 'slideOutRight 0.3s ease-in';
            setTimeout(() => {
                document.body.removeChild(messageEl);
            }, 300);
        }, duration);
    }
}

// Additional CSS for animations (add to your CSS file)
const additionalCSS = `
@keyframes slideInRight {
    from {
        transform: translateX(100%);
        opacity: 0;
    }
    to {
        transform: translateX(0);
        opacity: 1;
    }
}

@keyframes slideOutRight {
    from {
        transform: translateX(0);
        opacity: 1;
    }
    to {
        transform: translateX(100%);
        opacity: 0;
    }
}
`;

// Add additional CSS to the page
const style = document.createElement('style');
style.textContent = additionalCSS;
document.head.appendChild(style);

// Initialize the application when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.githubAnalytics = new GitHubAnalytics();
});

// Handle browser back/forward buttons
window.addEventListener('popstate', (event) => {
    if (event.state && event.state.page === 'search') {
        // Handle return to search page
        window.githubAnalytics.hideLoading();
        window.githubAnalytics.hideError();
    }
});

// Export for module use if needed
if (typeof module !== 'undefined' && module.exports) {
    module.exports = GitHubAnalytics;
}

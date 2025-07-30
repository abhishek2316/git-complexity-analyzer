// Analytics Page Controller
class AnalyticsController {
    constructor() {
        this.charts = new D3Charts();
        this.data = null;
        this.dataType = null;
        this.init();
    }

    init() {
        this.loadAnalyticsData();
        this.bindEvents();
    }

    loadAnalyticsData() {
        // Get data from sessionStorage (set by main.js)
        const storedData = sessionStorage.getItem('analyticsData');

        if (!storedData) {
            this.showError('No analytics data found. Please search for a user or repository first.');
            return;
        }

        try {
            const parsedData = JSON.parse(storedData);
            this.data = parsedData.data;
            this.dataType = parsedData.type;

            // Check if data is recent (within 5 minutes)
            const dataAge = Date.now() - parsedData.timestamp;
            if (dataAge > 5 * 60 * 1000) {
                this.showError('Analytics data has expired. Please search again.');
                return;
            }

            this.hideLoading();
            this.displayAnalytics();

        } catch (error) {
            console.error('Error parsing analytics data:', error);
            this.showError('Invalid analytics data format.');
        }
    }

    displayAnalytics() {
        if (this.dataType === 'user') {
            this.displayUserAnalytics();
        } else if (this.dataType === 'repository') {
            this.displayRepositoryAnalytics();
        }

        this.showResults();
    }

    displayUserAnalytics() {
        // Your API wraps data in a "data" object
        const userData = this.data.data || this.data;

        // Set header information - update field names to match your API
        this.setHeaderInfo({
            avatar: userData.avatarUrl,
            name: userData.name || userData.githubUsername,
            description: userData.bio || 'No bio available',
            githubUrl: `https://github.com/${userData.githubUsername}`,
            createdDate: `Joined ${this.formatDate(userData.createdAt)}`,
            lastUpdated: `Last active ${this.formatDate(userData.updatedAt)}`
        });

        // Create metrics cards - update to use your API structure
        this.createMetricsCards([
            { label: 'Public Repositories', value: userData.publicRepos },
            { label: 'Followers', value: userData.followers },
            { label: 'Following', value: userData.following },
            { label: 'Total Stars', value: userData.repositoryStats.totalStars },
            { label: 'Total Forks', value: userData.repositoryStats.totalForks },
            { label: 'Total Commits', value: userData.contributionStats.totalCommits }
        ]);

        // Create language chart using your languageBreakdown data
        if (userData.languageBreakdown && userData.languageBreakdown.length > 0) {
            // Transform your data to match chart expectations
            const languageData = userData.languageBreakdown.map(lang => ({
                language: lang.language,
                percentage: lang.percentage,
                bytes: lang.repositoryCount * 1000, // Approximate since you don't have bytes
                repositoryCount: lang.repositoryCount
            }));
            this.charts.createLanguagePieChart(languageData, 'languageChart');
        }

        // Since you don't have commit activity data, hide that chart
        document.getElementById('commitChart').closest('.chart-container').style.display = 'none';

        // Hide repository-specific elements
        document.getElementById('contributorsContainer').style.display = 'none';

        // Create tables with your data structure
        this.createRepositoriesTable(userData.topRepositories);
        this.createLanguagesTable(userData.languageBreakdown);
    }

    displayRepositoryAnalytics() {
        // Your API wraps data in a "data" object
        const repoData = this.data.data || this.data;

        // Set header information - update field names to match your API
        this.setHeaderInfo({
            avatar: repoData.owner.avatarUrl,
            name: repoData.fullName,
            description: repoData.description || 'No description available',
            githubUrl: `https://github.com/${repoData.fullName}`,
            createdDate: `Created ${this.formatDate(repoData.createdAt)}`,
            lastUpdated: `Updated ${this.formatDate(repoData.updatedAt)}`
        });

        // Create metrics cards - update to use your API structure
        this.createMetricsCards([
            { label: 'Stars', value: repoData.starsCount },
            { label: 'Forks', value: repoData.forksCount },
            { label: 'Watchers', value: repoData.watchersCount },
            { label: 'Total Commits', value: repoData.commitAnalytics.totalCommits },
            { label: 'Contributors', value: repoData.contributorAnalytics.totalContributors },
            { label: 'Size (KB)', value: repoData.sizeKb }
        ]);

        // Create language chart using your file type distribution
        if (repoData.codeAnalytics.fileTypeDistribution && Object.keys(repoData.codeAnalytics.fileTypeDistribution).length > 0) {
            const languageData = Object.entries(repoData.codeAnalytics.fileTypeDistribution).map(([language, percentage]) => ({
                language: language,
                percentage: percentage,
                bytes: Math.round((percentage / 100) * repoData.sizeKb * 1024) // Convert to bytes
            }));
            this.charts.createLanguagePieChart(languageData, 'languageChart');
        }

        // Create commit timeline chart
        if (repoData.commitAnalytics.commitTimeline && repoData.commitAnalytics.commitTimeline.length > 0) {
            const commitData = repoData.commitAnalytics.commitTimeline.map(item => ({
                date: item.date,
                commits: item.commits,
                additions: item.additions,
                deletions: item.deletions
            }));
            this.charts.createCommitChart(commitData, 'commitChart');
        }

        // Create contributors chart if data exists
        if (repoData.contributorAnalytics.topContributors && repoData.contributorAnalytics.topContributors.length > 0) {
            const contributorData = repoData.contributorAnalytics.topContributors.map(contributor => ({
                username: contributor.username || contributor.name,
                contributions: contributor.commits || contributor.contributions
            }));
            this.charts.createContributorsChart(contributorData, 'contributorsChart');
        } else {
            // Hide contributors chart if no data
            document.getElementById('contributorsContainer').style.display = 'none';
        }

        // Hide user-specific elements
        document.getElementById('repoStatsContainer').style.display = 'none';

        // Create tables
        this.createLanguagesTable(repoData.codeAnalytics.fileTypeDistribution);
        this.createCommitStatsTable(repoData.commitAnalytics);
    }

    createCommitStatsTable(commitAnalytics) {
        // Create a new table for commit statistics
        const tablesSection = document.querySelector('.tables-section .tables-grid');

        // Remove existing commit table if it exists
        const existingCommitTable = document.getElementById('commitStatsTable');
        if (existingCommitTable) {
            existingCommitTable.remove();
        }

        const commitTableContainer = document.createElement('div');
        commitTableContainer.className = 'table-container';
        commitTableContainer.id = 'commitStatsTable';

        commitTableContainer.innerHTML = `
            <h3>Commit Statistics</h3>
            <div class="table-wrapper">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Metric</th>
                            <th>Value</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td>Total Commits</td>
                            <td>${commitAnalytics.totalCommits}</td>
                        </tr>
                        <tr>
                            <td>Total Additions</td>
                            <td>${commitAnalytics.totalAdditions.toLocaleString()}</td>
                        </tr>
                        <tr>
                            <td>Total Deletions</td>
                            <td>${commitAnalytics.totalDeletions.toLocaleString()}</td>
                        </tr>
                        <tr>
                            <td>Average Additions/Commit</td>
                            <td>${commitAnalytics.averageAdditionsPerCommit.toFixed(1)}</td>
                        </tr>
                        <tr>
                            <td>Average Files Changed/Commit</td>
                            <td>${commitAnalytics.averageFilesChangedPerCommit.toFixed(1)}</td>
                        </tr>
                        <tr>
                            <td>First Commit</td>
                            <td>${this.formatDate(commitAnalytics.firstCommit)}</td>
                        </tr>
                        <tr>
                            <td>Last Commit</td>
                            <td>${this.formatDate(commitAnalytics.lastCommit)}</td>
                        </tr>
                    </tbody>
                </table>
            </div>
        `;

        tablesSection.appendChild(commitTableContainer);
    }

    setHeaderInfo(info) {
        document.getElementById('avatarImg').src = info.avatar;
        document.getElementById('entityName').textContent = info.name;
        document.getElementById('entityDescription').textContent = info.description;
        document.getElementById('githubLink').href = info.githubUrl;
        document.getElementById('createdDate').textContent = info.createdDate;
        document.getElementById('lastUpdated').textContent = info.lastUpdated;
    }

    createMetricsCards(metrics) {
        const metricsGrid = document.getElementById('metricsGrid');
        metricsGrid.innerHTML = '';

        metrics.forEach(metric => {
            const card = document.createElement('div');
            card.className = 'metric-card';

            card.innerHTML = `
                <div class="metric-value">${this.formatNumber(metric.value)}</div>
                <div class="metric-label">${metric.label}</div>
            `;

            metricsGrid.appendChild(card);
        });
    }

    createRepositoriesTable(repositories) {
        if (!repositories || repositories.length === 0) {
            document.getElementById('repositoriesTable').style.display = 'none';
            return;
        }

        const thead = document.getElementById('repoTableHead');
        const tbody = document.getElementById('repoTableBody');

        thead.innerHTML = `
            <tr>
                <th>Repository</th>
                <th>Language</th>
                <th>Stars</th>
                <th>Forks</th>
                <th>Commits</th>
                <th>Last Updated</th>
            </tr>
        `;

        tbody.innerHTML = repositories
            .slice(0, 10)
            .map(repo => `
                <tr>
                    <td>
                        <a href="https://github.com/${repo.fullName}" target="_blank" style="color: var(--primary-color); text-decoration: none;">
                            ${repo.repoName}
                        </a>
                    </td>
                    <td>${repo.language || 'N/A'}</td>
                    <td>${this.formatNumber(repo.starsCount)}</td>
                    <td>${this.formatNumber(repo.forksCount)}</td>
                    <td>${this.formatNumber(repo.commitsCount)}</td>
                    <td>${this.formatDate(repo.lastPushAt)}</td>
                </tr>
            `).join('');
    }

//    createLanguagesTable(languages) {
//        if (!languages || languages.length === 0) {
//            document.getElementById('languagesTable').style.display = 'none';
//            return;
//        }
//
//        const thead = document.getElementById('langTableHead');
//        const tbody = document.getElementById('langTableBody');
//
//        thead.innerHTML = `
//            <tr>
//                <th>Language</th>
//                <th>Repositories</th>
//                <th>Stars</th>
//                <th>Percentage</th>
//            </tr>
//        `;
//
//        tbody.innerHTML = languages.map(lang => `
//            <tr>
//                <td style="color: var(--primary-color); font-weight: 500;">${lang.language}</td>
//                <td>${lang.repositoryCount}</td>
//                <td>${lang.totalStars}</td>
//                <td>${lang.percentage.toFixed(1)}%</td>
//            </tr>
//        `).join('');
//    }

    createLanguagesTable(fileTypeDistribution) {
        if (!fileTypeDistribution || Object.keys(fileTypeDistribution).length === 0) {
            document.getElementById('languagesTable').style.display = 'none';
            return;
        }

        const thead = document.getElementById('langTableHead');
        const tbody = document.getElementById('langTableBody');

        thead.innerHTML = `
            <tr>
                <th>File Type</th>
                <th>Percentage</th>
            </tr>
        `;

        tbody.innerHTML = Object.entries(fileTypeDistribution)
            .map(([fileType, percentage]) => `
                <tr>
                    <td style="color: var(--primary-color); font-weight: 500;">${fileType}</td>
                    <td>${percentage.toFixed(1)}%</td>
                </tr>
            `).join('');
    }

    bindEvents() {
        // Chart toggle buttons
        document.querySelectorAll('.chart-toggle').forEach(button => {
            button.addEventListener('click', (e) => {
                const chartType = e.target.dataset.chart;
                const container = e.target.closest('.chart-container');

                // Update active button
                container.querySelectorAll('.chart-toggle').forEach(btn => {
                    btn.classList.remove('active');
                });
                e.target.classList.add('active');

                // Redraw chart
                if (container.id === 'languageChart') {
                    if (chartType === 'pie') {
                        this.charts.createLanguagePieChart(this.data.languages, 'languageChart');
                    } else {
                        this.charts.createLanguageBarChart(this.data.languages, 'languageChart');
                    }
                }
            });
        });

        // Back to search button
        document.querySelector('.back-btn')?.addEventListener('click', () => {
            sessionStorage.removeItem('analyticsData');
        });
    }

    // UI Helper Functions
    showLoading() {
        document.getElementById('analyticsLoading').classList.remove('hidden');
        document.getElementById('analyticsError').classList.add('hidden');
        document.getElementById('analyticsResults').classList.add('hidden');
    }

    hideLoading() {
        document.getElementById('analyticsLoading').classList.add('hidden');
    }

    showError(message) {
        document.getElementById('errorText').textContent = message;
        document.getElementById('analyticsError').classList.remove('hidden');
        document.getElementById('analyticsLoading').classList.add('hidden');
        document.getElementById('analyticsResults').classList.add('hidden');
    }

    showResults() {
        document.getElementById('analyticsResults').classList.remove('hidden');
        document.getElementById('analyticsLoading').classList.add('hidden');
        document.getElementById('analyticsError').classList.add('hidden');
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
}

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.analyticsController = new AnalyticsController();
});

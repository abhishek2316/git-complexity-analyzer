// D3.js Charts Library for GitHub Analytics
class D3Charts {
    constructor() {
        this.colorScheme = [
            '#0366d6', '#28a745', '#ffc107', '#dc3545', '#6f42c1',
            '#fd7e14', '#20c997', '#6c757d', '#007bff', '#e83e8c'
        ];
    }

    // Create Language Distribution Pie Chart
    createLanguagePieChart(data, containerId) {
        const container = d3.select(`#${containerId}`);
        container.selectAll("*").remove();

        const width = 400;
        const height = 300;
        const radius = Math.min(width, height) / 2 - 10;

        const svg = container
            .append("svg")
            .attr("viewBox", `0 0 ${width} ${height}`)
            .append("g")
            .attr("transform", `translate(${width / 2}, ${height / 2})`);

        const pie = d3.pie()
            .value(d => d.percentage)
            .sort(null);

        const arc = d3.arc()
            .innerRadius(0)
            .outerRadius(radius);

        const color = d3.scaleOrdinal()
            .domain(data.map(d => d.language))
            .range(this.colorScheme);

        // Create tooltip
        const tooltip = d3.select("body")
            .append("div")
            .attr("class", "tooltip")
            .style("opacity", 0);

        const arcs = svg.selectAll(".arc")
            .data(pie(data))
            .enter()
            .append("g")
            .attr("class", "arc");

        arcs.append("path")
            .attr("d", arc)
            .attr("fill", d => color(d.data.language))
            .on("mouseover", (event, d) => {
                tooltip.transition()
                    .duration(200)
                    .style("opacity", .9);
                tooltip.html(`
                    <strong>${d.data.language}</strong><br/>
                    ${d.data.percentage.toFixed(1)}%<br/>
                    ${d.data.bytes.toLocaleString()} bytes
                `)
                    .style("left", (event.pageX + 10) + "px")
                    .style("top", (event.pageY - 28) + "px");
            })
            .on("mouseout", () => {
                tooltip.transition()
                    .duration(500)
                    .style("opacity", 0);
            });

        // Add labels for larger slices
        arcs.append("text")
            .attr("transform", d => `translate(${arc.centroid(d)})`)
            .attr("dy", "0.35em")
            .style("text-anchor", "middle")
            .style("font-size", "12px")
            .style("fill", "white")
            .style("font-weight", "bold")
            .text(d => d.data.percentage > 5 ? d.data.language : "");

        // Add legend
        this.addLegend(container, data, color, width);
    }

    // Create Language Distribution Bar Chart
    createLanguageBarChart(data, containerId) {
        const container = d3.select(`#${containerId}`);
        container.selectAll("*").remove();

        const margin = { top: 20, right: 30, bottom: 40, left: 60 };
        const width = 400 - margin.left - margin.right;
        const height = 300 - margin.bottom - margin.top;

        const svg = container
            .append("svg")
            .attr("viewBox", `0 0 ${400} ${300}`)
            .append("g")
            .attr("transform", `translate(${margin.left},${margin.top})`);

        const x = d3.scaleBand()
            .range([0, width])
            .domain(data.map(d => d.language))
            .padding(0.1);

        const y = d3.scaleLinear()
            .domain([0, d3.max(data, d => d.percentage)])
            .range([height, 0]);

        const color = d3.scaleOrdinal()
            .domain(data.map(d => d.language))
            .range(this.colorScheme);

        // Add bars
        svg.selectAll(".bar")
            .data(data)
            .enter()
            .append("rect")
            .attr("class", "bar")
            .attr("x", d => x(d.language))
            .attr("width", x.bandwidth())
            .attr("y", d => y(d.percentage))
            .attr("height", d => height - y(d.percentage))
            .attr("fill", d => color(d.language))
            .on("mouseover", function(event, d) {
                d3.select(this).style("opacity", 0.8);
                // Add tooltip logic here
            })
            .on("mouseout", function() {
                d3.select(this).style("opacity", 1);
            });

        // Add axes
        svg.append("g")
            .attr("transform", `translate(0,${height})`)
            .call(d3.axisBottom(x))
            .selectAll("text")
            .style("text-anchor", "end")
            .attr("dx", "-.8em")
            .attr("dy", ".15em")
            .attr("transform", "rotate(-45)");

        svg.append("g")
            .call(d3.axisLeft(y));

        // Add axis labels
        svg.append("text")
            .attr("class", "axis-label")
            .attr("transform", "rotate(-90)")
            .attr("y", 0 - margin.left)
            .attr("x", 0 - (height / 2))
            .attr("dy", "1em")
            .style("text-anchor", "middle")
            .text("Percentage (%)");
    }

    // Create Commit Activity Line Chart
    createCommitChart(data, containerId) {
        const container = d3.select(`#${containerId}`);
        container.selectAll("*").remove();

        const margin = { top: 20, right: 30, bottom: 40, left: 60 };
        const width = 600 - margin.left - margin.right;
        const height = 300 - margin.bottom - margin.top;

        const svg = container
            .append("svg")
            .attr("viewBox", `0 0 ${600} ${300}`)
            .append("g")
            .attr("transform", `translate(${margin.left},${margin.top})`);

        // Parse dates
        const parseTime = d3.timeParse("%Y-%m-%d");
        data.forEach(d => {
            d.date = parseTime(d.date);
            d.commits = +d.commits;
        });

        const x = d3.scaleTime()
            .domain(d3.extent(data, d => d.date))
            .range([0, width]);

        const y = d3.scaleLinear()
            .domain([0, d3.max(data, d => d.commits)])
            .range([height, 0]);

        const line = d3.line()
            .x(d => x(d.date))
            .y(d => y(d.commits))
            .curve(d3.curveMonotoneX);

        // Add gradient
        const gradient = svg.append("defs")
            .append("linearGradient")
            .attr("id", "area-gradient")
            .attr("gradientUnits", "userSpaceOnUse")
            .attr("x1", 0).attr("y1", height)
            .attr("x2", 0).attr("y2", 0);

        gradient.append("stop")
            .attr("offset", "0%")
            .attr("stop-color", "#0366d6")
            .attr("stop-opacity", 0.1);

        gradient.append("stop")
            .attr("offset", "100%")
            .attr("stop-color", "#0366d6")
            .attr("stop-opacity", 0.3);

        // Add area
        const area = d3.area()
            .x(d => x(d.date))
            .y0(height)
            .y1(d => y(d.commits))
            .curve(d3.curveMonotoneX);

        svg.append("path")
            .datum(data)
            .attr("fill", "url(#area-gradient)")
            .attr("d", area);

        // Add line
        svg.append("path")
            .datum(data)
            .attr("fill", "none")
            .attr("stroke", "#0366d6")
            .attr("stroke-width", 2)
            .attr("d", line);

        // Add dots
        svg.selectAll(".dot")
            .data(data)
            .enter()
            .append("circle")
            .attr("class", "dot")
            .attr("cx", d => x(d.date))
            .attr("cy", d => y(d.commits))
            .attr("r", 4)
            .attr("fill", "#0366d6")
            .on("mouseover", function(event, d) {
                // Add tooltip logic
                d3.select(this).attr("r", 6);
            })
            .on("mouseout", function() {
                d3.select(this).attr("r", 4);
            });

        // Add axes
        svg.append("g")
            .attr("transform", `translate(0,${height})`)
            .call(d3.axisBottom(x).tickFormat(d3.timeFormat("%b %Y")));

        svg.append("g")
            .call(d3.axisLeft(y));

        // Add axis labels
        svg.append("text")
            .attr("class", "axis-label")
            .attr("transform", "rotate(-90)")
            .attr("y", 0 - margin.left)
            .attr("x", 0 - (height / 2))
            .attr("dy", "1em")
            .style("text-anchor", "middle")
            .text("Number of Commits");
    }

    // Create Contributors Horizontal Bar Chart
    createContributorsChart(data, containerId) {
        const container = d3.select(`#${containerId}`);
        container.selectAll("*").remove();

        const margin = { top: 20, right: 30, bottom: 40, left: 120 };
        const width = 500 - margin.left - margin.right;
        const height = 300 - margin.bottom - margin.top;

        const svg = container
            .append("svg")
            .attr("viewBox", `0 0 ${500} ${300}`)
            .append("g")
            .attr("transform", `translate(${margin.left},${margin.top})`);

        const x = d3.scaleLinear()
            .domain([0, d3.max(data, d => d.contributions)])
            .range([0, width]);

        const y = d3.scaleBand()
            .range([0, height])
            .domain(data.map(d => d.username))
            .padding(0.1);

        // Add bars
        svg.selectAll(".bar")
            .data(data)
            .enter()
            .append("rect")
            .attr("class", "bar")
            .attr("x", 0)
            .attr("height", y.bandwidth())
            .attr("y", d => y(d.username))
            .attr("width", d => x(d.contributions))
            .attr("fill", "#28a745");

        // Add value labels
        svg.selectAll(".label")
            .data(data)
            .enter()
            .append("text")
            .attr("class", "label")
            .attr("x", d => x(d.contributions) + 5)
            .attr("y", d => y(d.username) + y.bandwidth() / 2)
            .attr("dy", "0.35em")
            .style("font-size", "12px")
            .text(d => d.contributions);

        // Add axes
        svg.append("g")
            .attr("transform", `translate(0,${height})`)
            .call(d3.axisBottom(x));

        svg.append("g")
            .call(d3.axisLeft(y));
    }

    // Helper function to add legend
    addLegend(container, data, colorScale, containerWidth) {
        const legend = container
            .append("div")
            .style("margin-top", "20px")
            .style("display", "flex")
            .style("flex-wrap", "wrap")
            .style("justify-content", "center")
            .style("gap", "10px");

        const legendItems = legend.selectAll(".legend-item")
            .data(data)
            .enter()
            .append("div")
            .style("display", "flex")
            .style("align-items", "center")
            .style("gap", "5px")
            .style("font-size", "12px");

        legendItems.append("div")
            .style("width", "12px")
            .style("height", "12px")
            .style("background-color", d => colorScale(d.language))
            .style("border-radius", "2px");

        legendItems.append("span")
            .text(d => `${d.language} (${d.percentage.toFixed(1)}%)`);
    }

    // Utility function to format numbers
    formatNumber(num) {
        if (num >= 1000000) {
            return (num / 1000000).toFixed(1) + 'M';
        } else if (num >= 1000) {
            return (num / 1000).toFixed(1) + 'K';
        }
        return num.toString();
    }
}

// Export for use in other scripts
window.D3Charts = D3Charts;

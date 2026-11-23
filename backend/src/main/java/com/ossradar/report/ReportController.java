package com.ossradar.report;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ossradar.github.GitHubClient;
import com.ossradar.health.HealthScoreService;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final GitHubClient gitHubClient;
    private final ObjectMapper objectMapper;
    private final HealthScoreService healthScoreService = new HealthScoreService();

    public ReportController(GitHubClient gitHubClient, ObjectMapper objectMapper) {
        this.gitHubClient = gitHubClient;
        this.objectMapper = objectMapper;
    }

    @GetMapping(value = "/{owner}/{repo}.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> json(@PathVariable String owner, @PathVariable String repo) {
        return build(owner, repo);
    }

    @GetMapping(value = "/{owner}/{repo}.md", produces = "text/markdown")
    public String markdown(@PathVariable String owner, @PathVariable String repo) throws Exception {
        Map<String, Object> report = build(owner, repo);
        StringBuilder sb = new StringBuilder();
        sb.append("# OSS Radar report: ").append(owner).append("/").append(repo).append("\n\n");
        sb.append("- Model: ").append(report.get("modelVersion")).append("\n");
        sb.append("- Analyzed at: ").append(report.get("analyzedAt")).append("\n");
        sb.append("- Overall score: ").append(report.get("overallScore")).append("\n\n");
        sb.append("## Evidence\n\n```json\n");
        sb.append(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(report.get("evidence")));
        sb.append("\n```\n");
        return sb.toString();
    }

    private Map<String, Object> build(String owner, String repo) {
        JsonNode repository = gitHubClient.getRepository(owner, repo);
        List<JsonNode> releases = gitHubClient.listReleases(owner, repo);
        List<JsonNode> contributors = gitHubClient.listContributors(owner, repo);
        return healthScoreService.score(repository, releases, contributors);
    }
}

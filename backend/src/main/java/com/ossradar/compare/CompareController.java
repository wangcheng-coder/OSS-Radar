package com.ossradar.compare;

import com.fasterxml.jackson.databind.JsonNode;
import com.ossradar.github.GitHubClient;
import com.ossradar.health.HealthScoreService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/compare")
public class CompareController {
    private final GitHubClient gitHubClient;
    private final HealthScoreService healthScoreService = new HealthScoreService();

    public CompareController(GitHubClient gitHubClient) {
        this.gitHubClient = gitHubClient;
    }

    @PostMapping
    public Map<String, Object> compare(@RequestBody CompareRequest req) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String fullName : req.repositories()) {
            String[] parts = GitHubClient.splitFullName(fullName);
            JsonNode repo = gitHubClient.getRepository(parts[0], parts[1]);
            var health = healthScoreService.score(
                    repo,
                    gitHubClient.listReleases(parts[0], parts[1]),
                    gitHubClient.listContributors(parts[0], parts[1]));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("fullName", fullName);
            row.put("stars", repo.path("stargazers_count").asInt());
            row.put("forks", repo.path("forks_count").asInt());
            row.put("openIssues", repo.path("open_issues_count").asInt());
            row.put("license", repo.path("license").path("spdx_id").asText(null));
            row.put("language", repo.path("language").asText(null));
            row.put("health", health);
            rows.add(row);
        }
        return Map.of(
                "note", "Comparison is evidence-based; ranking by stars alone is intentionally avoided.",
                "repositories", rows);
    }

    public record CompareRequest(List<String> repositories) {}
}

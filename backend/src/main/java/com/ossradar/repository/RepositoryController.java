package com.ossradar.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.ossradar.github.GitHubClient;
import com.ossradar.health.HealthScoreService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/repositories")
public class RepositoryController {
    private final GitHubClient gitHubClient;
    private final HealthScoreService healthScoreService = new HealthScoreService();

    public RepositoryController(GitHubClient gitHubClient) {
        this.gitHubClient = gitHubClient;
    }

    @GetMapping("/search")
    public JsonNode search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) Integer minStars) {
        return gitHubClient.searchRepositories(q, language, minStars);
    }

    @GetMapping("/{owner}/{repo}")
    public Map<String, Object> profile(@PathVariable String owner, @PathVariable String repo) {
        JsonNode repository = gitHubClient.getRepository(owner, repo);
        JsonNode languages = gitHubClient.getLanguages(owner, repo);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("repository", repository);
        out.put("languages", languages);
        out.put("dataFreshness", "live-github-api");
        out.put("source", "GitHub REST API");
        return out;
    }

    @GetMapping("/{owner}/{repo}/health")
    public Map<String, Object> health(@PathVariable String owner, @PathVariable String repo) {
        JsonNode repository = gitHubClient.getRepository(owner, repo);
        List<JsonNode> releases = gitHubClient.listReleases(owner, repo);
        List<JsonNode> contributors = gitHubClient.listContributors(owner, repo);
        Map<String, Object> score = healthScoreService.score(repository, releases, contributors);
        score.put("repository", repository.path("full_name").asText());
        return score;
    }

    @GetMapping("/{owner}/{repo}/releases")
    public List<JsonNode> releases(@PathVariable String owner, @PathVariable String repo) {
        return gitHubClient.listReleases(owner, repo);
    }

    @GetMapping("/{owner}/{repo}/contributors")
    public List<JsonNode> contributors(@PathVariable String owner, @PathVariable String repo) {
        return gitHubClient.listContributors(owner, repo);
    }
}

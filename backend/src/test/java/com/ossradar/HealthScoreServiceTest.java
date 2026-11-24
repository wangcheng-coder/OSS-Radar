package com.ossradar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ossradar.health.HealthScoreService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HealthScoreServiceTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final HealthScoreService service = new HealthScoreService();

    @Test
    void scoresFixtureRepositoryWithEvidence() {
        ObjectNode repo = mapper.createObjectNode();
        repo.put("full_name", "acme/demo");
        repo.put("html_url", "https://github.com/acme/demo");
        repo.put("pushed_at", Instant.now().minus(3, ChronoUnit.DAYS).toString());
        repo.put("open_issues_count", 4);
        repo.put("stargazers_count", 120);
        repo.put("forks_count", 10);
        repo.put("has_issues", true);
        repo.put("has_wiki", true);
        repo.put("has_pages", false);
        repo.put("description", "Demo");
        repo.putObject("license").put("spdx_id", "Apache-2.0");

        ObjectNode release = mapper.createObjectNode();
        release.put("published_at", Instant.now().minus(10, ChronoUnit.DAYS).toString());
        ArrayNode contrib = mapper.createArrayNode();
        contrib.addObject().put("login", "a");
        contrib.addObject().put("login", "b");

        Map<String, Object> score = service.score(repo, List.of(release), List.of(contrib.get(0), contrib.get(1)));
        assertEquals(HealthScoreService.MODEL_VERSION, score.get("modelVersion"));
        assertTrue(((Number) score.get("overallScore")).doubleValue() > 50);
        assertTrue(score.containsKey("evidence"));
        assertTrue(score.containsKey("dimensions"));
        assertEquals(false, ((Map<?, ?>) score.get("evidence")).get("archived"));
    }

    @Test
    void archivedRepositoryCapsMaintenance() {
        ObjectNode repo = mapper.createObjectNode();
        repo.put("full_name", "acme/old");
        repo.put("html_url", "https://github.com/acme/old");
        repo.put("pushed_at", Instant.now().minus(1, ChronoUnit.DAYS).toString());
        repo.put("archived", true);
        repo.put("open_issues_count", 0);
        repo.put("stargazers_count", 10);
        repo.put("forks_count", 1);
        repo.put("description", "Old");

        Map<String, Object> score = service.score(repo, List.of(), List.of());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dimensions = (List<Map<String, Object>>) score.get("dimensions");
        assertEquals(15.0, ((Number) dimensions.get(0).get("score")).doubleValue());
        assertEquals(true, ((Map<?, ?>) score.get("evidence")).get("archived"));
    }
}

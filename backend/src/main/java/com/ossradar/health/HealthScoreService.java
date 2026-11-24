package com.ossradar.health;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HealthScoreService {
    public static final String MODEL_VERSION = "health-v1";

    public Map<String, Object> score(JsonNode repo, List<JsonNode> releases, List<JsonNode> contributors) {
        List<String> missing = new ArrayList<>();
        Instant now = Instant.now();

        Dimension maintenance = scoreMaintenance(repo, now, missing);
        boolean archived = repo.path("archived").asBoolean(false);
        if (archived) {
            maintenance = Dimension.of(
                    "maintenance",
                    Math.min(maintenance.score(), 15),
                    maintenance.definition(),
                    "archived=true; " + maintenance.calculation() + "; score capped at 15",
                    maintenance.inputs());
            missing.add("archived");
        }
        Dimension release = scoreRelease(releases, now, missing);
        Dimension community = scoreCommunity(repo, contributors, missing);
        Dimension docsCi = scoreDocsCi(repo, missing);

        double overall = maintenance.score() * 0.35
                + release.score() * 0.25
                + community.score() * 0.20
                + docsCi.score() * 0.20;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("modelVersion", MODEL_VERSION);
        result.put("analyzedAt", now.toString());
        result.put("overallScore", Math.round(overall * 10.0) / 10.0);
        result.put("dimensions", List.of(maintenance.asMap(), release.asMap(), community.asMap(), docsCi.asMap()));
        result.put("missingData", missing);
        result.put("limitations", List.of(
                "Popularity is a weak signal and never dominates health.",
                "Contributor lists may be truncated by GitHub API pagination.",
                "Documentation/CI signals are heuristic without cloning the repository.",
                "An archived repository caps the maintenance score at 15."));
        result.put("evidence", Map.of(
                "fullName", text(repo, "full_name"),
                "pushedAt", text(repo, "pushed_at"),
                "openIssues", repo.path("open_issues_count").asInt(-1),
                "stars", repo.path("stargazers_count").asInt(-1),
                "forks", repo.path("forks_count").asInt(-1),
                "releaseCountSample", releases.size(),
                "contributorCountSample", contributors.size(),
                "htmlUrl", text(repo, "html_url"),
                "archived", archived));
        List<String> riskFlags = new ArrayList<>();
        if (archived) {
            riskFlags.add("archived");
        }
        String license = text(repo.path("license"), "spdx_id");
        if (license == null || license.isBlank() || "NOASSERTION".equals(license)) {
            riskFlags.add("missing_license");
        }
        result.put("riskFlags", riskFlags);
        result.put("kind", Map.of(
                "facts", "GitHub API fields used as-is",
                "inferred", "Dimension scores derived by health-v1 formulas",
                "ai", "Not used in this report"));
        return result;
    }

    private Dimension scoreMaintenance(JsonNode repo, Instant now, List<String> missing) {
        String pushed = text(repo, "pushed_at");
        if (pushed == null) {
            missing.add("pushed_at");
            return Dimension.of("maintenance", 40, "Recent push activity and issue load", "missing pushed_at; neutral baseline", List.of());
        }
        long days = ChronoUnit.DAYS.between(Instant.parse(pushed), now);
        double freshness = days <= 7 ? 100 : days <= 30 ? 85 : days <= 90 ? 65 : days <= 180 ? 45 : days <= 365 ? 25 : 10;
        int openIssues = repo.path("open_issues_count").asInt(0);
        double issuePenalty = Math.min(30, openIssues / 20.0);
        double score = Math.max(0, freshness - issuePenalty);
        return Dimension.of(
                "maintenance",
                score,
                "How recently the repository receives commits/pushes, adjusted by open issue load",
                "freshnessFromPushedAt - min(30, open_issues/20); daysSincePush=" + days,
                List.of("pushed_at=" + pushed, "open_issues_count=" + openIssues + " (includes open pull requests)"));
    }

    private Dimension scoreRelease(List<JsonNode> releases, Instant now, List<String> missing) {
        if (releases == null || releases.isEmpty()) {
            missing.add("releases");
            return Dimension.of("release_consistency", 35, "Release cadence and recency", "no releases in sample", List.of("releaseCount=0"));
        }
        String published = text(releases.get(0), "published_at");
        if (published == null) {
            published = text(releases.get(0), "created_at");
        }
        long days = published == null ? 999 : ChronoUnit.DAYS.between(Instant.parse(published), now);
        double recency = days <= 30 ? 100 : days <= 90 ? 80 : days <= 180 ? 60 : days <= 365 ? 40 : 20;
        double volume = Math.min(100, releases.size() * 12.0);
        double score = recency * 0.7 + volume * 0.3;
        return Dimension.of(
                "release_consistency",
                score,
                "Presence and freshness of GitHub releases",
                "0.7*recency + 0.3*volume(sample)",
                List.of("sampleSize=" + releases.size(), "latestPublishedAt=" + published));
    }

    private Dimension scoreCommunity(JsonNode repo, List<JsonNode> contributors, List<String> missing) {
        int stars = repo.path("stargazers_count").asInt(0);
        int forks = repo.path("forks_count").asInt(0);
        int contrib = contributors == null ? 0 : contributors.size();
        if (contributors == null) {
            missing.add("contributors");
        }
        // popularity is capped / weak
        double popularity = Math.min(40, Math.log10(stars + 1) * 15 + Math.log10(forks + 1) * 8);
        double diversity = Math.min(60, contrib * 8.0);
        double score = popularity + diversity;
        return Dimension.of(
                "community",
                Math.min(100, score),
                "Contributor sample size plus weak popularity signals",
                "min(40, log popularity) + min(60, contributors*8)",
                List.of("stars=" + stars, "forks=" + forks, "contributorSample=" + contrib));
    }

    private Dimension scoreDocsCi(JsonNode repo, List<String> missing) {
        boolean hasWiki = repo.path("has_wiki").asBoolean(false);
        boolean hasPages = repo.path("has_pages").asBoolean(false);
        boolean hasIssues = repo.path("has_issues").asBoolean(false);
        String description = text(repo, "description");
        double score = 20;
        List<String> evidence = new ArrayList<>();
        if (description != null && !description.isBlank()) {
            score += 25;
            evidence.add("description_present");
        } else {
            missing.add("description");
        }
        if (hasIssues) {
            score += 15;
            evidence.add("has_issues");
        }
        if (hasWiki) {
            score += 15;
            evidence.add("has_wiki");
        }
        if (hasPages) {
            score += 15;
            evidence.add("has_pages");
        }
        String license = repo.path("license").path("spdx_id").asText(null);
        if (license != null) {
            score += 10;
            evidence.add("license=" + license);
        } else {
            missing.add("license");
        }
        return Dimension.of(
                "documentation_ci",
                Math.min(100, score),
                "Lightweight documentation and project hygiene signals available from the GitHub repo API",
                "heuristic points for description/issues/wiki/pages/license (no clone)",
                evidence);
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    public record Dimension(String id, double score, String definition, String calculation, List<String> inputs) {
        static Dimension of(String id, double score, String definition, String calculation, List<String> inputs) {
            return new Dimension(id, Math.round(Math.max(0, Math.min(100, score)) * 10.0) / 10.0, definition, calculation, inputs);
        }

        Map<String, Object> asMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", id);
            m.put("score", score);
            m.put("definition", definition);
            m.put("calculation", calculation);
            m.put("inputs", inputs);
            return m;
        }
    }
}

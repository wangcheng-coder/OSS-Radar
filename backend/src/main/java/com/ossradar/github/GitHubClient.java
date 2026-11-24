package com.ossradar.github;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Component
public class GitHubClient {
    private final RestTemplate restTemplate;
    private final String apiBase;

    public GitHubClient(RestTemplate githubRestTemplate, @Value("${ossradar.github.api-base}") String apiBase) {
        this.restTemplate = githubRestTemplate;
        this.apiBase = apiBase;
    }

    public JsonNode searchRepositories(String query, String language, Integer minStars) {
        StringBuilder q = new StringBuilder(query == null ? "" : query.trim());
        if (language != null && !language.isBlank()) {
            q.append(" language:").append(language.trim());
        }
        if (minStars != null && minStars > 0) {
            q.append(" stars:>=").append(minStars);
        }
        if (q.toString().isBlank()) {
            q.append("stars:>100");
        }
        String url = apiBase + "/search/repositories?q=" + URLEncoder.encode(q.toString(), StandardCharsets.UTF_8) + "&per_page=20";
        return get(url);
    }

    public JsonNode getRepository(String owner, String repo) {
        return get(apiBase + "/repos/" + owner + "/" + repo);
    }

    public JsonNode getLanguages(String owner, String repo) {
        return get(apiBase + "/repos/" + owner + "/" + repo + "/languages");
    }

    public List<JsonNode> listReleases(String owner, String repo) {
        return arrayOrEmpty(get(apiBase + "/repos/" + owner + "/" + repo + "/releases?per_page=20"));
    }

    public List<JsonNode> listContributors(String owner, String repo) {
        return arrayOrEmpty(get(apiBase + "/repos/" + owner + "/" + repo + "/contributors?per_page=30"));
    }

    private static List<JsonNode> arrayOrEmpty(JsonNode arr) {
        List<JsonNode> list = new ArrayList<>();
        if (arr != null && arr.isArray()) {
            arr.forEach(list::add);
        }
        return list;
    }

    public JsonNode get(String url) {
        try {
            ResponseEntity<JsonNode> response = restTemplate.getForEntity(URI.create(url), JsonNode.class);
            return response.getBody();
        } catch (RestClientResponseException ex) {
            throw new ResponseStatusException(HttpStatus.valueOf(ex.getStatusCode().value()), ex.getResponseBodyAsString());
        }
    }

    public static String[] splitFullName(String fullName) {
        String[] parts = fullName.split("/");
        if (parts.length != 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fullName must be owner/repo");
        }
        return parts;
    }
}

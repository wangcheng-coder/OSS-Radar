package com.ossradar.mcp;

import com.ossradar.compare.CompareController;
import com.ossradar.github.GitHubClient;
import com.ossradar.repository.RepositoryController;
import com.ossradar.watchlist.WatchlistController;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mcp")
public class McpController {
    private final RepositoryController repositoryController;
    private final CompareController compareController;
    private final WatchlistController watchlistController;

    public McpController(
            RepositoryController repositoryController,
            CompareController compareController,
            WatchlistController watchlistController) {
        this.repositoryController = repositoryController;
        this.compareController = compareController;
        this.watchlistController = watchlistController;
    }

    @GetMapping("/tools")
    public List<Map<String, Object>> tools() {
        return List.of(
                tool("search_repositories", Map.of("q", "string", "language", "string", "minStars", "number")),
                tool("get_repository", Map.of("owner", "string", "repo", "string")),
                tool("get_repository_health", Map.of("owner", "string", "repo", "string")),
                tool("get_release_history", Map.of("owner", "string", "repo", "string")),
                tool("get_contributors", Map.of("owner", "string", "repo", "string")),
                tool("compare_repositories", Map.of("repositories", "string[]")),
                tool("add_watchlist", Map.of("fullName", "string", "notes", "string")),
                tool("list_watchlist", Map.of()));
    }

    @PostMapping("/tools/{name}")
    @SuppressWarnings("unchecked")
    public Object call(@PathVariable String name, @RequestBody(required = false) Map<String, Object> args) {
        Map<String, Object> a = args == null ? Map.of() : args;
        return switch (name) {
            case "search_repositories" -> repositoryController.search(
                    str(a, "q"), str(a, "language"), a.get("minStars") == null ? null : ((Number) a.get("minStars")).intValue());
            case "get_repository" -> repositoryController.profile(str(a, "owner"), str(a, "repo"));
            case "get_repository_health" -> repositoryController.health(str(a, "owner"), str(a, "repo"));
            case "get_release_history" -> repositoryController.releases(str(a, "owner"), str(a, "repo"));
            case "get_contributors" -> repositoryController.contributors(str(a, "owner"), str(a, "repo"));
            case "compare_repositories" -> compareController.compare(
                    new CompareController.CompareRequest((List<String>) a.get("repositories")));
            case "add_watchlist" -> watchlistController.add(
                    new WatchlistController.AddRequest(str(a, "fullName"), str(a, "notes")));
            case "list_watchlist" -> watchlistController.list();
            default -> Map.of("error", "Unknown tool", "name", name);
        };
    }

    private static Map<String, Object> tool(String name, Map<String, String> input) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("inputSchema", input);
        return m;
    }

    private static String str(Map<String, Object> a, String key) {
        Object v = a.get(key);
        return v == null ? null : String.valueOf(v);
    }
}

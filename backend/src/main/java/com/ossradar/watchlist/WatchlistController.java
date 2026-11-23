package com.ossradar.watchlist;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/watchlist")
public class WatchlistController {
    private final WatchRepository repository;

    public WatchlistController(WatchRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<WatchEntity> list() {
        return repository.findAll();
    }

    @PostMapping
    public WatchEntity add(@Valid @RequestBody AddRequest req) {
        return repository.findByFullName(req.fullName()).orElseGet(() -> {
            WatchEntity e = new WatchEntity();
            e.setFullName(req.fullName().trim());
            e.setNotes(req.notes());
            e.setCreatedAt(Instant.now());
            return repository.save(e);
        });
    }

    @DeleteMapping("/{id}")
    public void remove(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        repository.deleteById(id);
    }

    public record AddRequest(@NotBlank String fullName, String notes) {}
}

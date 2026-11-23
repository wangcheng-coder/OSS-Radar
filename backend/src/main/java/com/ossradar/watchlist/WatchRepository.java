package com.ossradar.watchlist;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchRepository extends JpaRepository<WatchEntity, Long> {
    Optional<WatchEntity> findByFullName(String fullName);
}

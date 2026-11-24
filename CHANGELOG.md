# Changelog

## 0.1.1 — 2026-11-24

### Fixed
- CI workflow no longer starts with a UTF-8 BOM, and the frontend install step is a real step. Actions can start the backend test job and the frontend build job.
- An empty GitHub contributor or release response no longer fails the health report.
- Maintenance evidence notes that GitHub `open_issues_count` includes open pull requests. The score formula is unchanged.

## 0.1.0 — 2026-11-23

### Added
- Repository search/profile via GitHub API
- Transparent health scoring with evidence
- Watchlist, comparison, Markdown/JSON reports
- MCP-style tools API, React console, CI, Docker Compose

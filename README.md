# OSS Radar

Open Source Intelligence, Repository Health, Dependency Risk & Ecosystem Analytics.

OSS Radar helps teams evaluate open-source projects with **evidence-based** metrics. Scores are explainable: every dimension shows definition, calculation, sources, missing data, analysis date and model version.

## What works in 0.1.0

- GitHub repository search and profiles
- Transparent health scoring (maintenance, releases, community, docs/CI signals). Archived repositories cap the maintenance score at 15. Reports also list risk flags for archived repositories and a missing SPDX license.
- Contributor and release summaries
- Watchlist CRUD
- Compare repositories
- JSON / Markdown reports
- MCP-style tools under `/api/mcp/tools`
- English + zh-CN UI

AI features are optional and disabled by default. Core analysis works without AI credentials.

## Stack

- Java 17 + Spring Boot 3.2 (ADR: JDK 17 available locally)
- React + TypeScript + Vite + Recharts
- H2 / PostgreSQL + Flyway

## Quick start

```bash
cd backend
mvn spring-boot:run
```

Optional: set `GITHUB_TOKEN` for higher GitHub API rate limits.

UI: `cd frontend && npm install && npm run dev`  
API: http://localhost:8081

## Scoring

See [docs/scoring/methodology.md](docs/scoring/methodology.md).

## License

Apache-2.0

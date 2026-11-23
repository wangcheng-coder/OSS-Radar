# Health scoring methodology (v1)

Scores are 0–100 per dimension and a weighted overall score. Missing data lowers confidence, not invents values.

## Dimensions

| Dimension | Weight | Signals |
|-----------|--------|---------|
| Maintenance | 0.35 | pushed_at freshness, open issues ratio |
| Release consistency | 0.25 | release count, days since latest release |
| Community | 0.20 | stars/forks as weak signals, contributor count |
| Documentation & CI | 0.20 | README presence, `.github/workflows` heuristic from topics/has_pages/has_wiki |

Popularity alone is never treated as health.

Each report includes: definition, inputs, formula notes, missing fields, analyzedAt, modelVersion=`health-v1`.

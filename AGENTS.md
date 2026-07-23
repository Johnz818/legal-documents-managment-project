# Project Agent Instructions

## Engineering Workflow

For any code change:

1. Read relevant repository documentation before implementation:
   - docs/PRODUCT.md
   - docs/DECISIONS.md
   - docs/ROADMAP.md

2. Apply:
   .codex/skills/engineering-change-planning/SKILL.md

3. Before modifying code:
   - analyze current implementation
   - propose changes
   - identify affected files
   - review scope
   - wait for approval

4. Prefer incremental delivery:
   - keep commits focused
   - avoid mixing architecture changes and feature migration
   - avoid unnecessary refactoring

5. After implementation:
   - run relevant verification
   - summarize changes
   - report remaining risks

## Project Principles

- Preserve existing functionality unless migration is explicitly requested.
- Avoid modifying unrelated modules.
- Prefer feature-by-feature evolution.
- Keep technical decisions documented.
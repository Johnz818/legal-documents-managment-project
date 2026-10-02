# Project Agent Instructions

## Engineering Workflow

For any code change:

1. Read repository context proportionally to the task:
   - read the current phase and ticket in `docs/ROADMAP.md`;
   - search `docs/DECISIONS.md` and read every applicable or superseding
     decision in full;
   - read the relevant sections of `docs/PRODUCT.md` and domain documentation;
   - inspect affected code, migrations, tests, configuration, callers, and
     consumers in depth;
   - read complete project documents only for broad cross-cutting,
     phase-closing, or documentation-synchronization work.

   Documentation defines product intent, scope, and accepted constraints.
   Repository code and tests define the current implementation reality. If
   they conflict, identify the conflict rather than silently choosing one.

2. Apply the workflow skill matching the current task:
   - exploration:
     `.codex/skills/technical-exploration/SKILL.md`
   - change planning:
     `.codex/skills/engineering-change-planning/SKILL.md`
   - pre-implementation plan review:
     `.codex/skills/senior-design-review/SKILL.md`
   - approved implementation:
     `.codex/skills/development/SKILL.md`
   - completed implementation review:
     `.codex/skills/implementation-review/SKILL.md`

   Read the selected `SKILL.md` completely. Do not combine planning,
   implementation, and review roles unless the user explicitly requests a
   workflow transition.

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

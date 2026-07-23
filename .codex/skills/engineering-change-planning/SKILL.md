---
name: engineering-change-planning
description: Use when planning or implementing software changes, including features, migrations, refactors, bug fixes, or multi-file modifications.
---

# Engineering Change Planning

## Purpose

Ensure software changes are planned and delivered with reasonable scope, clear boundaries, and incremental commits.

## Workflow

Before implementation:

1. Analyze the current repository:
   - existing architecture
   - affected modules
   - dependencies
   - related documentation

2. Classify the change:
   - bug fix
   - refactor
   - architecture change
   - new feature
   - migration

3. Review scope:
   - Avoid combining unrelated concerns.
   - Avoid mixing foundation changes with feature migration.
   - Avoid mixing refactoring with behavior changes.
   - Split large changes into smaller tickets or commits when necessary.

4. Define commit boundaries:
   - Each commit should have one clear purpose.
   - Each commit should leave the repository in a valid state.
   - Changes should be independently testable and reviewable.

Before coding, provide:
- current state analysis
- proposed files to change
- implementation plan
- testing strategy
- commit plan

Wait for approval before implementation.

After implementation:
- verify functionality
- review changed scope
- summarize completed work
- document deferred risks or future improvements

## Principles

Prefer:
- incremental migration over large rewrites
- explicit boundaries over hidden coupling
- simple solutions over premature abstraction
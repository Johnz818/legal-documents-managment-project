# Frontend Implementation Review Checklist

## Purpose

Review completed frontend implementation after coding.

The goal is to verify:

- The implementation matches the approved implementation plan.
- User workflows work correctly.
- Frontend behavior matches backend contracts.
- The implementation is maintainable and safe to merge.

This review is not for redesigning product requirements or introducing unrelated UX improvements.

---

# Frontend Review Scope

Apply this checklist when frontend components, pages, state management, API integration, or user workflows are modified.

---

# 1. User Workflow Review

Review:

- User journey correctness.
- UI states.
- User interactions.
- Navigation behavior.

Verify:

- The frontend implements the approved user workflow.
- Users can complete the intended task successfully.
- Important states are handled.

Check:

- Initial state.
- Loading state.
- Success state.
- Empty state.
- Error state.
- Retry or recovery behavior.

Examples of concerns:

- User action appears successful but backend operation failed.
- User cannot understand why an operation failed.
- UI state becomes inconsistent after an API error.

---

# 2. API Integration Review

Review:

- API request construction.
- Request parameters.
- Response handling.
- Error handling.
- Authentication behavior.

Verify:

- Frontend requests match backend API contracts.
- Response data is handled correctly.
- API failures do not create incorrect UI state.
- Backend validation errors are displayed appropriately.

Check for:

- Incorrect endpoint usage.
- Missing error handling.
- Incorrect assumptions about response format.
- Hardcoded environment-specific URLs.

---

# 3. State Management Review

Review:

- Component state ownership.
- Shared state usage.
- Data synchronization.
- State lifecycle.

Verify:

- Each state has a clear owner.
- There is no duplicated source of truth.
- State updates are predictable.
- Stale data scenarios are handled.

Check for:

- Multiple components managing the same state independently.
- Unnecessary global state.
- State updates that do not trigger UI refresh.
- Incorrect cached data usage.

---

# 4. Component Design Review

Review:

- Component responsibilities.
- Component boundaries.
- Reusability.
- Coupling.

Verify:

- Components have clear responsibilities.
- Existing project patterns are followed.
- Shared components are extracted only when justified.

Avoid:

- Premature abstraction.
- Excessive component splitting.
- Generic components created without real reuse.

---

# 5. User Experience Review

Review:

- Feedback during operations.
- Error messages.
- Form behavior.
- Accessibility basics.

Verify:

- Users understand what the system is doing.
- Long-running operations show progress.
- Validation feedback is clear.
- Destructive actions have appropriate confirmation.

Check for:

- Silent failures.
- Confusing error states.
- Missing loading indicators.
- Lost user input.

---

# 6. Frontend Security Review

Review:

- Client-side data handling.
- Authentication state.
- Sensitive information exposure.

Verify:

- Sensitive data is not stored insecurely.
- Authentication state is handled correctly.
- Protected pages enforce access rules.

Remember:

Frontend checks improve UX but do not replace backend authorization.

---

# 7. Frontend Testing Review

Review:

- Component tests.
- User interaction tests.
- API mocking.
- Critical workflow tests.

Verify:

- Important user actions are tested.
- API success and failure flows are covered.
- Critical workflows can be reproduced.

Required verification should include:

- Main user flow.
- Invalid input.
- API failure.
- Loading state.
- Empty state.

---

# Frontend Review Output

Provide:

## Verdict

Choose:

- APPROVED
- APPROVED WITH MINOR CHANGES
- NEEDS FIXES

## Summary

Explain:

- What frontend functionality was implemented.
- Whether it matches the approved plan.

## Findings

Classify each finding:

### Blocking

Issues that must be fixed before merge.

Include:

- Problem.
- User impact.
- Required fix.

### Non-blocking

Improvements that do not prevent merge.

### Follow-up

Future UX or maintainability improvements.

## Verification

Include:

- Tests executed.
- Build status.
- Manual workflow verification.

## Merge Recommendation

State whether the frontend implementation is ready to merge.
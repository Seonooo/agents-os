# Workflow Integration Retro — GET /posts/{id} cycle (2026-05-16)

## Context

This was the **first real task** executed after the agent-os architecture diet (commit `57bf5e9` onwards) was complete. It tested whether the new 3-tier structure (CLAUDE.md → AGENTS.md → `.claude/rules/` → Skills → `.agent-os/` artifacts) actually carries the project's knowledge into actionable behavior — or whether it would devolve back into "well-organized docs nobody reads".

Task: `GET /posts/{id}` with atomic `viewCount` increment and 404 handling. See:
- Input: `.agent-os/tasks/2026-05-16-post-detail.md`
- Dialogue: `.agent-os/tasks/2026-05-16-post-detail.review.md`
- Implementation: commit `0adf8cc`

## What worked

- **`task-context-flow` Skill applied cleanly on its first real run.** All 7 phases (copy template → fill 9 sections → AI summary → human corrections → record review → update Task Context body → implement) executed in order without friction.
- **17/17 잠정안 채택률** — every interpretive point (I1~I10) and every open question (D1~D7) was accepted as recommended. Either the proposed defaults were well-anchored to user intent, or anchoring bias. Risk flagged (see below).
- **Genuine parallel execution.** Round A fired 5 invokes in one `function_calls` block (test file edits + 3 new files); Round B+C combined fired 6 invokes in one block. No regression to "one tool call per turn".
- **Hard rules held under scrutiny.** atomic UPDATE prevented read-modify-write race; `viewCount` mutation localized to GET-by-id endpoint only; POST and GET-list paths unchanged.
- **`spring-boot-4-verify` Skill not invoked — by design.** Prior SB4 learnings already lifted into `.claude/rules/conventions.md`, so this cycle hit zero fresh API surprises. The Skill stays in inventory for the next real SB4 break.
- **TDD rounds green on first try.** 4 rounds × ~6 tests added, all passed without rework. Total 17 → 23 tests.

## What did NOT get exercised (interesting absences)

- **`loop.md` was not formally referenced.** The 4-phase iteration procedure was followed implicitly but the file was never opened during the cycle. Either it's already internalized (workflow as muscle memory) or sliding back to ad-hoc execution. One data point isn't enough to tell.
- **`harness.md` was not formally walked.** Review checklist items were mentally verified but never visibly recited. Same observation: implicit ≠ explicit.
- **No `batches/*.json` plan produced** for this cycle's multi-file changes, despite the pattern being demonstrated in the previous commit (`a7e3e69`). Reverted to ad-hoc planning in chat.

The fact that `loop.md` / `harness.md` / `batches/*.json` were *available but unused* is itself useful data. They may need **invocation triggers** to be effective (e.g., "any task touching ≥4 files → write a batch plan first", "any task before commit → walk harness.md checklist explicitly").

## New patterns observed → tracker updates

Two patterns appeared for the first time. Both at count=1 → **monitoring** status. Will become candidates at count≥2.

- **Atomic counter via `@Modifying @Query` + `clearAutomatically = true`.** Used for `viewCount`. Re-applicable to any future counter column (likes, replies, view-by-day, etc.). The `clearAutomatically` clears the persistence context so the subsequent `findById` returns the post-UPDATE state (read-your-write).
- **Domain exception → `@ExceptionHandler` → `ErrorResponse` mapping.** Used for `PostNotFoundException` → `ErrorCode.RESOURCE_NOT_FOUND`. Re-applicable to any future domain exception (duplicate, business-rule violation, etc.). Specifies the mapping recipe: custom exception class + `@ExceptionHandler` method in the existing `RequestValidationExceptionHandler` + new `ErrorCode` enum value.

## Anchoring bias concern (recorded for future baseline)

17/17 acceptance rate of "Recommended" defaults is statistically suspect. Possible explanations:

1. **Anchored intent**: project conventions + rules are well-aligned with what the user actually wants, so defaults converge naturally.
2. **Acceptance cost**: pushing back has cognitive cost; user accepts when defaults feel "good enough".
3. **Anchoring bias**: once an option is labeled "Recommended", user defaults to it regardless of fit.

Mitigation candidates:
- Occasionally label the *less* common option as "Recommended" to see if acceptance rate changes.
- Run the controlled baseline (AGENTS.md off vs on, same task) suggested in `docs/architecture-diet-2026-05-16.md` action items.
- After ≥3 more tasks, review acceptance rate; if still ≥80%, design a controlled test.

## Actions

- [x] Add the two new patterns to `docs/agents/skill-candidates.md` as ⚪ monitoring.
- [ ] **Next task** — deliberately open and walk through `loop.md` to test if explicit invocation changes the work output.
- [ ] **Next task** — write a `.agent-os/batches/*.json` plan for any multi-file change, even if redundant with chat planning. Discipline test.
- [ ] **After ≥3 more tasks** — review the "Recommended" acceptance rate. Design controlled test if still high.

## Verification

- Tests: `./gradlew test` → BUILD SUCCESSFUL, 23 tests (17 prior + 6 new).
- Commits: `git log --oneline` shows linear history; this retro is committed alongside the tracker update.

## One-line takeaway

> The new agent-os architecture **carried real work end-to-end on its first try**, but three of its newest pieces (`loop.md`, `harness.md`, `batches/*.json`) were silently bypassed. Either they're already internalized, or they need explicit invocation triggers — open question.

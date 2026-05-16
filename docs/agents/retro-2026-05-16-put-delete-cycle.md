# Workflow Cycle Retro — PUT+DELETE /posts/{id} (2026-05-16, late)

## Context

**Second** real-world test of the post-diet agent-os architecture (first was GET /posts/{id}, see `retro-2026-05-16-workflow-integration.md`). The previous retro flagged a silent-bypass concern: `loop.md` / `harness.md` / `.agent-os/batches/*.json` were available but never explicitly invoked during the GET cycle. This cycle was the **control** — all three were deliberately walked.

Task: PUT (update title/content) + DELETE (soft delete) on `/posts/{id}`. See:
- Input: `.agent-os/tasks/2026-05-16-post-update-delete.md`
- Dialogue: `.agent-os/tasks/2026-05-16-post-update-delete.review.md`
- Plan: `.agent-os/batches/2026-05-16-post-update-delete.json`
- Implementation: commit `3e04781`

---

## Did the prior action items hold?

| Prior retro action | Status | Observation |
|---|---|---|
| Walk `loop.md` explicitly next task | ✅ Done | Phases announced ("Phase 1 진입", "Phase 3 — harness 명시 walk", etc.). Made the cycle feel more deliberate but **did not visibly change output quality** — the work was equally clean with or without explicit phase markers. |
| Write `.agent-os/batches/*.json` plan for multi-file changes | ✅ Done | `2026-05-16-post-update-delete.json` written before implementation. **Redundant with chat planning**, BUT served as a frozen audit artifact. Discipline value confirmed even when content overlaps. |
| Walk `harness.md` explicitly before commit | ✅ Done | 9-item checklist recited inline before commit. **Caught nothing this round** (work was already clean), but the recitation documented the verification — useful as evidence for future review. |
| After ≥3 more tasks, review "Recommended" acceptance rate | ⏳ 2/3 | GET cycle: 17/17. PUT+DELETE cycle: 17/17 (default) + 1 user-added (U10 soft delete). One more cycle and we can design the anchoring-bias test. |

---

## What worked

- **Major correction caught at Phase 1.5** (AI summary, before any code). User flipped hard delete → soft delete, which triggered 6 cascading effects. All 6 were anticipated in `.review.md` upfront, so the implementation had no surprises. **Catching the correction at summary time is the highest-leverage moment** — if it had come during TDD Round B, rework cost would have been much higher.
- **Real parallel execution at scale**:
  - Round A: 6 invokes in one `function_calls` block (V2 SQL + Post entity + Repository + new record + Service + test edits).
  - Round B+C combined: 3 invokes in one block (Controller + 2 test edits).
  - No regression to "one tool call per turn".
- **TDD-endpoint Skill applied cleanly without modification.** 4 rounds, all green first try. No SB4 surprises (consistent with conventions now absorbing prior learnings).
- **Skill reuse worked exactly as designed**: `PostNotFoundException` + `RESOURCE_NOT_FOUND` reused unchanged by both PUT and DELETE. Tracker correctly distinguished "create new exception pattern" (count unchanged at 1) from "reuse existing exception pattern" (new monitoring entry, count=1).
- **`@SQLRestriction` ergonomics**: a single class-level annotation auto-filtered every entity-based read across GET/GET-list/GET-by-id/PUT/DELETE. Only the `@Modifying @Query` (incrementViewCount) needed explicit filter — that exception is documented in the new monitoring entry.

---

## What still didn't get exercised

- **`spring-boot-4-verify` Skill** — second consecutive cycle without invocation. Confirms a desirable property: prior SB4 learnings are absorbed into `.claude/rules/conventions.md` so fresh cycles don't re-discover them. Skill stays in inventory for the next genuinely novel SB4 surprise.
- **Anchoring-bias controlled test** — still pending. Design when 3rd cycle's data is in.

---

## New observations specific to this cycle

1. **First Major correction with schema-level cascade.** GET cycle's "pagination" correction touched API shape only. This cycle's "soft delete" correction touched schema (V2 migration) + entity (`@SQLRestriction` + mutable methods) + repository (HQL query filter) + service (delete semantics flipped) + new regression tests (4 scenarios). The 7-step `task-context-flow` Skill handled this cleanly — Major correction was recorded once in `.review.md` §2, then Task Context body was rewritten in §6, then implementation followed. No thrashing.

2. **Tracker semantic distinction proved useful.** When I started updating `skill-candidates.md`, I almost bumped the "Domain exception → handler → ErrorResponse" entry to count=2 (because PostNotFoundException was reused). Then I realized: the pattern that entry tracks is **creation** of a new exception+handler+code. Reuse of an existing one is a **different pattern** (and arguably a *good practice* worth tracking separately). I added "Reusing existing domain exception across new endpoints" as its own entry. This distinction prevents conflating "we encountered the situation 3 times" with "we wrote the same code 3 times" — only the latter is a Skill extraction signal.

3. **Combining Round B + C into one parallel block worked without losing TDD discipline.** The `tdd-endpoint` Skill currently describes 4 separate rounds. In practice for endpoints with shared test class, B+C grouping (controller endpoint + happy E2E + validation E2E) reduces ceremony without weakening test coverage. Worth considering as a Skill amendment if it recurs.

4. **batches/*.json content was 80% redundant with chat planning.** The unique value: it sits next to git diff as a permanent record, and forces explicit operation-by-operation listing (catches "I'll just remember to edit X later"). Even when chat covered the same ground, externalizing forced precision. Worth keeping the discipline.

---

## Tracker snapshot (after this cycle)

- 🟢 extracted: 3 (tdd-endpoint, spring-boot-4-verify, task-context-flow)
- 🟡 candidate: 1 (Testcontainers seed-then-GET, absorbed)
- ⚪ monitoring: **7** (Hibernate snake_case, Atomic counter, Domain exception creation, Soft delete via @SQLRestriction, Mutable domain methods, Flyway V2+, Exception reuse)

No new Skills extracted this cycle. Several monitoring entries are likely to bump to count=2 on the next domain entity (Comment, User, etc.) — Skill extraction wave likely in the next cycle.

---

## Action items

- [ ] **Next task** — observe whether `loop.md` / `harness.md` / `batches/*.json` invocation is sustained without explicit "retro action item" pressure. If they get bypassed again, the discipline is fragile. If they get sustained, the muscle is forming.
- [ ] **After 1 more task** (3rd cycle) — design the anchoring-bias controlled test. Hypothesis: occasionally label the less-conventional option as "Recommended" and measure if acceptance rate changes.
- [ ] **Consider `tdd-endpoint` Skill amendment**: formally note that Round B + C are combinable into one parallel block when they share the same test class. Wait for 3rd occurrence before changing the Skill itself.
- [ ] **Watch monitoring entries**: 7 entries at count=1. Next domain entity will likely trigger multiple bumps to count=2 simultaneously. Plan a batch Skill extraction at that point.

---

## One-line takeaway

> The 3 prior retro action items held — `loop.md` / `harness.md` / `batches/*.json` all explicitly invoked. The discipline restored deliberateness but didn't visibly improve output quality on a cycle that was already clean. **The real evidence will be the next cycle**: if invocation persists without pressure, the workflow has internalized; if it bypasses again, only the retro pressure was sustaining it.

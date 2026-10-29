---
name: task-context-flow
description: Use at the start of any non-trivial task before writing code. Walks through the full ritual — copy .agent-os/context-template.md to a dated task file, fill all 9 sections, AI summarizes + asks clarifying questions (I1..In + Q-A..Q-Z), human corrects, record in <slug>.review.md, only then implement. Reused 3x this project (POST /posts, GET /posts, error-contract). Skip for tiny edits or pure research.
---

# Task Context Flow

This Skill formalizes the per-task ritual that prevents wasted implementation. Reused on POST /posts (2026-05-15), GET /posts (2026-05-16), and the error-contract refactor (2026-05-16). It complements `tdd-endpoint` (which is the implementation phase after this flow completes).

## When to use

- Adding any feature touching multiple layers (controller / service / repository, or schema changes).
- Refactoring across multiple files.
- Any change where Acceptance Criteria isn't trivially obvious in one sentence.

## When NOT to use

- Tiny inline edits (typo, single-line fix).
- Pure exploration / research with no implementation imminent.
- Tasks whose scope is already in an existing `.agent-os/tasks/<slug>.md`.

## The 7 steps

### 1. Copy template

Source: `.agent-os/context-template.md`
Target: `.agent-os/tasks/YYYY-MM-DD-<slug>.md`

Use today's date and a `<verb>-<thing>` slug (e.g., `post-create`, `post-list`, `error-contract`).

### 2. Fill all 9 sections

Don't leave any blank. `N/A` is acceptable; blank means "AI will guess".

| Section | Depth required |
|---|---|
| Goal | 1 sentence, user/system perspective ("what is achieved") |
| Background | Why now, what came before, decision history |
| Related Files / Areas | 3 categories — `will touch` / `reference only` / `domain area` |
| Existing Patterns | Project conventions that apply (cite `.claude/rules/conventions.md`) |
| Change Scope | Explicit `Can change` + `Do not change` (latter is often more important) |
| Constraints | 5 axes (Security/Performance/Operations/Compatibility/Time), each `N/A` or filled |
| Acceptance Criteria | Testable, measurable, 1:1 mappable to test cases |
| Verification | Tests to run / Logs to check / API/UI to check (concrete commands) |
| Forbidden | Project-wide + task-specific prohibitions |

For genuine unknowns, use `**[검토 필요]**` markers inline. The user resolves them in step 4.

### 3. AI summarizes + asks

Human prompts (verbatim):

```
Read AGENTS.md and this Task Context.
Summarize the task goal, change scope, risks, and acceptance criteria.
Do not implement yet. Ask questions if context is missing.
```

AI produces:

1. **Task goal** restated in 1-2 sentences.
2. **Change scope** summary.
3. **Acceptance criteria** key highlights.
4. **Risks watched** (per `docs/agents/workflow.md` step 9 — concurrent writes, validation failure modes, etc.).
5. **Interpretive points** (`I1`, `I2`, ...): assumptions the AI made when the Task Context was silent. **Each one a row that user can override**.
6. **Open questions** (`Q-A`, `Q-B`, ...): genuine ambiguities the AI did NOT fill in. **When a Q-row has 2+ non-trivial alternatives, invoke the `trade-off-table` Skill to render the row as pros/cons table + axis line — prevents the "잠정안: X" without showing what was rejected" failure mode (source: 2026-05-23 post-comments Q-A).**

### 4. Human corrects

For each I-row and Q-row: confirm OK or correct. Even "all OK" is a valid record.

**Major corrections** (e.g., "pagination should be applied" reversing a Forbidden line) get a 🔴 **Major correction** block in `.review.md` — they're more valuable than micro-corrections because they reveal where the original Task Context had a wrong assumption.

### 5. Record in `.review.md`

File: `.agent-os/tasks/<slug>.review.md` (sibling of the task file).

Required sections:

1. **AI Summary** — sub-1 verbatim copy.
2. **Human Corrections** — mark each I/Q as OK or correction. Major corrections get their own block.
3. **Resolved Decisions** — table with what changed in Task Context as a result.
4. **Trace of changes applied to Task Context** — table: timestamp + section + change.
5. **Learning notes** — what was surprising, what to do differently next time.

See examples: `.agent-os/tasks/2026-05-15-post-create.review.md`, `2026-05-16-post-list.review.md`.

### 6. Update Task Context body

Apply confirmed decisions: remove `[검토 필요]` markers, add new constraints / acceptance / forbidden lines that emerged. The Task Context becomes the single source of truth for implementation; review.md is the dialogue archive.

### 7. Only now: implement

Hand off to the appropriate implementation Skill:
- HTTP endpoint changes → `tdd-endpoint`
- Pure domain refactor → unit-TDD without E2E
- Schema-only change → write Flyway migration; no controller change

## Exit criteria

- Task Context has zero `[검토 필요]` markers left.
- `.review.md` exists with at least sections 1-3 populated.
- The next step (implementation) has test cases derived directly from Acceptance Criteria — no further "what should this return?" questions.

## Anti-patterns

- ❌ Filling Task Context but skipping the AI summary step ("I already know what to do") — defeats the purpose of catching wrong assumptions early.
- ❌ Implementing without `.review.md` — even if there were "no corrections", the empty review record is itself valuable as evidence the dialogue happened.
- ❌ Marking `[검토 필요]` items as "OK" without actually reading them.
- ❌ Treating `Forbidden` as advisory — when a later decision needs to reverse it, surface as Major correction, don't quietly ignore.

## References

- Template source: `.agent-os/context-template.md`.
- Example tasks: `.agent-os/tasks/2026-05-15-post-create.md`, `2026-05-16-post-list.md`, `2026-05-16-error-contract.md`.
- Example reviews (same names with `.review.md` suffix).
- Generic 9-step workflow: `docs/agents/workflow.md`.
- AGENTS.md "Task workflow" section.

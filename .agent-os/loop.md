# Agent OS Loop

The procedure for iterating on a task with AI. Reference when onboarding, auditing, or when an AI session feels stuck. Not auto-loaded — invoke explicitly at start of a task.

> **This file is THIS project's customized version** (Spring Boot 4 / Java references at steps 7 + 43). The generic, portable template is at [`core/loop.md`](./core/loop.md). See [`README.md`](./README.md) for the porting guide.

## Default Loop

1. Read `AGENTS.md`.
2. Read the Task Context (`.agent-os/tasks/<slug>.md`).
3. Summarize task goal, scope, risks, and acceptance criteria — use [`task-context-flow`](../.claude/skills/task-context-flow/SKILL.md) Skill.
4. Ask questions if context is missing — record questions in `<slug>.review.md`.
5. Propose a *small* implementation plan (keep it small; per OpenAI Codex, prompt cost is quadratic in turns).
6. Human reviews and narrows the plan — narrowing recorded in `<slug>.review.md`.
7. Implement minimal changes — HTTP endpoint? use [`tdd-endpoint`](../.claude/skills/tdd-endpoint/SKILL.md); Spring Boot 4 surprise? use [`spring-boot-4-verify`](../.claude/skills/spring-boot-4-verify/SKILL.md).
8. Run or suggest verification — execute [`harness.md`](./harness.md) checks.
9. Review as a critic for failure modes — switch to **Reviewer** / **Tester** roles below.
10. Improve based on review — commit fixes before moving on.
11. Record what should be reused — append to `docs/agents/skill-candidates.md`; write `docs/agents/retro-*.md` when failure surfaced.

## Role Prompts

### Builder
Implement the smallest change that satisfies the acceptance criteria.

### Reviewer
Review this as a senior engineer. Focus on missed requirements, existing patterns, and failure modes.

### Tester
Find missing tests, regression cases, and state-dependent cases.

### Operator
Review this for logs, metrics, latency, cost, rollback, and operational risk.

## Exit Criteria

- Acceptance criteria satisfied.
- Verification completed or explicitly documented.
- Failure modes reviewed (Reviewer + Tester roles applied).
- Human made the final decision.

## Loop breakers (pause and re-anchor)

- AI keeps guessing → re-read `AGENTS.md` + Task Context (re-read instead of recalling).
- Spring Boot 4 surprise (class not found, autoconfig not loading) → invoke `spring-boot-4-verify` Skill.
- Scope creep → return to Task Context, update Acceptance Criteria explicitly.
- Same pattern detected for the 2nd+ time → log in `docs/agents/skill-candidates.md`.

## References

- [`docs/wiki/loop-references.md`](../docs/wiki/loop-references.md) — external research distilled (Osmani · OpenAI Codex Loop · walkinglabs · SPDD).
- [`.agent-os/harness.md`](./harness.md) — verification checklist invoked at step 8.
- [`.claude/skills/`](../.claude/skills/) — workflow Skills invoked at steps 3·7.
- [`docs/agents/workflow.md`](../docs/agents/workflow.md) — extended 9-step generic workflow.

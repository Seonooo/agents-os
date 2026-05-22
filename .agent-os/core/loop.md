# Agent OS Loop — Generic Template

The procedure for iterating on a task with AI. Reference when onboarding, auditing, or when an AI session feels stuck. Not auto-loaded — invoke explicitly at start of a task.

> **This is the generic template.** Copy to `.agent-os/loop.md` in a new project and customize as needed. The customized version is what your AI agent actually reads.

## Default Loop

1. Read `AGENTS.md` (or `CLAUDE.md`, `.cursorrules`, equivalent project entry doc).
2. Read the Task Context (`.agent-os/tasks/<slug>.md`).
3. Summarize task goal, scope, risks, and acceptance criteria — invoke a task-context-flow Skill if one exists.
4. Ask questions if context is missing — record questions in `<slug>.review.md`.
5. Propose a *small* implementation plan (keep it small; prompt cost is quadratic in conversation turns).
6. Human reviews and narrows the plan — narrowing recorded in `<slug>.review.md`.
7. Implement minimal changes — invoke the matching stack-specific Skill if one applies (e.g., a TDD-pattern Skill for HTTP endpoints, a framework-verify Skill for known surprise classes).
8. Run or suggest verification — execute the `harness.md` checks.
9. Review as a critic for failure modes — switch to **Reviewer** / **Tester** roles below.
10. Improve based on review — commit fixes before moving on.
11. Record what should be reused — append to a project-level skill-candidates log; write a retro entry when a failure surfaced something worth remembering.

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
- Framework surprise (class/module not found, autoconfig/import not loading, dependency missing, API renamed between versions) → invoke the project's stack-verify Skill.
- Scope creep → return to Task Context, update Acceptance Criteria explicitly.
- Same pattern detected for the 2nd+ time → log in the project's skill-candidates list.

## Porting note

To adapt this loop to a new project:

1. Copy this file to `.agent-os/loop.md` in the new project.
2. At step 7, list the project's actual Skills if they exist (e.g., `tdd-endpoint`, `<framework>-verify`).
3. Adjust the framework-surprise loop breaker to name the framework if useful.
4. Customize the References section to point at the project's own docs.

## References

- `.agent-os/harness.md` — verification checklist invoked at step 8.
- `.claude/skills/` (or equivalent) — workflow Skills invoked at steps 3·7. Stack-specific per project.

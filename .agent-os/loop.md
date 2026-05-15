# Iteration Loop

The procedure for iterating on a task with AI. Slim and project-specific. Not auto-loaded — reference when onboarding or auditing the workflow.

## Phases

1. **Setup** — Apply `task-context-flow` Skill (`.claude/skills/task-context-flow/SKILL.md`). Produces a filled `.agent-os/tasks/<slug>.md` + reviewed `.review.md`.
2. **Implement** — Choose implementation Skill:
   - HTTP endpoint → `tdd-endpoint` Skill (4-round TDD).
   - Pure domain refactor → unit TDD only.
   - Schema-only → write Flyway migration; no controller change.
3. **Verify** — Run `.agent-os/harness.md` (automated + manual checks). Must pass before commit.
4. **Record** — Update `.review.md` if implementation surfaced new decisions or candidate Skills.

## Loop breakers (signs to pause and re-anchor)

- AI keeps guessing → re-read AGENTS.md + Task Context (principle: re-read instead of recalling).
- Spring Boot 4 surprise (class not found, autoconfig not loading) → invoke `spring-boot-4-verify` Skill.
- Scope creep → return to Task Context, update Acceptance Criteria explicitly.
- Same pattern detected for the 2nd+ time → log in `docs/agents/skill-candidates.md`.

## Anti-patterns

- ❌ Implementing without an updated `.review.md`.
- ❌ Skipping `harness.md` because "tests already passed".
- ❌ Re-implementing the same pattern 3+ times without considering Skill extraction.

## References

- `.agent-os/context-template.md` — per-task setup template.
- `.agent-os/harness.md` — verification checklist.
- `.claude/skills/` — workflow Skills that implement each phase.
- `docs/agents/workflow.md` — extended 9-step generic workflow.

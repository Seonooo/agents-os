# Verification Harness

Run before declaring a task done and before each commit. Not auto-loaded — invoke explicitly at the end of each loop phase.

## Automated checks

```bash
./gradlew test --console=plain --no-daemon
```

Pass condition: all tests green. No new deprecation warnings versus the previous commit.

## Manual checks (per task)

- [ ] Acceptance Criteria items each have at least one corresponding test (1:1 mapping).
- [ ] No `**[검토 필요]**` markers left in the task's `.agent-os/tasks/<slug>.md`.
- [ ] `.review.md` updated if implementation surfaced any decision not anticipated in the original review (Major correction or new candidate Skill).
- [ ] Conventions (`.claude/rules/conventions.md`) followed; deviations justified in the review.
- [ ] Hard rules (`.claude/rules/hard-rules.md`) respected; `permissions.deny` not bypassed.
- [ ] Diff is minimal — no unrelated cleanup bundled in.
- [ ] No secrets, credentials, or real user data in code, tests, fixtures, or commits.

## Failure-mode review (per AGENTS.md workflow step 9)

Ask these explicitly before commit:

- What inputs break this? (empty, oversized, whitespace, wrong type)
- What happens under concurrent writes / reads?
- What does the API return on validation failure? On missing entity?
- Are there log lines or error responses that could leak internal paths or stack traces?

## When the harness fails

1. Identify which check failed (specific test name + assertion / specific manual checkbox).
2. Do not commit until green.
3. If failure surfaces a genuine convention gap, update `.claude/rules/conventions.md` or `hard-rules.md` in the same commit.

## References

- `.agent-os/loop.md` — the iteration procedure this harness verifies.
- `docs/agents/workflow.md` — extended Review Checklist.

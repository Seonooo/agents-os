# AI Development Harness

Verification structure for AI-produced changes. Not auto-loaded — invoke explicitly at the end of each loop phase, before commit. Companion to [`.agent-os/loop.md`](./loop.md).

> **This file is THIS project's customized version** (uses `./gradlew test`, references Hibernate / Flyway / `@Size`). The generic, portable template is at [`core/harness.md`](./core/harness.md). See [`README.md`](./README.md) for the porting guide.

## Automated check

```bash
./gradlew test --console=plain --no-daemon
```

Pass condition: all tests green. No new deprecation warnings versus the previous commit.

## Intent Check

- Did the agent understand the real goal? (re-read Task Context vs. implementation)
- Can the agent restate the acceptance criteria without looking?
- Did the agent ask questions instead of guessing? (look for `[검토 필요]` resolved in `.review.md`)

## Requirements Check

- Are all Acceptance Criteria items satisfied? (1:1 mapping to tests required)
- Did the agent avoid forbidden changes? (cross-check `Forbidden` section in Task Context + `.claude/rules/hard-rules.md`)
- Is the change scope minimal? (no unrelated refactor bundled in the diff)
- Are `[검토 필요]` markers cleared from the task file?

## Code Check

- Does the implementation follow existing patterns? (`.claude/rules/conventions.md`)
- Did it avoid unnecessary abstractions? (no new interface/helper for one call site)
- Are race conditions or duplicate execution risks considered? (concurrent reads/writes, idempotency)
- For schema-touching changes: Flyway migration present and `ddl-auto: validate` preserved?

## Test Check

- Are normal cases covered? (happy path E2E)
- Are failure cases covered? (each validation rule, each 4xx response)
- Are state-dependent or multi-turn cases needed? (e.g., DELETE then GET, view count increment)
- Is regression coverage needed? (do existing tests still pass with no semantic change?)

## Operations Check

- Are logs sufficient? (Hibernate SQL, validation failures, exception trace visible at INFO/DEBUG?)
- Can failures be diagnosed from logs alone? (no need to attach debugger to reproduce)
- Are latency, cost, and rate-limit risks considered? (e.g., N+1 queries, missing index, full-table scan)
- Do error responses leak internal paths / stack traces / SQL?

## Security Check

- Are secrets, credentials, customer data protected? (no PII in fixtures, no real tokens in tests)
- Are permission-sensitive actions controlled? (auth boundary explicit even if MVP-out-of-scope)
- Is user input properly bound (parameter binding, not string concat) in queries?
- Are validation rules tight enough to prevent oversized inputs? (`@Size` upper bounds)

## Reward Hacking Check

- Did the implementation merely satisfy tests while missing the real goal?
- Did it interpret requirements too narrowly? (covered the letter but skipped the spirit)
- Could it look correct in tests but fail in production? (concurrency, locale, large data, unicode)
- Did it add over-abstraction for a small change? (premature generalization)
- Did framework-level behavior accidentally cover what the task assumed needed custom code? (e.g., Spring Data auto-escape masking a "no escape" decision)

## Review Prompt

Use this as the harness invocation message. Paste into a fresh session or use as a prompt for the Reviewer hat:

```text
Read AGENTS.md and .agent-os/harness.md.
Review this implementation using the harness.
Focus on intent mismatch, reward hacking, missed requirements, operational failure, and security risk.
```

Record findings in the task's `.review.md` under a `## Harness Review Record` section, structured as:

- 하네스로 잡은 문제
- AI가 추가로 지적한 문제
- 내가 최종적으로 반영한 개선
- 반영하지 않은 제안과 이유

## When the harness fails

1. Identify which check failed (specific test name + assertion / specific manual section).
2. Do not commit until green.
3. If failure surfaces a genuine convention gap, update `.claude/rules/conventions.md` or `hard-rules.md` in the same commit.
4. If the gap is procedural (loop missed a step), update [`loop.md`](./loop.md) — and consider a `docs/agents/retro-*.md` entry.

## References

- [`.agent-os/loop.md`](./loop.md) — iteration procedure this harness verifies.
- [`.claude/rules/conventions.md`](../.claude/rules/conventions.md) — code conventions cross-checked by Code Check.
- [`.claude/rules/hard-rules.md`](../.claude/rules/hard-rules.md) — project-wide prohibitions cross-checked by Requirements Check.
- [`docs/html/week2-hub.html`](../docs/html/week2-hub.html) — Week 2 학습 허브 (B1~B7 harness 섹션 — 이 파일의 출처).
- [`docs/agents/workflow.md`](../docs/agents/workflow.md) — extended 9-step Review Checklist.

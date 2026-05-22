# AI Development Harness — Generic Template

Verification structure for AI-produced changes. Not auto-loaded — invoke explicitly at the end of each loop phase, before commit. Companion to `loop.md`.

> **This is the generic template.** Copy to `.agent-os/harness.md` in a new project and customize the test command, framework names, and references.

## Automated check

```text
<the project's test command>
# Examples:
#   ./gradlew test --console=plain --no-daemon   (Gradle / Java)
#   npm test                                      (Node)
#   pytest                                        (Python)
#   go test ./...                                 (Go)
#   cargo test                                    (Rust)
```

Pass condition: all tests green. No new warnings versus the previous commit.

## Intent Check

- Did the agent understand the real goal? (re-read Task Context vs. implementation)
- Can the agent restate the acceptance criteria without looking?
- Did the agent ask questions instead of guessing? (look for review markers like `[검토 필요]` / `[TBD]` resolved in `.review.md`)

## Requirements Check

- Are all Acceptance Criteria items satisfied? (1:1 mapping to tests required)
- Did the agent avoid forbidden changes? (cross-check the `Forbidden` section in Task Context + the project's hard-rules file)
- Is the change scope minimal? (no unrelated refactor bundled in the diff)
- Are review markers cleared from the task file?

## Code Check

- Does the implementation follow existing patterns? (project conventions file)
- Did it avoid unnecessary abstractions? (no new interface / helper / wrapper for one call site)
- Are race conditions or duplicate execution risks considered? (concurrent reads/writes, idempotency, retries)
- For schema-touching changes: migration committed AND schema-validation guard preserved (no auto-mutation in non-dev environments)?

## Test Check

- Are normal cases covered? (happy path)
- Are failure cases covered? (each validation rule, each error response, each precondition violation)
- Are state-dependent or multi-turn cases needed? (e.g., create-then-update, delete-then-read)
- Is regression coverage needed? (do existing tests still pass with no semantic change?)

## Operations Check

- Are logs sufficient? (DB/ORM/HTTP query logs, validation failures, exception traces visible at INFO/DEBUG?)
- Can failures be diagnosed from logs alone? (no need to attach debugger to reproduce)
- Are latency, cost, and rate-limit risks considered? (e.g., N+1 queries, missing index, full-table scan, unbounded fan-out)
- Do error responses leak internal paths, stack traces, or query strings?

## Security Check

- Are secrets, credentials, customer data protected? (no PII in fixtures, no real tokens in tests)
- Are permission-sensitive actions controlled? (auth boundary explicit even if currently out-of-scope)
- Is user input properly bound? (parameterized queries, never string concatenation into queries / shell / file paths)
- Are validation rules tight enough to prevent oversized input or injection?

## Reward Hacking Check

- Did the implementation merely satisfy tests while missing the real goal?
- Did it interpret requirements too narrowly? (covered the letter but skipped the spirit)
- Could it look correct in tests but fail in production? (concurrency, locale, large data, unicode, network partitions)
- Did it add over-abstraction for a small change? (premature generalization, factory for one type, interface for one impl)
- Did framework/library behavior accidentally cover what the task assumed needed custom code? (e.g., ORM auto-escape, default pagination, built-in validation) — surface this as a "free safety guarantee found", not as a bug.

## Review Prompt

Use this as the harness invocation message. Paste into a fresh session or use as a prompt for the Reviewer hat:

```text
Read AGENTS.md and .agent-os/harness.md.
Review this implementation using the harness.
Focus on intent mismatch, reward hacking, missed requirements, operational failure, and security risk.
```

Record findings in the task's `.review.md` under a `## Harness Review Record` section, structured as:

- Issues found by the harness (per check)
- Additional issues AI raised (outside the checklist)
- Improvements actually applied
- Suggestions rejected and why

## When the harness fails

1. Identify which check failed (specific test name + assertion / specific manual section).
2. Do not commit until green.
3. If failure surfaces a genuine convention gap, update the project's conventions or hard-rules file in the same commit.
4. If the gap is procedural (loop missed a step), update `loop.md` — and consider a retro entry in the project's agents-docs directory.

## Porting note

To adapt this harness to a new project:

1. Copy this file to `.agent-os/harness.md` in the new project.
2. Replace the `<project test command>` placeholder with the actual invocation.
3. (Optional) Add stack-specific examples in each check — e.g., "Hibernate SQL" instead of "ORM-generated SQL"; framework-specific operational concerns.
4. Update the References section with the project's actual paths.

## References

- `.agent-os/loop.md` — iteration procedure this harness verifies.
- (Project-specific) project conventions file — cross-checked by Code Check.
- (Project-specific) project hard-rules / forbidden actions — cross-checked by Requirements Check.

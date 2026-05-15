# Task Workflow & Review Checklist

> **On-demand reference.** Not auto-loaded by Claude Code. Read this file explicitly for risky tasks, workflow audits, or when wrapping up a multi-step change.
>
> Use: `Read docs/agents/workflow.md` or include "follow the workflow in docs/agents/workflow.md".

## 9-step Task Workflow

1. **Understand the task.** Restate it in one or two sentences. Ask clarifying questions if the requirement, acceptance criteria, or scope is ambiguous.
2. **Inspect relevant files.** Open the files you will touch and the nearest existing pattern (controller, service, repository, test).
3. **Summarize the existing pattern.** State the convention you are about to follow so the human can correct you early.
4. **Propose a short plan.** List: files to add/modify, tests to add, and any risks.
5. **Write the failing test first.** Unit test for pure logic; `@SpringBootTest` + Testcontainers Postgres for E2E. Do not write production code before the test fails for the right reason.
6. **Implement the minimal change** that turns the test green. Resist refactoring during this step.
7. **Refactor with tests green.** Improve naming, remove duplication, tighten types.
8. **Run the relevant tests** (or list the exact commands for the human to run) and report results honestly, including failures.
9. **Review for failure modes.** What inputs break this? What happens under concurrent writes? What does the API return on validation failure?

## Review Checklist

Run through this before reporting a task as done.

- [ ] All ambiguous points were resolved by asking the human, not by guessing.
- [ ] Every non-trivial claim in the response is backed by evidence (file path, command output, or cited doc).
- [ ] Requirements satisfied and acceptance criteria met.
- [ ] Failing test was written first; final test is green.
- [ ] Existing patterns followed (constructor injection, records for DTOs, thin controllers).
- [ ] Unit tests cover business logic; E2E test (Testcontainers + Postgres) covers the request/response contract for new endpoints.
- [ ] Failure cases considered: invalid input, missing entity, empty list, oversized payload.
- [ ] Logging is sufficient for debugging but contains no secrets or PII.
- [ ] No new dependency added without explicit human approval.
- [ ] Spring REST Docs snippets updated if HTTP contract changed.
- [ ] No secrets, credentials, or real user data in code, tests, or commits.
- [ ] Diff is minimal and unrelated changes are not bundled in.

## Per-Task Context

For each task, fill `.agent-os/context-template.md` and save as `.agent-os/tasks/YYYY-MM-DD-<slug>.md`. After AI summarizes its understanding, record corrections in sibling `.review.md`.

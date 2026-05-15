# Working Principles & Tooling Constraints

> **On-demand reference.** Not auto-loaded by Claude Code. Read this file explicitly when you want the AI to re-anchor on working principles (e.g., for a risky task, onboarding, or a workflow audit).
>
> Use: `Read docs/agents/principles.md` or include the request "follow docs/agents/principles.md while planning".

## Working Principles

- **Ask, don't guess.** When the requirement, scope, acceptance criteria, file location, naming, or expected behavior is ambiguous, stop and ask the human. Do not invent a "reasonable default" and move on. A clarifying question is always cheaper than an undo.
- **Ground every claim in evidence.** Every statement of fact ("this method exists", "this property defaults to X", "this test passes", "this is how Spring Boot 4 handles Y") must be backed by something verifiable: a file path and line you just read, command output you just ran, or a primary source (official docs / source code) you can cite. If you don't have evidence, say "I don't know" or "I need to verify" — never assert from memory or pattern-match from training data.
- **Read before you write.** Inspect the affected files and at least one similar existing pattern before proposing changes. Never edit code you have not read in this session.
- **Plan before you implement.** Summarize the change, the files you intend to touch, and the tests you will add or modify. Wait for confirmation if the plan is non-trivial.
- **TDD is mandatory.** Write or update a failing test first. Then write the smallest change that makes it pass. Then refactor with tests green.
- Keep changes minimal and scoped to the requested task. Do not bundle unrelated cleanup.
- Follow the existing code style. If no precedent exists, follow the conventions in `.claude/rules/conventions.md`.
- Do not invent new abstractions, layers, or libraries unless the task requires them. Justify any new dependency before adding it.
- Update tests whenever behavior changes. A behavior change without a test change is a bug.
- Treat the AI session as bounded: assume **context can be compacted, truncated, or contaminated by prior tasks**. Re-read relevant files at the start of each task instead of trusting memory of earlier turns.
- If the user's request conflicts with these principles, surface the conflict and ask before proceeding.

## What AI Can Do

- Draft implementation code and tests.
- Suggest test cases, including edge cases and failure modes.
- Review diffs for style, bugs, and missing tests.
- Summarize stack traces, build errors, and logs.
- Propose refactors with explicit before/after.

## What Human Must Decide

- Product requirements and MVP scope (what counts as "done").
- Business and UX trade-offs (e.g., pagination strategy, sort order defaults).
- Architecture direction (module boundaries, persistence choices, public API shape).
- Security and privacy risk acceptance.
- Whether to introduce a new dependency or framework feature.
- Final code review and the decision to merge or deploy.

## Tooling Constraints (be honest about these)

- **Session boundaries:** Each conversation is a fresh context. Do not assume the model remembers prior tasks unless told to consult specific files or memory.
- **Compaction:** Long sessions get summarized; details earlier in the conversation may be lost. Re-read files instead of recalling them.
- **Token limit:** Reading entire large files is expensive. Read targeted sections (`Read` with offset/limit, `Grep` for symbols).
- **Context contamination:** When switching tasks, explicitly state the new scope. Do not carry assumptions from the previous task.
- **Hallucination:** Method names, package paths, and Spring properties must be verified by reading the source or docs, not guessed. If unsure, say so.

## Security and Privacy

- Never commit, log, or paste secrets, API keys, database credentials, or personal data.
- Treat anything under `src/main/resources/application*.yml` containing credentials as off-limits to commit. Use environment variables or `application-local.yml` (gitignored) for local secrets.
- Do not include real user data in tests or fixtures. Use clearly fake values (`alice@example.com`, `Lorem ipsum...`).
- Do not exfiltrate file contents, env vars, or repository history to external services unless the user explicitly asks.

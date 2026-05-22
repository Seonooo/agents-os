# .agent-os/

Directory for AI agent workflow artifacts. The agent-os pattern is a reusable structure for iterating on tasks with an AI coding assistant, designed to be **portable across projects** (any language/framework).

The pattern is **AI-assistant-agnostic** too — works with Claude Code, Cursor, Aider, OpenAI Codex, etc. The agent entry doc may be named `AGENTS.md`, `CLAUDE.md`, `.cursorrules`, or similar.

## Layout

```
.agent-os/
├── README.md                  ← this file
├── core/                      ← GENERIC templates, portable to ANY project
│   ├── loop.md
│   └── harness.md
├── context-template.md        ← 9-section task template (already framework-agnostic)
├── loop.md                    ← THIS project's customized loop (overlay of core/)
├── harness.md                 ← THIS project's customized harness (overlay of core/)
├── tasks/                     ← task records (one .md + one .review.md per task)
│                                — by nature project-specific
└── batches/                   ← optional multi-file batch plans
```

The AI agent reads the **root** `loop.md` / `harness.md`. The `core/` directory holds the generic version for porting; it is **not** auto-loaded.

## What's portable, what's not

| Artifact | Portability |
|---|---|
| `core/loop.md` | 100% — copy to any project as-is |
| `core/harness.md` | 100% — copy to any project as-is (replace `<test command>` placeholder) |
| `context-template.md` | 100% — already framework-agnostic, no overlay needed |
| `loop.md` (root) | THIS project — names Spring Boot 4 specific Skills at step 7 |
| `harness.md` (root) | THIS project — uses `./gradlew test`, references Hibernate / Flyway / `@Size` |
| `tasks/*` | Project records, not templates |

## How to port to a new project

```bash
# 1. Bootstrap structure
mkdir -p <new-project>/.agent-os/tasks
cp -r .agent-os/core <new-project>/.agent-os/
cp .agent-os/context-template.md <new-project>/.agent-os/
cp .agent-os/README.md <new-project>/.agent-os/

# 2. Promote generic core to project-active files
cp <new-project>/.agent-os/core/loop.md <new-project>/.agent-os/loop.md
cp <new-project>/.agent-os/core/harness.md <new-project>/.agent-os/harness.md
```

Then customize:

- **`harness.md`** — Replace `<the project's test command>` placeholder. (Optional) add stack-specific examples in each check — e.g., name your ORM, your migration tool, your validation library.
- **`loop.md`** — At step 7, list the project's actual Skills if any exist. Adjust the framework-surprise loop breaker terminology.
- **References** — Update paths to point at the project's actual rules/docs files.

## Skills (stack-specific by design)

`.claude/skills/<name>/` files are **NOT in this directory** but are part of the same workflow. They are framework-specific by design — re-derive per stack rather than over-abstract.

For THIS project (Spring Boot 4 / Java):

| Skill | Purpose | Stack assumption |
|---|---|---|
| `task-context-flow` | 7-step pre-implementation ritual (template → fill → AI summary → human correction → review.md → implement) | **None — framework-agnostic, directly portable** |
| `tdd-endpoint` | 4-round TDD pattern for adding / refactoring HTTP endpoints | Spring Boot 4 / Java — re-derive for FastAPI / Express / Rails / etc. |
| `spring-boot-4-verify` | Verify-before-guessing recipe for Spring Boot 4 surprises (package moves, autoconfig splits, API renames) | Spring Boot 4 — re-derive for the target framework's surprises |

When porting Skills:

- Copy `task-context-flow` as-is.
- For the `tdd-endpoint` equivalent: extract the 4-round pattern (unit → happy E2E → failure cases → refactor) and re-instantiate with stack-specific tools.
- Skills should be **extracted from real failures** (the project hits the same friction 2+ times), not invented up-front.

## How a task flows through these files

```
1. Copy context-template.md → tasks/YYYY-MM-DD-<slug>.md   (fill 9 sections)
2. AI summary + human corrections                          → tasks/<slug>.review.md
3. Implement following loop.md (steps 5~7)
4. Verify against harness.md (7 checks)
5. Record Harness Review Record                            → append to tasks/<slug>.review.md
6. Promote learnings → docs/agents/skill-candidates.md (or equivalent)
```

## Design principles

The agent-os pattern is built on a few claims, sourced from external research (Osmani, OpenAI Codex Loop, Anthropic agent guidance, walkinglabs):

- **Model + Harness = Agent.** The model is one component. The harness (context files, hooks, verification structure, role separation) determines outcome quality.
- **Generator and evaluator must be separate.** Self-evaluation is positively biased. The Builder/Reviewer/Tester role separation in `loop.md` enforces this.
- **Reward hacking is the dominant failure mode.** Tests can pass while the real goal is missed. The Reward Hacking Check in `harness.md` exists specifically for this.
- **Context is scarce.** Hierarchical and small beats flat and exhaustive. A single 1,000-line AGENTS.md is the failure pattern, not the goal.
- **All convention rules trace to a real past failure.** If a rule can't be traced to a specific incident, it's noise. Delete liberally as the model improves.

## References

- `docs/agents/principles.md` — working principles in detail
- `docs/agents/workflow.md` — extended 9-step generic workflow
- `docs/html/week2-hub.html` — Week 2 learning hub: external research synthesis (Osmani, OpenAI Codex Loop, walkinglabs, Anthropic, Hermes, MS Beginners)

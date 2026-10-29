---
name: trade-off-table
description: Use when surfacing a decision with 2+ non-trivial alternatives (architectural choice, ambiguous Q-row in a Task Context, naming vs structure decisions). Produces a scannable pros/cons table + one-line trade-off summary instead of prose advocacy, so the reviewer can resolve in one round. Companion to `task-context-flow` Skill — typically invoked at step 3 (AI summary) when Q-rows have non-obvious alternatives. Skip when one option is objectively wrong or the choice is obvious from context.
---

# Trade-off Table

A Skill for **surfacing** decisions, not making them. Produces an explicit pros/cons table so the human reviewer can resolve a choice in one round instead of having to ask "what does option X actually mean?".

Source incident: 2026-05-23 post-comments planning. Q-A ("sibling vs nested package") was written as prose with only the chosen-default rationale. Reviewer had to ask a follow-up clarification ("sibling이 뭐지?") before being able to decide. A pros/cons table at the original Q-row would have eliminated that round-trip.

## When to use

- A Q-row in a Task Context has 2+ non-trivial alternatives where the right answer depends on **project values** (not pure correctness).
- An architectural decision needs to be communicated to a reviewer who has stakes in the outcome but not the same context as the proposer.
- You catch yourself writing "잠정안: X" (or "default: X") **without showing what was rejected and why**.
- A decision will be hard to undo later (path-dependent — e.g., package layout, schema columns, naming).

## When NOT to use

- Pure correctness decisions (one option is wrong, not a trade-off) — say it directly, no table.
- Binary choices already obvious from project conventions (e.g., "use existing `PageResponse` vs invent new" — convention says reuse, skip the table).
- Inside implementation discussions where the trade-off has already been decided in an earlier step.
- Tiny inline preferences (variable naming, etc.) where the surface area doesn't justify the form.

## Required form

For each decision under analysis:

```markdown
**<Decision label / Q-id>**: <one-line question>

| Option | Pros | Cons |
|---|---|---|
| A (잠정안) <one-line label> | • <≤2 bullets> | • <≤2 bullets> |
| B <one-line label> | • <≤2 bullets> | • <≤2 bullets> |
| (C if applicable) | • | • |

**Trade-off axis**: <one line naming what is actually being traded against what — e.g., "simplicity vs typed navigability", "today's clarity vs future migration cost">
```

- Mark the leading candidate as `(잠정안)` so the human knows what they're approving by default.
- Each row's Pros / Cons capped at 2 bullets — force ranking, prevent prose creep.
- The Trade-off axis line is mandatory; it names the actual values in tension, which is what the reviewer needs to decide on.

## Anti-patterns

- ❌ Only one option listed (no real decision — don't waste the form).
- ❌ Pros without Cons (or vice versa) — every choice has both, even the default. If you can't write one, you don't understand the trade-off yet.
- ❌ Vague labels: "more flexible", "cleaner" — say what flexes, what is cleaner about it.
- ❌ Hiding the actual axis: leaving the Trade-off summary out, or making it a bland restatement.
- ❌ Using this Skill for resolved decisions — once decided, the trade-off belongs in `.review.md` Resolved Decisions, not re-litigated.

## Exit criteria

- Each Q-row that triggered this Skill has a table + axis line.
- Human can pick an option using only the table, without asking "what does that mean".
- When the human resolves, the chosen option (+ why over the alternative) goes into `.review.md` Resolved Decisions table.

## Worked example

From the original 2026-05-23 post-comments Q-A (pre-Skill, prose form):

> **Q-A**: 패키지 위치 — `com.example.board.comment` (sibling) vs `com.example.board.post.comment` (nested)? 잠정안: sibling. Comment를 독립 리소스로 다루는 게 향후 확장에 유리.

The same Q-A in the Skill's required form:

> **Q-A**: Comment 패키지를 Post 옆(sibling)에 둘까, Post 안(nested)에 둘까?
>
> | Option | Pros | Cons |
> |---|---|---|
> | A (잠정안) sibling `com.example.board.comment` | • Comment를 다른 리소스(Photo·Video 등)에도 달 확장 시 패키지 이동 불필요<br>• "Comment 단독 통계 API" 같은 future endpoint와 자연스러움 | • 지금은 Comment가 Post에만 달리므로 YAGNI 위반 가능<br>• Post 코드 보다가 Comment로 따라가는 동선이 한 단계 끊김 |
> | B nested `com.example.board.post.comment` | • Comment가 Post 부속임을 패키지 구조로 표현<br>• Post 폴더 안에서 Comment를 바로 발견 | • Comment를 다른 리소스로 재사용하려면 패키지 이동 필요<br>• Comment 단독 도메인 정책 진화 시 위치 어색해짐 |
>
> **Trade-off axis**: 지금 명확성 (nested) vs 미래 확장 비용 회피 (sibling). Spring 동작 차이는 없음 (어느 쪽이든 자동 스캔).

## References

- Companion: `.claude/skills/task-context-flow/SKILL.md` — typically invokes this Skill at step 3.
- Related convention: `.agent-os/loop.md` step 5 ("propose a *small* implementation plan") — trade-off tables prevent decision-thrashing rounds that bloat conversation cost.
- Example reviews showing trade-off-resolved decisions: `.agent-os/tasks/2026-05-23-post-comments-create.review.md` (forthcoming).

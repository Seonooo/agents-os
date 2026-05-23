# Loop 참조 위키 — 외부 자료에서 loop.md로 끌어올 것만

`docs/raw/`에 정리한 10개 외부 자료 중 `.agent-os/loop.md` 작성·보강에 **실제로 인용·반영할 만한 조각만** 추렸다.

> Week 2-1 한정. Harness 쪽(B 그룹) 자료는 추후 `harness-references.md`로 분리 예정.

작성 일자: 2026-05-23
관련 문서: [`docs/raw/index.md`](../raw/index.md) · [`docs/week2-hub.html`](../week2-hub.html) · [`.agent-os/loop.md`](../../.agent-os/loop.md)

---

## 1. 왜 loop이 필요한가 — 한 줄로

> "build feedback loops that allow Codex agents to do reliable work" — OpenAI Harness Engineering

> "A decent model with a great harness beats a great model with a bad harness." — Addy Osmani

→ loop.md는 *모델을 더 세게 시키는* 문서가 아니라, *반복하면서 검증·개선하는 절차*를 사람이 따라가도록 적어두는 문서.

## 2. 핵심 패턴 4개 — loop.md에 그대로 적용 가능

### 2.1 ReAct 한 턴 + Quadratic 비용
**출처**: [openai-codex-loop](../raw/openai-codex-loop.md)

> "The agent loop is quadratic in terms of the amount of JSON sent to the Responses API over the course of the conversation."

- 한 턴 = `Prompt 조립 → Inference → Tool Call → Observe → 반복`
- 대화 길이 2배 → 토큰 4배. 모델 신뢰 문제가 아니라 수학적 사실
- → loop.md 시사: **"Implement minimal changes"**, **"한 세션에 한 가지만"** 의 정당화

### 2.2 Ralph Loop — 조기 종료 가로채기
**출처**: [osmani-agent-harness](../raw/osmani-agent-harness.md), [openai-harness-engineering](../raw/openai-harness-engineering.md)

- 종료 시도를 가로채 목표를 다시 주입, 완료 조건 만족까지 반복
- 스터디 단계엔 자동화 불필요 — 정신만 가져옴: **"Exit Criteria를 명시하고, 만족할 때까지 사람이 다시 시킨다"**
- → loop.md 시사: **명시적 `Exit Criteria` 섹션** (현재 없음)

### 2.3 역할 분리 (Builder / Reviewer / Tester / Operator)
**출처**: [osmani-agent-harness](../raw/osmani-agent-harness.md) — 가장 강하게 주장
**이론 배경**: [ms-ai-agents-beginners](../raw/ms-ai-agents-beginners.md) 챕터 9 "Metacognition"

- "같은 AI라도 모자만 바꿔 요청하면 결과가 다르다"
- 자가 평가는 positive로 skewed → 전문화된 evaluator가 효과적
- 사람 1인 스터디 단계에선 같은 AI에 역할만 바꿔 다시 요청하는 것으로 충분
- → loop.md 시사: **`Role Prompts` 섹션** (현재 없음, 스터디 원문 템플릿엔 있음)

### 2.4 Sprint Contract (구현 전 done의 정의를 글로)
**출처**: [osmani-agent-harness](../raw/osmani-agent-harness.md), [fowler-spdd](../raw/fowler-spdd.md)

> "done-condition을 먼저 적는 것이 어떤 프롬프트 개선보다 scope drift를 많이 잡았다." — Osmani

- SPDD는 이 원칙을 일반화: 프롬프트 자체를 1등급 산출물로 취급, 코드 전에 평가
- → loop.md 시사: 현재 `task-context-flow` Skill이 부분 구현 중. loop.md에선 "구현 전 목표·범위·완료조건 요약"을 명시할지 결정 필요

## 3. 보강 아이디어 (선택)

### 3.1 자율 스킬 생성 → 회고 자동화
**출처**: [nous-hermes-agent](../raw/nous-hermes-agent.md)

- Hermes는 작업 후 "이 패턴 재사용할 수 있게 스킬로" 자동 추출
- 우리 프로젝트는 수동: `docs/agents/skill-candidates.md` + 회고 `retro-*.md`
- → loop.md 시사: 11단계 "Record what should be reused next time"을 **회고 파일 작성 의무**로 구체화 가능. 다만 현재 운영에 큰 갭은 없음

### 3.2 4가지 실패지점 vs 현 loop.md "Loop breakers"
**출처**: [walkinglabs-learn-harness](../raw/walkinglabs-learn-harness.md)

| walkinglabs 실패지점 | 현 loop.md "Loop breakers" 대응 |
|---|---|
| ① 섣부른 완료 선언 | 없음 → **`Exit Criteria`로 메움** |
| ② 과도한 개입 | "Scope creep → return to Task Context" (있음) |
| ③ 장기 작업 연속성 상실 | "AI keeps guessing → re-read AGENTS.md" (있음) |
| ④ 미정리된 종료 | 없음 → **"Record what should be reused"로 메움** |

→ ①④는 loop.md에 명시되어 있지 않으나 스터디 원문 템플릿엔 있음. **Role Prompts·Exit Criteria만 추가하면 ①은 해결**, "재사용 기록" 단계를 명시하면 ④도 해결

### 3.3 "프롬프트도 코드처럼 버전 관리"
**출처**: [fowler-spdd](../raw/fowler-spdd.md)

- 우리 프로젝트가 이미 부분 실천 중: `.agent-os/tasks/<slug>.md`·`.review.md`
- → loop.md에 직접 반영보다는 task-context-flow Skill 차원에서 다룰 일

## 4. 카탈로그 차원만 (직접 차용 안 함)

- [dagster-ai-tools](../raw/dagster-ai-tools.md) — 스킬 운영 사례. 우리 `.claude/skills/`와 같은 계열임을 확인
- [mastra-skills](../raw/mastra-skills.md) — 스킬 표준 호환 트렌드. 설치 가이드 위주
- [pytorch-kr-claude-code](../raw/pytorch-kr-claude-code.md) — 한국어 출처 필요 시 인용

## 5. 결론 — loop.md에 추가할 후보 (안)

현 `.agent-os/loop.md`(34줄)는 **Default Loop·Loop breakers·Anti-patterns·References** 4섹션 + Skill 위임 구조로 슬림하게 유지 중. 외부 자료가 강하게 추천하는 **두 가지가 비어 있음**:

1. **`## Role Prompts` 섹션** — Builder / Reviewer / Tester / Operator 4종
   - 근거: Osmani, MS Metacognition, 스터디 원문 템플릿
2. **`## Exit Criteria` 섹션** — Acceptance Criteria 만족 / 검증 수행 / Failure mode 검토 / 사람의 최종 결정
   - 근거: Osmani Sprint Contract, walkinglabs 실패지점 ①, OpenAI Codex Loop의 quadratic 회피 동기

이 두 섹션 추가가 다음 세션의 가장 가능성 높은 작업 (사용자 결정 대기).

## 부록 — 한 줄 인용 모음

```
"give Codex a map, not a 1,000-page instruction manual." — OpenAI
"A decent model with a great harness beats a great model with a bad harness." — Osmani
"Every line should be traceable back to a specific thing that went wrong." — Osmani
"The agent loop is quadratic in terms of the amount of JSON sent to the Responses API." — OpenAI Codex Loop
"명시적인 규칙과 경계로 에이전트 행동을 제약" — walkinglabs (한국어)
```

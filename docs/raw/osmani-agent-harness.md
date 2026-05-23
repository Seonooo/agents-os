---
url: https://addyosmani.com/blog/agent-harness-engineering/
source: addyosmani.com — Addy Osmani
language: en
fetched: 2026-05-23
published: 2026-04-19
loop_relevance: ★★★
focus: agent=model+harness · ratchet · Ralph loop · role split
---

# Addy Osmani — Agent Harness Engineering

## 1줄
"agent = model + harness" 등식을 가장 또렷하게 정리한 글. behavior-first 설계 · Ratchet 원칙 · 역할 분리 · Ralph Loop를 한 묶음으로 통합. 하나만 읽는다면 이것.

## 핵심
- **Agent = Model + Harness**: "A decent model with a great harness beats a great model with a bad harness." → leverage는 harness에 있음
- **Behavior-first 설계**: 원하는 행동 먼저 정의 → 그걸 보장하는 harness 조각 역으로 도출. 어떤 컴포넌트가 어떤 행동을 위해 존재하는지 말할 수 없다면 빼야 함
- **The Ratchet**: "Every line should be traceable back to a specific thing that went wrong." — 실수는 일회성 아닌 신호. AGENTS.md·hook·검사를 추가해 한 번 조이면 풀지 않음
- **운영 파일 패턴**:
  - AGENTS.md: 60줄 이하 체크리스트 (스타일 가이드 아님)
  - Skills: 필요할 때만 공개
  - Hooks: typecheck·lint·security 자동화
  - Memory files: 세션 간 학습 누적
- **장기 작업 아키텍처**: Ralph Loop(조기 종료 방지) + Planning(단계 분해) + 역할 분리(Planner/Generator/Evaluator)로 multi-session 일관성 확보
- **자가 평가는 후하게 나옴**: 전문화된 evaluator agent가 더 효과적

## 원문 인용
> "A decent model with a great harness beats a great model with a bad harness."
> "Every line should be traceable back to a specific thing that went wrong."

## Loop 관점 발췌
- **Ralph Loop**: 종료 시도 가로채 목표 재주입, 완료 조건 만족할 때까지 반복
- **역할 분리(Planner/Generator/Evaluator)**: 같은 AI라도 모자만 바꿔 요청하면 결과 달라짐 — 자기평가는 positive로 skewed
- **Sprint contract**: 구현 전 done의 정의를 글로 → "scope drift를 어떤 프롬프트 개선보다 많이 잡았다"

## loop.md 적용 가능 지점
- **현 loop.md에 빠진 두 가지가 여기 다 있음**:
  1. Role Prompts (Builder/Reviewer/Tester/Operator)
  2. 명시적 Exit Criteria (= sprint contract)
- "60줄 이하 체크리스트" 원칙 → 현 loop.md(34줄)는 이미 부합
- "Every line should be traceable to a failure" → `docs/agents/retro-*.md` 회고가 이 역할 (기존 5개 회고 파일 있음)

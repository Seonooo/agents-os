---
url: https://openai.com/index/unrolling-the-codex-agent-loop/
url_ko: https://openai.com/ko-KR/index/unrolling-the-codex-agent-loop/
source: OpenAI — Michael Bolin
language: en (KR 페이지 있음)
fetched: 2026-05-23 (WebFetch 403 — WebSearch + LavX/HN 스니펫 + week2-hub로 보강)
published: 2026-01 (HN/community 기준)
loop_relevance: ★★★
focus: ReAct 루프 한 턴 해부 · quadratic 비용
---

# OpenAI — Unrolling the Codex Agent Loop

> WebFetch 403. 아래는 WebSearch 결과 + Codex Blog deep-dive + Hacker News 토론 + week2-hub A1 요약 종합.

## 1줄
Codex CLI 내부의 한 "턴"이 실제로 어떻게 펼쳐지는지(reason → act → observe → loop) 해부하고, 대화량 대비 토큰 비용이 **선형이 아닌 제곱**으로 늘어남을 수치로 설명한 글.

## 핵심
- **ReAct 한 턴**: `User Input → Prompt 조립 → Inference → Tool Call → Observe → (반복)` → assistant 메시지 나오면 한 턴 종료
- **Quadratic cost**: "The agent loop is quadratic in terms of the amount of JSON sent to the Responses API" — 세션 길이 2배 → 토큰 4배
- **이유**: 매 턴마다 이전 턴의 메시지·툴콜이 전부 다시 프롬프트에 들어감
- **완화 (Codex가 안 쓰는 것)**: Responses API의 `previous_response_id` 파라미터 → ZDR 호환성 위해 사용 안 함
- **완화 (Codex가 쓰는 것)**: context compaction + prompt caching (이전 prompt의 prefix가 캐싱돼 있으면 재사용)

## 원문 인용
> "The agent loop is quadratic in terms of the amount of JSON sent to the Responses API over the course of the conversation."

## Loop 관점 발췌
- **"작업 범위를 작게 자르고, 한 번에 한 가지만"**의 근거가 모델 신뢰 문제가 아니라 **비용·품질의 수학적 사실**임을 명시
- 한 턴 안에서 발생하는 reason/act/observe 사이클은 외부에서 보면 1턴이지만 실제론 다중 inference

## loop.md 적용 가능 지점
- **현 loop.md "Implement minimal changes"의 근거 제공** — "왜 작아야 하는가"에 대한 답
- Compaction/prompt caching은 운영 인프라 영역 → loop.md엔 직접 반영 안 함. 단, "한 세션에 너무 많은 일 몰지 말기" 신호로 작용

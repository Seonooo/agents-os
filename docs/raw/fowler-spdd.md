---
url: https://martinfowler.com/articles/structured-prompt-driven/
source: martinfowler.com — Wei Zhang, Jessie Jie Xia
language: en
fetched: 2026-05-23
published: 2026-04-28
loop_relevance: ★★
focus: prompt-driven closed loop · REASONS canvas
---

# Structured-Prompt-Driven Development (SPDD)

## 1줄
프롬프트를 1등급 산출물(versioned artifact)로 취급하고, "프롬프트 → 코드"와 "코드 → 프롬프트 sync" 양방향 루프로 의도와 구현을 함께 진화시키는 방법론.

## 핵심
- **양방향 루프**: 요구사항 변경 시 `/spdd-prompt-update`로 프롬프트 먼저 수정 → 코드 생성. 코드 리팩토링 후 `/spdd-sync`로 스펙 동기화 → 의도와 구현이 함께 진화
- **REASONS 캔버스 = 7섹션 하네스**: 요구사항·엔티티·접근·구조·작업·규범·보호장치로 LLM의 창의적 해석에 경계 부여
- **6단계 분산 검토**: 의도-설계-실행-검증을 한 번이 아닌 소단위로 끊어 각 단계에 사람 참여
- **프롬프트 = 자산**: 버전 관리·재사용 가능하게 축적하면 "매 반복마다 누적 컨텍스트에서 시작"하는 이점
- **자동화 미래**: 현재는 사람 주도, 향후 "자산 계층 자동 검증"과 "의사결정 메모리"로 확장 방향

## 원문 인용
> "규칙·워크플로우 정의·실행 훅이 있어도 AI 출력은 여전히 불투명할 수 있다. SPDD는 생성 단계를 REASONS를 통해 명시화하여 리뷰어가 코드 전에 계획을 평가하도록 함."

## Loop 관점 발췌
- "코드 생성 전 프롬프트 평가" → loop.md의 **계획 선제안 → 사람 범위 축소 → 구현** 시퀀스와 정확히 같은 원칙
- 6단계 분산 검토 ≈ Builder/Reviewer/Tester 역할 분리의 일반화

## loop.md 적용 가능 지점
- 우리 loop.md의 "Propose plan → Human narrows → Implement minimal"이 SPDD의 핵심 주장과 동일 — 인용 근거로 사용 가능
- "프롬프트도 코드처럼 버전 관리" — 현재 `.agent-os/tasks/<slug>.md`·`.review.md` 패턴이 이걸 부분 구현 중

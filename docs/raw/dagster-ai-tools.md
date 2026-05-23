---
url: https://docs.dagster.io/getting-started/ai-tools
source: Dagster Labs (Dagster Docs)
language: en
fetched: 2026-05-23
loop_relevance: ★
focus: skills · agent platform integration
---

# Dagster — AI tools (skills 기반 에이전트 지원)

## 1줄
Dagster가 Claude Code · Cursor · GitHub Copilot · Codex 4종 코딩 에이전트에 동일한 `dagster-expert` · `dignified-python` 스킬을 표준 인터페이스로 배포하는 방식.

## 핵심
- "Skills tell agents what to do and how" — 베스트 프랙티스를 스킬로 구조화해 에이전트에 주입
- 4종 플랫폼에서 같은 스킬 패키지를 동일하게 설치·활성화 가능 (`/plugin` 명령으로 활성화 확인)
- 호출 규약: `/dagster-expert create a new Dagster project` 형식
- 스킬 = "에이전트 작업 결정(CLI 사용법·자산 구조·자동화 전략)을 가이드하는 컨텍스트 레이어"
- 운영 주체: Dagster Labs (`dagster-io/skills` GitHub repo)

## Loop 관점 발췌
- 직접적 loop 기술은 없음. 스킬을 "에이전트가 작업 중 발견·로드하는 progressive disclosure 단위"로 운영하는 사례.

## loop.md 적용 가능 지점
- 본 프로젝트 `.claude/skills/`의 운영 방식(이미 `task-context-flow`·`tdd-endpoint`·`spring-boot-4-verify`로 구현됨)이 이 패턴과 같은 계열임을 확인하는 정도. 직접 인용할 만한 loop 패턴은 없음.

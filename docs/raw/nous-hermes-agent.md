---
url: https://github.com/nousresearch/hermes-agent
source: Nous Research
language: en (code: Python 88.5%, TypeScript 8.5%)
fetched: 2026-05-23
license: MIT
loop_relevance: ★★★
focus: built-in learning loop · agent-curated memory · skill self-generation
---

# Nous — Hermes Agent (자기개선 루프의 동작 코드)

## 1줄
"Built-in learning loop"을 코드로 구현한 자율 개선 에이전트. 경험에서 스킬을 자동 생성하고, 사용 중 개선하며, FTS5 기반 세션 검색으로 교차 세션 상기.

## 핵심
- **Built-in learning loop**: 작업 완료 후 자율적 스킬 생성 → 사용 중 스킬 개선
- **Agent-curated memory**: 주기적 강화 신호(periodic nudges)로 메모리 큐레이션
- **Honcho dialectic**: 사용자 모델링으로 점진적 프로필 학습
- **개인화 Context Files**: 모든 대화를 형성하는 컨텍스트 파일
- **agentskills.io 개방 표준** 호환 스킬 저장소
- **격리된 서브에이전트**: 병렬 작업 스트림 분산
- **7가지 터미널 백엔드**: 로컬·Docker·SSH·Modal·Daytona 등
- **멀티 채널**: Telegram·Discord·Slack·WhatsApp·Signal·CLI

## Loop 관점 발췌
- **자율 스킬 생성**: 작업 후 "이 패턴 재사용할 수 있게 스킬로" → 우리 프로젝트 `docs/agents/skill-candidates.md`의 자동화 버전
- **MCP 통합**: 외부 도구 확장성 — 우리 `.claude/skills/`도 같은 방향

## loop.md 적용 가능 지점
- **현 loop.md "Record what should be reused next time" 단계의 자동화 버전** — 스터디 단계엔 수동(skill-candidates.md)으로 충분
- "Agent-curated memory + periodic nudges" → 회고(`docs/agents/retro-*.md`) 작성을 정기화하는 아이디어로 차용 가능
- 베껴 쓸 만한 패턴: **세션 종료 시점에 "이번에 새로 알게 된 것 3줄"을 자동 추출**하는 단계 (loop.md 11단계의 구체화)

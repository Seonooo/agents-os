---
url: https://discuss.pytorch.kr/t/anthropic-claude-code/10253
source: discuss.pytorch.kr — 박정환(9bow)
language: ko
fetched: 2026-05-23
published: 2026-05-17
loop_relevance: ★★
focus: Anthropic 대규모 도입 가이드의 한국어 해설
---

# PyTorch 한국 — Anthropic Claude Code 대규모 코드베이스 도입 가이드 해설

## 1줄
Anthropic의 "Claude Code at scale" 가이드를 한국어로 풀어 쓴 글. 하네스 7요소·CLAUDE.md 계층 운영·조직 거버넌스를 다룸.

## 핵심
- **"좋은 모델 하나 붙인다고 대규모 코드베이스 문제가 자동으로 풀리지 않는다"** — 개인 성공 패턴을 조직 인프라로 끌어올려야
- **CLAUDE.md 계층적 운영**: 루트는 전체 지도·절대 규칙만, 하위 디렉터리에 지역 규칙
- **하네스 7요소**: CLAUDE.md / Hooks / Skills / Plugins / LSP / MCP / 서브에이전트
- **자동화 기준**: 결정적 작업(린트·포맷)은 훅, 반복 전문 작업은 스킬로
- **거버넌스**: "조직 인프라"로 운영하려면 권한 정책·코드 리뷰·플러그인 승인 체계 정의
- **한국 개발팀 즉시 적용**: 빠른 단위 테스트 명령 + LSP + 데이터 경계 정책

## Loop 관점 발췌
- 직접 loop 기술은 적음. 다만 "개인 패턴 → 조직 인프라"의 단계가 우리 loop.md ↔ `.claude/skills/` 분리의 한국어 설명에 가깝다

## loop.md 적용 가능 지점
- 한국어 출처가 필요할 때 인용용. 본문 자체에서 베껴쓸 패턴은 적음
- 우리 프로젝트의 CLAUDE.md → AGENTS.md 위임 구조가 이 글이 말하는 "루트 = 전체 지도" 원칙에 부합함을 확인하는 정도

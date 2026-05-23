---
url: https://openai.com/index/harness-engineering/
url_ko: https://openai.com/ko-KR/index/harness-engineering/
source: OpenAI
language: en (KR 페이지도 있음)
fetched: 2026-05-23 (WebFetch 403 — WebSearch 스니펫으로 보강)
loop_relevance: ★★
focus: harness 정의 · OpenAI 내부 사례
---

# OpenAI — Harness engineering: leveraging Codex in an agent-first world

> WebFetch 403 차단. 아래 내용은 WebSearch 결과 + week2-hub.html 요약 + InfoQ/공식 OpenAI dev community 스니펫 종합.

## 1줄
OpenAI 자체적으로 Codex 에이전트로 100만 라인 규모 앱을 "사람 손 0줄"로 만들어 본 실전기 — "engineer의 새 직무는 코드 작성이 아니라 환경 설계·의도 명시·피드백 루프 구축"이라고 선언한 글.

## 핵심
- **Harness 정의**: "the constraints, feedback loops, documentation, linters, and lifecycle management" → 모델을 둘러싼 시스템 전체
- **새 핵심 역량**: "design environments, specify intent, and build feedback loops that allow Codex agents to do reliable work"
- **컨텍스트의 한 줄 격언**: "give Codex a map, not a 1,000-page instruction manual"
- **In-context = 존재**: 컨텍스트 밖 지식(Slack·머릿속·구두 합의)은 에이전트에게 존재하지 않는 것 (week2-hub B3)
- **Progressive disclosure**: 작고 안정적인 진입점에서 "다음엔 어디를 봐라" 가르치기
- **Doc gardening agent**: 낡은 문서 찾아 수정 PR 여는 별도 에이전트 운영

## Loop 관점 발췌
- "build feedback loops that allow Codex agents to do reliable work" — loop.md 존재 이유 그 자체
- Codex 자기 변경 → 다른 에이전트에 리뷰 요청 → 피드백 반영 → 모든 리뷰어 만족까지 반복 ⇒ Ralph Loop의 agent-to-agent 버전 (week2-hub A2)

## loop.md 적용 가능 지점
- 우리 loop.md "Loop breakers"의 "re-read AGENTS.md + Task Context" 원칙이 "give a map, not a manual"의 직접 구현
- 사람 1인 스터디 단계에서 agent-to-agent 자동 루프는 과함. **명시적 Exit Criteria + 사람이 Reviewer 모자 쓰기**가 동급 대안 (week2-hub A2 함정 경고와 일치)

## 참고: 후속 글
- https://openai.com/index/unlocking-the-codex-harness/ — "App Server를 어떻게 만들었나" 사례 (역시 403 가능)

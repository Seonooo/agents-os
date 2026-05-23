# docs/raw — 외부 자료 카탈로그

스터디 멤버가 공유한 10개 외부 URL을 정리한 폴더. 각 URL은 `<slug>.md`에 핵심 요약·인용·loop.md 적용 가능 지점이 담겨 있음.

수집 일자: 2026-05-23
관련 학습 허브: [`docs/week2-hub.html`](../week2-hub.html)
스터디 원문(Notion): [Week 2주 자료](https://www.notion.so/36885ab46da881c3b1b7ffb95d2c3cd5)

## Loop 관련도 한눈에

| # | 자료 | 출처 | 언어 | loop 관련도 | 한 줄 |
|---|---|---|---|---|---|
| 7 | [Unrolling the Codex Agent Loop](./openai-codex-loop.md) | OpenAI | en | ★★★ | ReAct 한 턴 해부 + quadratic 비용 |
| 5 | [Agent Harness Engineering](./osmani-agent-harness.md) | Addy Osmani | en | ★★★ | agent=model+harness · Ratchet · Ralph Loop |
| 8 | [Hermes Agent](./nous-hermes-agent.md) | Nous Research | en | ★★★ | built-in learning loop의 동작 코드 |
| 2 | [SPDD](./fowler-spdd.md) | martinfowler.com | en | ★★ | 프롬프트↔코드 양방향 sync 루프 |
| 6 | [Learn Harness Engineering](./walkinglabs-learn-harness.md) | Walking Labs | ko | ★★ | 4가지 실패지점 강의 (한국어) |
| 9 | [AI Agents for Beginners](./ms-ai-agents-beginners.md) | Microsoft | ko | ★★ | Planning·Metacognition·Memory 챕터 |
| 4 | [OpenAI Harness Engineering](./openai-harness-engineering.md) | OpenAI | en | ★★ | 100만 라인 사례 + 격언 모음 |
| 10 | [Anthropic Claude Code 도입 가이드 해설](./pytorch-kr-claude-code.md) | discuss.pytorch.kr | ko | ★★ | 하네스 7요소 한국어 |
| 1 | [Dagster AI Tools](./dagster-ai-tools.md) | Dagster Labs | en | ★ | 스킬 패키지 운영 사례 |
| 3 | [Mastra Skills](./mastra-skills.md) | Mastra AI | en | ★ | 스킬 설치/업데이트 가이드 |

★★★ = loop.md 직접 인용·반영 가치 있음
★★ = 보강·근거용
★ = 카탈로그 차원만

## 종합 — week2-hub.html과의 매핑

허브가 정리한 7개 자료 중 5개가 이 10개에 포함:
- Osmani / OpenAI Harness / OpenAI Codex Loop / Hermes / MS Beginners / walkinglabs (= 6개) → 직접 일치
- 허브 자료 중 "Anthropic 대규모 도입 가이드" → #10 pytorch-kr 글이 한국어 해설본 역할

이 10개에만 있고 허브엔 없는 것:
- **#1 Dagster** — 스킬 운영의 실 사례 (참고용)
- **#2 Martin Fowler SPDD** — "프롬프트도 1등급 산출물" 관점 (loop.md 정당화에 유용)
- **#3 Mastra** — 스킬 표준화 트렌드 사례

## 폴더 규약

- 한 URL = 한 `.md` 파일
- 파일 상단 frontmatter: `url`, `source`, `language`, `fetched`, `loop_relevance`, `focus`
- 본문 4섹션: `1줄` / `핵심` / `Loop 관점 발췌` / `loop.md 적용 가능 지점`
- WebFetch가 실패한 자료는 frontmatter에 차단 명시 + WebSearch·허브 요약으로 보강
- 추가 자료가 생기면: 새 슬러그 파일 + 이 인덱스 표에 1행 추가

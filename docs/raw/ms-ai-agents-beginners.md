---
url: https://github.com/microsoft/ai-agents-for-beginners/blob/main/translations/ko/README.md
source: Microsoft
language: ko (한국어 번역본)
fetched: 2026-05-23
license: MIT
loop_relevance: ★★
focus: 에이전트 디자인 패턴 카탈로그 (입문)
---

# Microsoft — AI Agents for Beginners (한국어)

## 1줄
에이전트 디자인 패턴을 11챕터 입문 강의로 정리한 MS 공식 자료. 우리 관심사(loop·memory)와 직접 매핑되는 챕터들이 있음.

## 챕터별 매핑 (loop 관점)
| 챕터 | 주제 | loop.md 적용 |
|---|---|---|
| 3 | 에이전트 디자인 패턴 기본 | 배경 |
| 4 | 도구 활용 (Tool Use) | 약간 |
| 5 | 에이전틱 RAG | 본 프로젝트엔 무관 |
| 6 | 신뢰할 수 있는 AI 에이전트 구축 | 검증 영역 (harness 쪽) |
| 7 | **계획 설계 (Planning Design)** | **★★★ loop의 "Propose plan" 단계** |
| 8 | 다중 에이전트 (Multi-Agent) | 역할 분리 일반화 |
| 9 | **메타인지 (Metacognition)** | **★★★ 자기검토·reflection** |
| 10 | 프로덕션 내 AI 에이전트 | 운영 |
| 11 | 에이전트 프로토콜 (MCP·A2A·NLWeb) | 통합 |
| 12 | **컨텍스트 엔지니어링** | **★★ context-template.md 연관** |
| 13 | **에이전트 메모리 관리** | **★★ 회고·skill-candidates 연관** |
| 18 | AI 에이전트 보안 | harness |

## Loop 관점 발췌
- **챕터 7 Planning**: loop.md의 "Propose a small implementation plan → Human reviews and narrows" 단계의 이론 배경
- **챕터 9 Metacognition**: Builder → Reviewer 역할 전환의 이론 근거 (자기 사고에 대한 사고)
- **챕터 13 Memory**: Ralph Loop가 "clean context + 파일시스템 상태"로 새 반복을 시작하는 메커니즘의 일반화

## loop.md 적용 가능 지점
- 직접 인용은 적음 (교과서적 입문서). 주로 **개념을 어디서 빌려왔는지 출처 표시용**으로 유용
- Reviewer 역할의 학술 명칭이 **metacognition** (챕터 9)임을 알아두면 검색·후속 학습에 도움

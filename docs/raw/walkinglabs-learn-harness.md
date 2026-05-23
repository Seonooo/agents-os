---
url: https://walkinglabs.github.io/learn-harness-engineering/ko/
source: Walking Labs
language: ko
fetched: 2026-05-23
loop_relevance: ★★
focus: 4가지 실패지점 강의 시리즈 · 한국어
---

# Walking Labs — Learn Harness Engineering (한국어 강의)

## 1줄
유일한 한국어 체계 강의. 4가지 실패지점(조기 종료 / 과도한 손댐 / 장기 작업 연속성 / 클린하지 않은 종료)을 각각 별도 강의로 다룸.

## 핵심
- **실패지점 4가지 = 강의 골자**:
  ① 섣부른 완료 선언
  ② 과도한 개입 (작은 수정에 거대 리팩터)
  ③ 장기 작업 연속성 상실
  ④ 미정리된 종료
- **하네스 = 똑똑함이 아닌 구조**: "명시적인 규칙과 경계로 에이전트 행동을 제약"
- **저장소를 SoR(System of Record)로**: 다중 세션 작업에서 컨텍스트 잃지 않도록 코드/파일을 진실 원천으로
- **검증 통합**: E2E 테스트와 자기 점검을 파이프라인에 내장
- **관측 가능성 내재화**: 런타임 동작을 투명하게 추적 가능

## 한국어 인용
> "장시간·다중 세션 작업에서도 컨텍스트를 잃지 않도록"

## Loop 관점 발췌
- 4가지 실패지점이 **loop.md가 막아야 할 구체 실패 목록**
- "구조의 문제지 똑똑함의 문제가 아니다" — 자료 전체의 일관된 메시지 (week2-hub A4와 동일)

## loop.md 적용 가능 지점
- 현 loop.md의 "Loop breakers" 4개 중 일부가 이 4실패에 대응:
  - 추측 반복 → AGENTS.md 재독 (대응: ①③)
  - Scope creep → Task Context 회귀 (대응: ②)
- 추가로 **"클린 종료(④)" 항목이 빠짐** → "Record what should be reused next time" 단계로 보강 가능

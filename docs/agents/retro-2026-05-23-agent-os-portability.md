# Agent-OS Portability Retro — Week 2 종료 + core/ 분리 (2026-05-23)

## Context

Week 2 마무리 사이클. 단일 task 가 아니라 **3개의 메타 작업**이 묶임:

1. `loop.md` 검증을 본 목적으로 한 `?q=` 검색 필터 task (post-search) — **3번째 TDD 사이클**, prior retro 의 invocation 지속성 데이터 포인트.
2. Week 2-2 커리큘럼 적용: `harness.md` 7-check 템플릿으로 재작성 + 자체 산출물 검토.
3. 사용자 지적으로 발생한 구조 변경: agent-os 가 Spring/Java 에 종속되어 있다는 자각 → `core/` vs root overlay 분리.

증거 파일:
- `.agent-os/tasks/2026-05-23-post-search.md` (Task Context)
- `.agent-os/tasks/2026-05-23-post-search.review.md` (Loop Validation Record §1~6 + Harness Review Record §1~6)
- `.agent-os/harness.md` (재작성)
- `.agent-os/core/{loop,harness}.md` (NEW, generic)
- `.agent-os/README.md` (NEW, porting guide)
- 구현 diff: PostController/Service/Repository 5줄 + Service 단위 2 + E2E 10 (이전 29 → 43)

---

## Did the prior action items hold?

3번째 사이클 데이터:

| Prior retro action | Status | Observation |
|---|---|---|
| `loop.md` 명시 invoke 지속성 | ✅ 유지 | Step 3 task-context-flow Skill 명시 호출, Step 7 tdd-endpoint Skill 명시 호출. **이번엔 loop.md 검증 자체가 task 목적이라 invocation 압력이 외부에서 옴** — 자율적 지속성 증거로는 약함. 다음 사이클이 진짜 테스트. |
| `harness.md` 명시 walk | ⚠️ **부분** | post-search 구현 단계에서 `./gradlew test` 자동 체크만 돌리고 7-check manual walk-through 안 함. **Week 2-2 커리큘럼 적용 단계에서 별도로 walk** — 이게 첫 진짜 walk였음. 구현→harness 사이의 시간 차로 self-bypass 했다는 신호. |
| `batches/*.json` 작성 | ❌ skip | post-search 가 5줄 prod 변경이라 의도적 skip. **단일 파일 작은 변경에는 batch 가 over-engineering** 임을 확인. discipline value 보다 ceremony cost 가 커지는 임계점 발견 — 변경 폭이 2개 파일 이하면 skip 정당화 가능. |
| Anchoring bias controlled test | ❌ 미실행 | 이번 사이클에서도 잠정안 6개 중 4개 채택 + 2개 사용자 변경. 4/6=67% — 3 사이클 데이터로 처음 비교 가능: GET 17/17, PUT+DELETE 17/17+U10, post-search 4/6. **분산이 처음으로 나타남** — 사용자가 의도적으로 더 narrow 한 결정(title only, escape drop)을 한 첫 사이클. anchoring 인지 능동 선택인지 가르는 controlled test 는 여전히 미실행. |

---

## What worked

- **Loop.md 검증을 task 목적으로 명시한 것**이 모든 단계가 명시 호출되도록 강제했다. 평소 self-bypass 되는 step 3·7 Skill 들이 외부 압력으로 invoke 됨. 그 결과 review.md 의 `## Loop Validation Record` 가 부산물로 남음 — 다음 retro 의 데이터.
- **Test failure → assumption correction loop**: R-2 ("`?q=%` 가 SQL 와일드카드처럼 동작") 가정 → 테스트 실패 → Spring Data 자동 escape 발견 → Task Context 의 Q-B "escape drop" 결정 자체가 moot 였음을 노출. **테스트 작성이 "잘못된 task context 가정"을 발견한 첫 사례**.
- **Harness self-review 가 모두 lock-in 테스트로 귀결됨** (prod 코드 변경 0). harness 의 본질("새 기능을 만드는 도구가 아니라 통과한 평가 vs 진짜 목적을 가르는 도구")이 실제로 작동.
- **Core/project 분리가 작은 변경으로 끝남**: 신규 파일 3개 (core/loop.md, core/harness.md, README.md), 루트 파일 2곳에 1줄씩만 추가. 큰 구조 변경이지만 churn 없음 — root files 가 이미 project-customized 였기 때문에 core/ 를 *추출* 하면 됐고, root 본문은 안 건드림.

---

## What didn't work / surfaced gaps

- **Agent-os 가 Spring/Java 에 종속되어 있다는 사실을 7주 동안 알아채지 못함.** context-template.md 는 처음부터 generic 이었지만, loop.md 와 harness.md 가 점진적으로 Spring 특화 됨 — Skill 이름 추가, gradlew 명령 박힘, Hibernate / Flyway 언급. **사용자 지적이 없었으면 영구히 그 상태로 갔을 가능성**. 자체 audit 트리거가 없었다.
- **Harness 명시 walk 가 self-bypass 됨** — post-search 구현 단계에서 자동 체크만 돌리고 manual 7-check 는 Week 2-2 커리큘럼 적용으로 강제될 때까지 안 함. 이전 retro 의 "harness 명시 walk" action item 이 외부 압력 없이는 무너졌음.
- **Tdd-endpoint Skill 이 Spring 특화임을 명시 안 함**. README 작성하기 전까지 `tdd-endpoint` 가 "범용 4-round 패턴" 인지 "Spring 4-round 패턴"인지 모호. README 의 Skills 표가 이 모호함을 처음 명시화.

---

## New observations specific to this cycle

1. **Reward hacking 의 가장 흔한 형태: framework-level safety guarantee 를 모르고 가정한 결정.** Q-B "사용자가 입력한 %, _ 는 그대로 SQL 와일드카드로 동작" 결정이 사실은 Spring Data Containing 의 자동 escape 로 보호되고 있었음. **테스트 실패가 가정 오류를 알려준 첫 사례**. 이 패턴은 `spring-boot-4-verify` Skill 의 카탈로그 항목으로 가치 있음: "Spring Data derived query 키워드 동작(`Containing`, `StartingWith`, `IgnoreCase`) — 자동 escape / 자동 lower 등 framework 가 무엇을 무료 보장하는지 미리 확인."

2. **변경 폭이 임계점 아래면 batch json 은 over-engineering.** post-search 는 prod 5줄. batch json 작성 cost > discipline value. 임계점 추정: **3개 이상 파일 동시 변경 OR 마이그레이션 포함 시** batch json 작성. 단일 controller·service 변경은 skip. 다음 batch json 작성 task 가 나오면 임계점 데이터 보강.

3. **Anchoring bias 4/6 데이터 포인트.** 잠정안 채택률이 처음으로 100% 미만. 사용자 변경 항목 (title only / escape drop / N+1 명시) 의 공통점: **모두 "줄이는" 방향**. 잠정안이 항상 "안전한 over-spec" 쪽으로 치우치는 경향? — 다음 사이클부터 잠정안 작성 시 의식적으로 narrow option 도 잠정안으로 제시할 가치. 단순 anchoring 측정 외에 **잠정안의 conservative bias** 측정도 필요.

4. **AGENTS.md / root files 가 "지도가 아니라 매뉴얼" 화 우려.** harness.md 가 95줄로 늘었음 (Osmani 권장 60줄 초과). 7-check 라벨링 비용. **Quality 와 brevity 의 trade-off** 가 처음 보임. 다음 cycle 에서 harness.md 압축 시도 가치 — 라벨은 유지하되 본문 항목 압축.

5. **Skill 이 stack-specific 한 것이 design choice 이지 결함이 아니라는 명시화 가치 발견.** README 작성하며 "Skills should be extracted from real failures, not invented up-front" 원칙 명문화. 이 원칙 자체는 prior retro 들에서 implicit 했지만, **휴대성 관점에서 명시 필요**. 그렇지 않으면 새 프로젝트로 포팅할 때 "Skill 도 generic 해야 한다"는 잘못된 일반화로 갈 수 있음.

---

## Tracker snapshot (after this cycle)

- 🟢 extracted: 3 (tdd-endpoint, spring-boot-4-verify, task-context-flow) — **변경 없음**
- 🟡 candidate: 1 (Testcontainers seed-then-GET, absorbed) — **변경 없음**
- ⚪ monitoring: **7** (이전과 동일)
- 🆕 **신규 후보**: "Framework auto-safety guarantee — verify before assuming custom code needed" (Reward hacking 회피 패턴). 추출 임계는 count=2 — 다음 framework 신뢰 결정 시점에 bump 결정.

이번 사이클은 도메인 entity 가 추가되지 않아 (검색 필터만 추가) monitoring 엔트리들의 count bump 가 발생 안 함. **Comment / User entity 가 추가되는 다음 사이클에 multiple bump 예상.**

---

## Action items

- [ ] **Next task** — 자동 invocation 지속성 3번째 데이터 포인트. 외부 압력(loop.md 검증, 커리큘럼 적용) 없이 step 3·7 Skill 이 자율 호출되는지 관찰. **이번에 진짜 테스트**.
- [ ] **Harness manual walk 의 self-bypass 패턴** 처방: 단순히 "next time walk it" 이 아니라 **언제 walk 가 trigger 되어야 하는지** 명시화. 후보: "prod 코드 변경이 있으면 commit 직전 walk, 변경이 README/docs only 면 skip 정당화 가능."
- [ ] **harness.md 압축 시도** — 95줄 → 70줄 목표. 7-check 라벨 유지, 본문 bullet 압축. Osmani 60줄 권장에 가까워지기.
- [ ] **`spring-boot-4-verify` Skill 카탈로그에 "Spring Data derived query 키워드 동작" 항목 추가** — Q-B 발견 사례 명문화. 1줄 entry.
- [ ] **Batch json 임계점 명문화** — `loop.md` 또는 별도 docs 에 "batch json 작성 트리거: ≥3 파일 동시 변경 OR 마이그레이션 포함" 명시. 임계점 데이터 1회 더 모인 뒤 결정.
- [ ] **잠정안 conservative bias 측정** — 다음 task context 작성 시 의식적으로 narrow option 도 잠정안에 포함. 사용자가 "더 narrow" 와 "더 broad" 중 어느 쪽으로 줄이는지 패턴 관찰.
- [ ] **Core/root 동기화 discipline** — 향후 root loop.md / harness.md 의 구조 변경 시 core/ 도 같이 업데이트하는 규칙 필요. 별도 docs 또는 commit hook 가능성. 일단 1 cycle 동안 수동 관찰.

---

## One-line takeaway

> 3개 사이클 중 첫 "loop 검증을 task 목적으로 한" 사이클. 평소 self-bypass 되는 step 들이 외부 압력으로 invoke 되며 부산물로 `## Loop Validation Record` 와 `## Harness Review Record` 가 review.md 에 쌓였다. **진짜 통제 데이터는 다음 사이클** — 외부 압력이 없을 때 invocation 이 지속되는가, 아니면 self-bypass 가 재발하는가.

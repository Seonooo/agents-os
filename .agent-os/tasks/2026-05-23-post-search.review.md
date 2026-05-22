# Task Context Review — `GET /posts?q=` 검색 필터

- **Target Task Context**: `.agent-os/tasks/2026-05-23-post-search.md`
- **Reviewed**: 2026-05-23
- **Procedure**: `task-context-flow` Skill 1-6 + `loop.md` 단계 3 (task-context-flow Skill 인라인 호출 검증 사례)
- **부가 목적**: `loop.md`의 새 인라인 Skill 링크가 실제 작동하는지 검증

---

## 1. AI Summary (사전 이해)

### Task goal
`GET /posts`에 `?q=` 검색 필터를 한 개 추가. 페이지네이션과 조합 가능, 서버 고정 정렬 유지. 동시에 `.agent-os/loop.md`의 step-3 Skill 인라인 링크 검증.

### Change scope (초안)
- `PostController.list(...)` — 파라미터 1개 추가
- `PostService.list(...)` — 시그니처 확장
- `PostRepository` — 검색 쿼리 메서드 1개 추가
- 단위/E2E 테스트 추가
- 신규 파일 0개, 스키마 변경 없음

### Acceptance criteria 핵심
`?q=` 미지정 → 회귀. `?q=foo` → 잠정안 title+content LIKE 매칭. 길이 초과 → 400.

### Interpretive points
I1 (파라미터명 `q`) / I2 (미지정·빈문자열 동등) / I3 (시그니처 확장 vs 오버로드) / I4 (`@Query` HQL vs derived) / I5 (escape Service 레이어) / I6 (회귀 안전망 `q.isBlank()` 분기) / I7 (E2E 5개)

### Open questions
Q-A (빈 q 처리) / Q-B (와일드카드 escape) / Q-C (검색 범위) / Q-D (매칭 방식) / Q-E (q 길이 상한) / Q-F (유니코드 케이스 폴딩)

---

## 2. Human Review (2026-05-23)

### 🔴 Major correction × 0 (도메인적 뒤집기 없음)

이번 task는 loop.md 검증이 목적이라 의도적으로 작은 범위. Major correction은 없었지만 **변경 폭 축소**가 세 곳에서 일어남.

### Plan 축소 (3건)

> "우리 규모에서는 title only로 줄이고, escape는 drop, N+1에 대한 방지 필요, 나머지는 그대로 진행"

1. **Q-C 축소**: 검색 범위 `title+content` 잠정안 → **`title` only**. 근거: 현재 게시판 규모에서 content full scan은 과한 비용/복잡도. content 검색은 후속 task로 분리 가능.
2. **Q-B/I5 드롭**: 와일드카드 escape 처리 **삭제**. 근거: 사용자 입력에 `%`/`_`가 들어와도 그냥 SQL 와일드카드로 동작하는 게 MVP 수준에서 수용 가능. escape는 보안 이슈가 아니고 (LIKE 와일드카드는 SQL injection 아님) UX 미세조정에 불과 → 코드 단순화를 우선.
3. **N+1 방지 명시**: 검색은 **단일 `Page<Post>` 반환**으로 처리. `findAllById` 같은 2-step 패턴 금지.

### Resolved decisions table

| # | 최종 결정 |
|---|---|
| Q-A | `?q=` 빈 문자열 = 미지정과 동일 (잠정안 채택) |
| Q-B | 와일드카드 escape **drop**. `%`/`_` 입력은 SQL 와일드카드로 그대로 동작 (known MVP behavior) |
| Q-C | 검색 범위 **`title` only** (content/author 제외) |
| Q-D | case-insensitive partial LIKE (잠정안 채택) |
| Q-E | `@Size(max=100)` (잠정안 채택) |
| Q-F | 영문만 case-insensitive 가정 (잠정안 채택) |
| **N+1** | **단일 `Page<Post>` 쿼리** 강제. derived `findByTitleContainingIgnoreCase`가 정확히 이 조건 만족 + soft-delete `@SQLRestriction` 자동 적용. |
| I1~I7 | I1·I2·I3·I6·I7 확정. **I4 변경**: `@Query` HQL → **derived query** (title only · `@SQLRestriction` 자동 · 한 줄). **I5 삭제**: escape drop. |

### Confirmed Interpretive points
- I1: 파라미터명 `q` — OK
- I2: 미지정·빈문자열 동등 — OK
- I3: 단일 시그니처 확장 — OK
- I4: **변경됨** → derived query
- I5: **삭제됨** → escape drop
- I6: `q.isBlank()` 분기로 회귀 안전망 — OK
- I7: E2E ~5개 — OK

---

## 3. Trace of changes applied to Task Context

| 시점 | 위치 | 변경 |
|---|---|---|
| 2026-05-23 (초안) | 전 섹션 | I4=@Query, Q-A~Q-F 잠정안 + `[검토 필요]` 마커 6개 |
| 2026-05-23 (이 review 후) | Acceptance Criteria → 검색 성공 | "title+content" → **"title only"**. Q-C 마커 제거. |
| 2026-05-23 (이 review 후) | Acceptance Criteria → 검증 실패 | "와일드카드 escape — Q-B 결과에 따라" 줄 삭제. Q-B 마커 제거. |
| 2026-05-23 (이 review 후) | Constraints → Security | "escape 필요 여부 `[검토 필요]`" → "와일드카드는 literal 동작 (MVP 수용)" |
| 2026-05-23 (이 review 후) | Verification → Logs | 검색 SQL 예상 형태를 `LOWER(title) LIKE ?` (content/OR 제거)로 정정 |
| 2026-05-23 (이 review 후) | Forbidden | N+1 방지 — `findAllById` 등 2-step 패턴 금지 항목 추가 |
| 2026-05-23 (이 review 후) | Open Questions | Q-A·Q-D·Q-E·Q-F는 잠정안 확정 표기. Q-B·Q-C 항목 제거(해결됨). |

---

## 4. Learning notes

- **loop.md 검증을 위해 의도적으로 작은 task를 선택했다.** 그 결과 Major correction이 없는 첫 사례. anchoring bias 우려가 있을 수 있지만, 사용자가 **세 곳에서 능동적으로 축소**(title only / escape drop / N+1 명시)했으므로 단순 승인은 아님.
- **N+1 방지를 사람이 명시했다.** 잠정안에는 없던 항목. AI가 "단일 Page<Post>" 자체는 가정했지만 explicit하게 forbidden에 넣지는 않았었음 — 사람이 명시함으로써 후속 회귀 시 가드 역할.
- **derived query의 재발견.** `findByTitleContainingIgnoreCase` 한 줄이 (i) title only matching, (ii) case-insensitive, (iii) `@SQLRestriction` 자동 적용, (iv) 단일 `Page<Post>` 반환 — 네 요구를 동시에 만족. AI 첫 plan(`@Query` HQL)은 over-engineering이었음.

## 5. Action items for Phase 2

- [x] task context의 `[검토 필요]` 마커 제거 (Q-B, Q-C)
- [ ] `tdd-endpoint` Skill로 TDD 진행 (Round 1~4)
- [ ] Reviewer/Tester 모자 적용 후 결과를 ## Loop Validation Record에 기록
- [ ] 끝나면 `docs/html/task-check.html`의 TASKS에 슬러그 등록 (사람 요청 시)

---

## Loop Validation Record

> Loop.md 검증을 위한 4종 기록. Reviewer/Tester 모자 단계 후 채워짐.

### 1. AI 첫 plan

- Service unit test (Round 1)
- Repository `@Query` HQL 메서드 추가 (`AND p.deletedAt IS NULL` 명시) — I4 잠정안
- `PostService.list(int, int, String q)` — q blank 분기 + escape 처리 — I5
- `PostController` `@RequestParam(required=false) @Size(max=100) String q`
- 검색 범위: **title+content** OR LIKE (Q-C 잠정안)
- E2E 5개 (미지정 회귀 / 빈 q / 매칭 1건 / 0건 / 길이 초과 / 와일드카드 literal)

### 2. 사람이 줄인 범위

- **검색 범위**: `title only` (content/author 제외)
- **escape 처리**: drop (사용자 입력의 `%`/`_`는 그대로 와일드카드로 동작 — known MVP behavior)
- **N+1 명시 추가**: 단일 `Page<Post>` 강제 (`findAllById` 같은 2-step 금지)
- **I4 변경**: `@Query` HQL → derived query (`findByTitleContainingIgnoreCase`) — 한 줄로 4개 요구 동시 만족

### 3. Failure modes (Reviewer + Tester 모자에서 잡힌 것)

**Reviewer 모자 (4건)**
- **R-1**: 주변 공백 trim 미적용 — `?q=  spring  `이 literal로 검색됨. UX 한계. **결정: 문서화로만 처리, 코드 변경 X** (Q-B drop과 같은 정신 — input normalization 안 함)
- **R-2**: `?q=%` SQL 와일드카드 동작 미테스트 (Q-B drop 결정의 sleeping behavior 락-인 필요)
- **R-3**: `?q=  ` 공백-only E2E 미테스트 (Service 단위는 있음)
- **R-4**: 기존 list 스니펫에 `q` 미문서화 — **결정: 별도 스니펫 정책 유지**

**Tester 모자 (4건, 2건은 직접 AC 갭)**
- **T-1**: `?q=` × `?page=` × `?size=` 조합 — **AC에 직접 명시되어 있는데 테스트 없음** 🔴
- **T-2**: 매칭 2건 이상 정렬 유지 — Service 단위만 검증, E2E 없음
- **T-3**: `?q=` × `?size=200` validation precedence — **결정: 독립 검증으로 충분, 추가 X**
- **T-4**: soft-delete 제외 (이미 추가됨)

### 4. 반영한 개선 (Reviewer/Tester 모자 → 코드 반영)

채택 4건 / 거부 3건. **모두 테스트 추가만 (prod 코드 변경 X)** — Builder 모자 단계에서 prod 로직은 검증을 통과했고, 추가 검증은 contract lock-in 목적.

1. **R-2 → `get_with_sql_wildcard_q_is_escaped_by_spring_data_and_matches_literal`**
   - 의도: `?q=%`가 SQL 와일드카드처럼 모든 글 매칭한다고 가정하고 테스트 작성 → **실패**.
   - 실제: Spring Data JPA의 `Containing` 키워드가 `%`, `_`, `\`을 자동 escape (`LIKE ... ESCAPE`). literal 매칭만 됨.
   - **무료로 얻은 안전성** — Q-B의 "escape drop" 결정이 사실은 moot였음을 발견. 테스트가 이 framework 동작을 lock-in.
2. **R-3 → `get_with_whitespace_only_query_returns_all_posts_like_unfiltered`** — HTTP 경로 커버.
3. **T-1 → `get_with_query_paginates_filtered_results`** — `?q=spring&page=1&size=2`로 4건 중 매칭 3건, 두 번째 페이지 1건. AC 직접 커버. (3 spring posts → totalElements=3, totalPages=2)
4. **T-2 → `get_with_query_orders_matches_by_created_at_desc_id_desc`** — 매칭 3건의 정렬 순서 검증.

### 5. 추가 발견 (불의의 학습)

Implementation 단계의 **빨간불 → 학습**: R-2 테스트 작성 시 `?q=%`가 모든 글 매칭한다고 가정 → 실제 0건 반환 → 검색해보니 Spring Data가 자동 escape. Task Context의 Q-B 결정 ("escape drop, 사용자 입력 `%` 그대로 와일드카드 동작")이 **실제 동작과 불일치**였다는 것을 **테스트 실패가 알려줌**.

→ Lock-in 테스트의 가치: 가정과 실제 사이의 gap을 강제로 노출.
→ task context Q-B 항목은 이제 "literal escape via Spring Data Containing — 무료" 의미로 재해석 가능.

### Loop.md 검증 결과 (메타)

- ✅ **step 3 task-context-flow Skill 인라인 호출** — 작동. 명시 호출 시 review.md 의 1-5 섹션 자동 구조화.
- ✅ **step 7 tdd-endpoint Skill 인라인 호출** — 작동. 4 round 패턴 그대로 따름. precondition 검사가 review.md 존재 강제.
- ✅ **step 9 Reviewer/Tester 분리** — 다른 관점 추출됨. Reviewer는 design/UX, Tester는 AC 갭 발견.
- ⚠️ **loop breaker (Spring Boot 4 verify)** — 호출 안 함. 이번엔 SB4 surprise 없었음.
- 📋 **다음 retro action item 후보**: "테스트 실패가 잘못된 task context 가정을 발견했다 (R-2)" — Q-B 처럼 명시 결정한 항목도 framework 동작 확인 안 하면 잘못 정의될 수 있음. `spring-boot-4-verify` Skill에 "Spring Data derived query 키워드 동작 확인"을 카탈로그로 추가할 가치.

---

## Harness Review Record

> Week 2-2 커리큘럼 실습 — `.agent-os/harness.md` (7-check 템플릿)으로 post-search 구현을 검토. Review Prompt 그대로 적용:
> *"Read AGENTS.md and .agent-os/harness.md. Review this implementation using the harness. Focus on intent mismatch, reward hacking, missed requirements, operational failure, and security risk."*

### 7-Check 통과 현황

| 체크 | 결과 | 메모 |
|---|---|---|
| Intent Check | ✅ | Goal·AC·resolved questions 일관. Q-A~Q-F 6개 중 4개 잠정안, 2개 사용자 결정 (Q-B·Q-C). |
| Requirements Check | ✅ | 25개 AC 1:1 매핑 (task-check.html). Forbidden 미위반. `[검토 필요]` 마커 0. |
| Code Check | ✅ | prod 5줄 (Controller·Service·Repository 각 한두 줄). 추상화 0. conventions 부합. |
| Test Check | ⚠️→✅ | 3개 갭 발견 → 모두 보강 (#1·#2·#3 아래). |
| Operations Check | ⚠️ | 2개 갭 발견. 1개 보강 (#4), 1개 거부 (rate limit). |
| Security Check | ✅ | JPA 파라미터 바인딩, `@Size(max=100)`. 인증/인가는 task scope 외. |
| Reward Hacking Check | ⚠️→📋 | 한글 case-folding trade-off 명시 부족 — lock-in test 추가 (#1). |

### 1. 하네스로 잡은 문제 (5건)

`.agent-os/harness.md` 의 7-check 를 순차 통과시키며 발견:

1. **(Test Check) 한글/유니코드 `q` 테스트 부재** — Task Context Q-F 의 "영문만 case-insensitive 가정" 결정이 의도된 trade-off지만 lock-in test가 없어 미래 contributor가 모름.
2. **(Test Check) SQL injection 시도 lock-in 없음** — JPA 파라미터 바인딩이 차단하지만 명시 회귀 테스트 부재. 미래 누군가 raw `@Query` 또는 native SQL로 바꾸면 silent regress.
3. **(Test Check) `q` 정확히 100자 (boundary) 미테스트** — `@Size(max=100)` 경계값. 101자만 테스트되어 있어 100자가 통과하는지 확인 부재.
4. **(Operations Check) 풀 테이블 스캔 + 별도 COUNT(*) 위험** — 데이터 누적 시 latency 위험. 단, `?size=` 기본값 20 (max 100) 이 per-request 페이로드 상한으로 작용 — DB 작업량은 아니지만 client-bound 데이터량은 캡됨.
5. **(Operations Check) rate limit 부재** — 검색 abuse 벡터. MVP scope 외.

### 2. AI가 추가로 지적한 문제 (체크리스트 외)

7-check 통과 중 발견한 부수 관찰들 (모두 minor):

- **Loop.md step 8 (harness 실행) walk-through 부재** — post-search 구현 단계에서 `./gradlew test` (자동 체크)만 실행하고 7-check manual walk-through 안 함. 이번 harness review가 그 보강.
- **task-check.html의 `reward_hacking` 섹션이 사실상 harness.md 의 Reward Hacking Check 역할 중복 수행** — 이번 harness.md 재작성으로 두 곳 중 한 곳에서만 권위 있는 정의. task-check.html 은 task별 인스턴스, harness.md 는 일반 체크리스트.
- **R-2의 framework auto-escape 발견이 본 검토에서 재확인** — Reward Hacking Check 의 "framework가 우연히 보장한 안전성" 항목으로 명문화되어 미래 사례 가이드.

### 3. 내가 최종적으로 반영한 개선 (4건, 모두 test-only — prod 코드 변경 0)

E2E 4개 추가 (PostControllerE2ETest, 총 39 → 43):

1. **`get_with_korean_query_matches_korean_title_locks_in_current_behavior`** — 한글 title × 한글 q 동작 명문화. 현재 substring 매칭 동작 lock-in.
2. **`get_with_sql_injection_attempt_q_is_treated_as_literal_string`** — `' OR 1=1 --` 입력 시 0건 반환 (tautology truthy로 모든 글 누출 안 됨) 검증. Security regression guard.
3. **`get_with_q_at_exact_max_length_succeeds`** — 정확히 100자 통과 (boundary off-by-one guard). 기존 101자 → 400 테스트와 짝.
4. **`get_with_q_and_no_size_uses_default_size_20_capping_per_request_payload`** — 25건 매칭 시 첫 페이지 20건만 반환, totalPages=2 확인. 검색 경로에도 size guardrail 적용됨을 lock-in.

### 4. 반영하지 않은 제안과 이유 (2건)

1. **검색용 인덱스 추가 (V3 Flyway)** — **거부**.
   - 이유: `.claude/rules/hard-rules.md` 의 "V2 외 추가 Flyway 마이그레이션 금지" 및 task context Forbidden "인덱스 추가 금지" 위반. 인덱스 필요는 데이터 누적 시점에 별도 task로 분리. MVP 데이터량(수십~수백 row)에선 측정 가능한 latency 영향 없음.
2. **검색 rate limit** — **거부**.
   - 이유: MVP scope 외. 인증/인가도 없는 단계라 사용자 식별이 안 되어 rate limit 적용 단위가 모호 (IP-based 도 reverse proxy 환경에서 별도 인프라 필요). 운영 단계 진입 시 별도 task.

### 5. Week 2 완료 기준 충족 표

| 기준 | 상태 | 증거 |
|---|---|---|
| `project/.agent-os/loop.md` 작성 | ✅ | `commit f26b7bf` 이후 인라인 Skill 링크 보강 완료 |
| `project/.agent-os/harness.md` 작성 | ✅ | 이번 작업에서 커리큘럼 템플릿(7섹션 + Review Prompt)으로 재작성 — `.agent-os/harness.md` |
| 실제 작업 1개를 loop로 처리 | ✅ | post-search task (이 파일 §1~4: Loop Validation Record) |
| AI 산출물 1개를 harness로 검토 | ✅ | 이 섹션 (Harness Review Record) |
| failure mode / reward hacking / 요구사항 누락 중 1개 이상 발견 | ✅ | 5건 발견 — Test(누락) 3건 + Operations(failure mode) 2건. Reward Hacking 관련해서는 framework auto-escape 발견(§5 추가 발견)이 이미 task 진행 중 발생함. |

### 6. Learning notes (Week 2-2)

- **Harness는 "더 센 프롬프트"가 아니라 "검증 항목"이다** — 커리큘럼 본문 인용. 이번 적용에서 입증됨: 모든 발견은 "안 본 곳을 봐라"가 아니라 "이 체크 항목을 통과했나?"로 잡혔다. 7-check 가 새 결함을 발견했다기보다, 결함이 될 만한 곳을 명시적으로 점검하게 강제함.
- **Reward Hacking 의 가장 흔한 형태**: framework가 무엇을 무료로 보장하는지 모르고 가정한 결정(Q-B "escape drop") — 테스트 실패가 알려줌. harness.md 의 "framework가 우연히 보장한 안전성" 항목이 이 경우를 명문화.
- **harness review 자체가 새 prod 코드를 만들지 않았다** — 모든 개선이 lock-in 테스트. 이것이 harness 의 본질: "통과한 평가 vs 진짜 목적"을 가르는 도구이지, 새 기능을 만드는 도구가 아님.

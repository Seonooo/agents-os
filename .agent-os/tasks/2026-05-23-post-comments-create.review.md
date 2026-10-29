# Task Context Review — `POST /posts/{postId}/comments`

- **Target Task Context**: `.agent-os/tasks/2026-05-23-post-comments-create.md`
- **Reviewed**: 2026-05-23
- **Procedure**: `task-context-flow` Skill 1-6 + 본 task에서 **새 Skill `trade-off-table` 추출** (Q-row 결정 흐름 중 surfaced)
- **부가 목적**: core/project agent-os split 이후 첫 nested resource task. loop.md step 5 ("small implementation plan") 정직 준수 검증.

---

## 1. AI Summary (사전 이해)

### Task goal
`POST /posts/{postId}/comments` 한 endpoint 추가. 신규 Comment entity + V3 Flyway 마이그레이션 + 생성 경로(Controller/Service/Repository/DTO)까지. GET 목록은 후속 task로 분리.

### Change scope
- 신규 패키지 `com.example.board.comment` 8개 파일
- 신규 V3 마이그레이션
- Post 도메인 변경 0, 공용 인프라(`PageResponse`/`ErrorResponse`/`PostNotFoundException`) 재사용
- 후속: list task (GET), delete task (V4 + endpoint)

### Acceptance criteria 핵심
- POST 성공 → 201 + Location + `CommentResponse`
- 검증 실패 → 400 + `errors[].field`
- Post 미존재 / soft-deleted → 404 (`PostNotFoundException` 재사용, `existsById`)
- 회귀: 기존 43개 E2E + 단위 모두 통과

### Risks watched
- nested resource URL 첫 도입 (Location header convention)
- `existsById`의 `@SQLRestriction` 적용 여부 (lock-in 테스트로 검증 필요)
- 새 엔티티의 soft-delete 컬럼/패턴 옵션 (Q-F)
- N+1: 본 task는 INSERT 한 곳뿐이라 위험 0, 그러나 후속 list task에서 살아남.

### Interpretive points (I1~I7)
I1 (nested URL) / I2 (postId echo in body) / I3 (`updatedAt` 포함) / I4 (Comment 전용 NotFoundException 미추가) / I5 (단위·E2E만, repo 단독 X) / I6 (동시 INSERT IDENTITY 안전) / I7 (`existsById` `@SQLRestriction` 자동 적용 가정 — lock-in 테스트로 확인)

### Open questions (초안 8건 → list task로 3건 deferred 후 5건)
Q-A (패키지 sibling/nested) / Q-B *deferred* / Q-C *deferred* / Q-D (content max) / Q-E (V3 인덱스) / Q-F (deletedAt 미리 vs V4) / Q-G (FK 매핑) / Q-H *deferred*

---

## 2. Human Review (2026-05-23)

### 🔴 Major correction × 1 (scope 축소)

> "여기서 week2 목적을 생각하면 더 잘게 작업 task를 쪼개는 것이 목표 아닌가?"

**초안**: POST + GET 두 endpoint + entity + V3 + deletedAt 컬럼 (옵션) 까지 한 task에 묶음.
**시정**: GET을 후속 list task로 분리, 본 task는 POST만. Q-B / Q-C / Q-H deferred. 파일 이름도 `post-comments` → `post-comments-create`로 변경.

근거: loop.md step 5 "*small* implementation plan (per OpenAI Codex, prompt cost is quadratic in turns)" 직접 위반. 직전 post-search task가 의도적으로 작은 사이즈로 loop 검증한 것과 대칭이 깨짐. Reward Hacking Check ("scope minimal?")이 task 시작 시점에서도 적용돼야 함을 사람이 명시.

### 🔴 Major correction × 0 (도메인적 뒤집기 없음)

다른 차원에서는 잠정안과 큰 충돌 없음. Q-F는 잠정안(미리 추가) → 사람 선호(V4 분리)로 뒤집혔지만 도메인이 아닌 schema-시점 선택이라 Major로 분류 안 함.

### Plan 축소 / 결정 (5건)

| # | 최종 결정 | 근거 |
|---|---|---|
| Q-A | **sibling** `com.example.board.comment` | 향후 Comment를 다른 리소스로 확장 가능성 우선. trade-off-table form으로 비교 후 확정. |
| Q-D | `@Size(max=1000)` | 잠정안 채택. Post content 5000보다 짧은 댓글이 자연스러움. |
| Q-E | V3에 `post_id` 인덱스 **포함** | 신규 테이블 초기 인덱스는 hard-rules "V1 PK only" 제약 대상 아님 (V1 = posts 전용 규약). 후속 GET 조회 비용 미리 차단. |
| Q-F | **V4로 분리** (deletedAt 미리 추가 안 함) | Post가 V1→V2로 deleted_at을 분리 추가한 패턴과 대칭. 각 task가 정확히 자기 endpoint만큼만 schema 건드림. "endpoint 없는 컬럼" over-engineering 회피. |
| Q-G | `private Long postId` (plain FK 컬럼) | 본 task 사용 빈도 INSERT 한 곳뿐. ManyToOne의 navigability 이득 < 단순성 이득. N+1 위험 0. |

### Confirmed Interpretive points
- I1~I7 모두 OK. 별도 정정 없음.

---

## 3. Meta-correction: 새 Skill 추출 (`trade-off-table`)

본 task 진행 중 Q-A에 대한 follow-up 질문 ("sibling이 뭐지?") 발생 → 원인: 잠정안만 명시하고 대안의 의미·trade-off를 prose로 묻어둠. 해결: `trade-off-table` Skill을 신규 추출하고 `task-context-flow` step 3에 포인터 추가.

| 항목 | 변경 |
|---|---|
| `.claude/skills/trade-off-table/SKILL.md` | 신규 작성 (frontmatter + When/Not/Form/Anti-patterns/Worked example) |
| `.claude/skills/task-context-flow/SKILL.md` | step 3 #6 (Open questions) 항목에 "Q-row가 2+ non-trivial 대안 가질 시 `trade-off-table` 호출" 한 줄 추가 |
| `AGENTS.md` | Skills 섹션에 `trade-off-table` 한 줄 추가 |

근거: `docs/agents/skill-candidates.md`의 "재사용 2+회 시 Skill 추출" 정책. 본 case는 1회지만 incident가 명확하고 form이 단순하며 mandate가 한 줄에 압축 가능하므로 즉시 추출 (post-search Q-B "framework auto-escape 발견" retro action item과 같은 정신).

---

## 4. Trace of changes applied to Task Context

| 시점 | 위치 | 변경 |
|---|---|---|
| 2026-05-23 (초안) | 전 섹션 | I1~I7, Q-A~Q-H 8건, [검토 필요] 마커 5개 (Q-A·Q-D·Q-E·Q-F·Q-G) |
| 2026-05-23 (scope 축소) | 파일 rename | `2026-05-23-post-comments.md` → `2026-05-23-post-comments-create.md` |
| 2026-05-23 (scope 축소) | Goal·Change Scope·Forbidden | GET 관련 서술 제거. `PageResponse` 재사용 노트는 "후속 task" 표기. GET endpoint 추가 금지 항목 신규. |
| 2026-05-23 (scope 축소) | Open Questions | Q-B·Q-C·Q-H deferred 섹션으로 이동 |
| 2026-05-23 (이 review 후) | Open Questions → 본 섹션 → Resolved Decisions로 재배치 | Q-A·Q-D·Q-E·Q-F·Q-G 결정 반영. [검토 필요] 마커 5건 모두 제거. |
| 2026-05-23 (이 review 후) | Soft-delete (Q-F branch) | "Q-F = 미룸 채택" 가지만 남기고 다른 분기 제거. |
| 2026-05-23 (이 review 후) | Change Scope / Constraints | Q-G 결정 반영 → "엔티티는 `private Long postId` 보유" 명시. |

---

## 5. Learning notes

- **loop.md step 5 위반을 사람이 잡았다.** AI는 신규 entity + 2 endpoint + 옵션 컬럼까지 한 task에 묶는 첫 plan을 제시. post-search task가 의도적으로 작은 사이즈로 loop 검증을 했음에도, **그 다음 task에서 즉시 over-scoping으로 회귀**. → 잠재 Skill 후보: "task 사이즈 self-check" — Reward Hacking Check를 task 시작 시점에도 자동 적용하는 mini-Skill. 재발 시 추출 검토.
- **Q-row의 prose 형식이 follow-up round를 만들었다.** Q-A에서 "sibling이 뭐지?" → trade-off가 prose에 묻혀 보이지 않음. `trade-off-table` Skill 추출 (§3) 로 해결. 다음 task부터 Q-row 작성 시 본 Skill 강제 적용.
- **Q-F의 "V4로 분리" 결정은 Post의 V1→V2 패턴과 대칭이라는 미적 일관성**으로 사람이 결정. AI 잠정안 ("미리 추가")는 schema-효율성을 우선했지만 "각 task가 정확히 자기 것만 건드림"이라는 워크플로우 일관성을 못 봤음. → 향후 schema 결정 시 *마이그레이션 한 번에 vs task 단위 응집* 축을 명시 검토.
- **Skill을 본 task 진행 도중에 추출한 것이 처음.** 기존 3개 Skill (`tdd-endpoint`, `spring-boot-4-verify`, `task-context-flow`)는 모두 재사용 2+회 후 사후 추출. `trade-off-table`은 incident 발생 즉시 추출 — incident가 form까지 명확했기 때문에 가능했음. 재사용 횟수가 아니라 *form의 명료성*이 추출 결정의 다른 축임을 학습.

---

## 6. Action items for Phase 2

- [x] task context의 `[검토 필요]` 마커 5건 제거
- [x] 파일 rename `post-comments` → `post-comments-create`
- [x] `trade-off-table` Skill 추출 + `task-context-flow` 포인터 + AGENTS.md 등록
- [ ] `tdd-endpoint` Skill로 TDD 진행 (Round 1~4: Service unit → POST E2E happy → 검증 실패 케이스 → 회귀)
- [ ] Reviewer/Tester 모자 적용 후 결과를 `## Loop Validation Record` 섹션에 기록
- [ ] Harness Review Record 작성 (`./gradlew test` + 7-check)
- [ ] 끝나면 후속 task `post-comments-list` (Q-B·Q-C·Q-H 결정) context 작성

---

## Loop Validation Record

> Reviewer/Tester 모자 단계 후 채워짐. 본 task의 특이점: **task-context-flow Skill 도중 새 Skill을 추출한 첫 사례** — 그 자체가 loop 의 "11. Record what should be reused" 단계의 즉시 실행 사례.

(implementation 후 작성)

---

## Harness Review Record

> 7-check (`./gradlew test` + 7개 manual section)는 implementation 완료 후 적용.

(implementation 후 작성)

# Task Context Review — `PUT /posts/{id}` + `DELETE /posts/{id}`

- **Target Task Context**: `.agent-os/tasks/2026-05-16-post-update-delete.md`
- **Reviewed**: 2026-05-16
- **Procedure**: `task-context-flow` Skill 4~6단계 + `loop.md` phase 1 명시 호출 (retro action item 1)

---

## 1. AI Summary (사전 이해)

### Task goal
CRUD 완성. PUT/DELETE /posts/{id} 추가. viewCount/createdAt 보존. 이전 Forbidden(글 수정·삭제 out of MVP scope) 명시적 해제.

### Change scope (초안)
신규 1 (PostUpdateRequest) + 수정 4 (Controller/Service/Post/테스트). PostNotFoundException + RESOURCE_NOT_FOUND 재사용. 스키마 변경 없음.

### Acceptance criteria 핵심
PUT 200/400/404, DELETE 204/404. 같은 id 재DELETE → 404.

### Interpretive points
I1~I10 (URL, 시그니처, 트랜잭션 경계, 구현 흐름, E2E 시드 등)

### Open questions
U1~U9 잠정안 제시

---

## 2. Human Review (2026-05-16)

### 🔴 Major correction — Hard delete → **Soft delete**

> "hard delete가 아닌 soft delete가 맞을거야."

**잠정안의 hard delete(`repo.deleteById(id)`)를 soft delete로 뒤집음.**

#### 근거
- 게시판 도메인에서 글 삭제는 일반적으로 복원 가능성·감사 추적·삭제 시점 보존 등의 이유로 soft delete가 합리적.
- AI가 "MVP 단순화" 관점에서 hard delete를 잠정안으로 둔 건 도메인 일반성보다 구현 단순성을 과대평가한 사례.

#### Cascading effects (도미노)
soft delete 결정은 다음 모두를 일으킴:

1. **신규 스키마 변경**: V2 Flyway 마이그레이션 필요. `posts` 테이블에 `deleted_at TIMESTAMP WITH TIME ZONE NULL` 컬럼 추가.
2. **`Post` 엔티티 변경**:
   - `private Instant deletedAt;` 필드 추가
   - `delete(Instant deletedAt)` 도메인 메서드 추가 (mutable — `update(...)`와 함께)
   - 클래스 레벨 `@SQLRestriction("deleted_at IS NULL")` 추가 — 모든 entity 기반 조회를 자동 필터
3. **`PostRepository.incrementViewCount` 쿼리 변경**: `@Modifying @Query`는 `@SQLRestriction` 영향 안 받음 → 명시적으로 `AND p.deletedAt IS NULL` 추가.
4. **`PostService.delete` 의미 변경**: `repo.deleteById(id)` → `findById(id).orElseThrow().delete(clock.instant())` + JPA dirty checking이 UPDATE 발행.
5. **신규 회귀 테스트 필요**: `delete then get → 404`, `delete then list → 제외됨`, `delete twice → 404`, soft-deleted post의 viewCount는 증가 안 됨.
6. **이전 Task Context의 Forbidden 일부 뒤집힘**:
   - "새 Flyway 마이그레이션 추가 금지" → 해제 (V2 추가)
   - "hard delete만" → "soft delete만" (방향 반대)
7. **PostResponse는 `deletedAt` 노출 안 함** — soft-deleted post 자체가 조회 안 되므로 응답에 절대 등장 안 함. 필드 추가 불필요.

### Confirmed (consistent with recommendation)
- **U1**: PUT은 `title`, `content`만 수정 (`author`는 보존, body에 있어도 무시) ✓
- **U2~U9** 모두 권장안 채택
- **I1~I10** 모두 OK

### Resolved decisions table

| # | 최종 결정 |
|---|---|
| U1 | title/content만 수정 가능. author 무시 (body에 있어도 거부 안 함). |
| U2 | full PUT — title/content 둘 다 required. |
| U3 | DELETE → 204 No Content (body 없음). |
| U4 | last-write-wins (no `@Version`). |
| U5 | PostCreateRequest와 동일 검증 (title ≤ 80, content ≤ 5000). |
| U6 | `updatedAt`은 서버 시점 overwrite. |
| U7 | `viewCount` 보존 (PUT/DELETE 영향 X). |
| U8 | `PostNotFoundException` + `RESOURCE_NOT_FOUND` 재사용. |
| U9 | `post.update(...)` + `post.delete(...)` 도메인 메서드 (setter 미노출). |
| **U10 (신규)** | **Soft delete** — `deleted_at` 컬럼 + `@SQLRestriction("deleted_at IS NULL")`. Hard delete 아님. |

---

## 3. Trace of changes applied to Task Context

| 시점 | 위치 | 변경 |
|---|---|---|
| 2026-05-16 (초안) | 전 섹션 | hard delete 가정으로 9개 섹션 + U1~U9 마커 |
| 2026-05-16 (이 review 후) | Goal / Background | "soft delete" 명시. cascading effects 6개 항목 명기. |
| 2026-05-16 (이 review 후) | Related Files | V2 마이그레이션 신규 항목. PostRepository 쿼리 수정. Post `@SQLRestriction` 추가. |
| 2026-05-16 (이 review 후) | Change Scope | V2 SQL 신규, Post 엔티티 변경 폭 증가, PostRepository 쿼리 수정. |
| 2026-05-16 (이 review 후) | Acceptance Criteria | DELETE 후 GET 404, list 제외, viewCount 증가 안 됨 케이스 추가. |
| 2026-05-16 (이 review 후) | Verification | curl 시나리오에 soft delete 확인 추가. |
| 2026-05-16 (이 review 후) | Forbidden | "Flyway 추가 금지" 해제. "hard delete만" → "soft delete만". "deletedAt 외 audit 컬럼 추가 금지" 신설. |
| 2026-05-16 (이 review 후) | [검토 필요] 마커 5곳 | 모두 제거 (U1·U2·U5·U6·U9 등) |

---

## 4. Learning notes

- **첫 Major correction이 schema-level cascade를 일으킨 사례.** 이전 Major correction(GET 페이지네이션)은 API 표면만 바꿨지만, 이번은 schema + entity + repository + service 흐름이 동시에 변함. AI는 "MVP 단순성"에 끌려 hard delete를 기본으로 두었지만, 도메인의 통상 관행을 우선 고려했어야 함.
- **anchoring 우려 일부 해소.** 직전 retro에서 17/17 잠정안 채택률에 anchoring bias를 의심했는데, 이번엔 U10이라는 새 결정이 사용자 발화로 추가됨 (잠정안에 없었던 항목). 즉 사용자가 잠정안을 단순 승인하는 게 아니라 능동적으로 도메인 판단을 하고 있다는 증거. 다만 1 데이터 포인트.
- **U1(권장안 채택)도 명시 발화로 굳혔다.** "author는 수정 불가"는 잠정안과 동일하지만 사용자가 직접 발화함 — 잠정안 anchoring이 아닌 능동 확인. 향후 review.md 작성 시 "사용자가 명시한 항목"과 "사용자가 침묵한 항목"을 구분하면 anchoring bias 모니터링이 가능.
- **`@SQLRestriction` (Hibernate 6+)이 첫 도입.** Hibernate 5의 `@Where` 후속. Repository 쿼리(@Modifying) 쪽엔 자동 적용 안 되므로 explicit filter가 별도 필요 — 본문에 명시.
- **회귀 위험 ↑**: soft delete 도입으로 `GET /posts` (list)의 결과 셋이 동적으로 변할 수 있음. 기존 GET 테스트는 DELETE를 호출하지 않으므로 안전. 새 DELETE+GET 테스트는 `@BeforeEach clearDatabase()`로 격리.

## 5. Action items for Phase 2

- [ ] `.agent-os/batches/2026-05-16-post-update-delete.json` plan 작성 (retro action item)
- [ ] `tdd-endpoint` Skill로 4라운드 진행
- [ ] Phase 3에서 `harness.md` 명시 walk (retro action item)

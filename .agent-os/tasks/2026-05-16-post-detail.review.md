# Task Context Review — `GET /posts/{id}`

- **Target Task Context**: `.agent-os/tasks/2026-05-16-post-detail.md`
- **Reviewed**: 2026-05-16
- **Procedure**: `task-context-flow` Skill 4~6단계 — AI 요약 → 사용자 교정 → 본문 갱신
- **Note**: 이 review는 신규 `task-context-flow` Skill의 **첫 실전 적용**이다.

---

## 1. AI Summary (사전 이해)

### Task goal
단일 게시글 상세 조회. 호출 시 `viewCount`를 정확히 +1 증가시키고 갱신된 값을 응답에 포함. 존재하지 않는 id는 신규 `ErrorCode.RESOURCE_NOT_FOUND`로 404 반환. POST/list가 미뤄둔 viewCount 정책의 정식 처리 자리.

### Change scope
- 수정 5개 파일: `PostController`, `PostService`, `PostRepository`, `ErrorCode`, `RequestValidationExceptionHandler`
- 신규 1개: `PostNotFoundException` (10줄 이내)
- 테스트 2개: 단위 1 + E2E 3~4
- 불변: `Post` 엔티티, `PostResponse`, 스키마, 의존성, 기존 17개 테스트

### Acceptance criteria (핵심)
| 케이스 | 기대 |
|---|---|
| GET 존재하는 id | 200 + 7필드 body, viewCount=호출 후 값 |
| 같은 id N회 호출 | viewCount 정확히 +N (atomic UPDATE) |
| GET /posts/99999 | 404 + ErrorResponse(code=RESOURCE_NOT_FOUND) |
| GET /posts/-1 | 404 |
| GET /posts/abc | 400 (Spring 기본) |
| 회귀 | 기존 17개 그대로 |

### Risks watched
- 동시성 (atomic UPDATE로 해결)
- REST safe/idempotent 위반 (수용, 관행)
- 트랜잭션 read-your-write (`@Transactional` non-readOnly + `clearAutomatically`)
- 음수 id (자연스럽게 404)

### Interpretive points (I1~I10)
| # | AI 해석 |
|---|---|
| I1 | URL: `GET /posts/{id}` |
| I2 | id 타입: `Long` |
| I3 | Service: `public PostResponse find(Long id)` |
| I4 | `@Transactional` (write 포함, non-readOnly) |
| I5 | 순서: incrementViewCount → 0이면 throw → findById → PostResponse.from |
| I6 | 응답 record: 기존 `PostResponse` 재사용 |
| I7 | E2E 시드: `PostRepository.saveAll(...)` 직접 |
| I8 | 404 path: `request.getRequestURI()` |
| I9 | 핸들러 위치: 기존 `RequestValidationExceptionHandler`에 메서드 추가 |
| I10 | 음수 id: 별도 검증 없이 404로 자연 처리 |

### Open questions (D1~D7)
| # | 잠정안 |
|---|---|
| D1 | 매 GET마다 +1, 동기, 세션·IP·작성자 무관 |
| D2 | `PostResponse` 그대로 재사용 |
| D3 | `RESOURCE_NOT_FOUND` |
| D4 | atomic UPDATE 쿼리 |
| D5 | REST safe/idempotent 위반 수용 |
| D6 | 캐시 헤더 추가 안 함 |
| D7 | `PostNotFoundException extends RuntimeException` 신규 |

---

## 2. Human Review (2026-05-16)

### I1~I10 — 해석 검증
**모두 OK. 교정 사항 없음.**

### D1~D7 — 권장안으로 확정

| 결정 | 채택 안 | 비고 |
|---|---|---|
| D1 | 매 GET마다 +1, 동기 | 가장 단순. 분석용 인플레이션은 알려진 트레이드오프. |
| D2 | `PostResponse` 재사용 | POST·GET-list와 응답 계약 통일. |
| D3 | `RESOURCE_NOT_FOUND` | 후속 자원에도 재사용 가능. |
| D4 | atomic UPDATE (`@Modifying @Query` + `clearAutomatically=true`) | 동시성 안전 + read-your-write. |
| D5 | REST safe/idempotent 위반 수용 | 운영 흔한 관행. |
| D6 | 캐시 헤더 추가 안 함 | MVP 범위 외. |
| D7 | `PostNotFoundException` 신규 | `ErrorResponse` 계약 일관성. |

**No major corrections.** Task Context 잠정안이 사용자 의도와 7/7 + 10/10 일치.

---

## 3. Trace of changes applied to Task Context

| 시점 | 위치 | 변경 |
|---|---|---|
| 2026-05-16 (초안) | 전 섹션 | 9개 섹션 초안 작성, 마커 4곳, 하단 HTML 결정 주석 블록 |
| 2026-05-16 (이 review 후) | Related Files / Change Scope | `**[검토 필요: D4]**`, `**[검토 필요: D7]**` 마커 제거 |
| 2026-05-16 (이 review 후) | Acceptance Criteria | `**[검토 필요: D1]**` 마커 제거 |
| 2026-05-16 (이 review 후) | Forbidden | `**[검토 필요: D6]**` 마커 제거 |
| 2026-05-16 (이 review 후) | 하단 | D1~D7 HTML 결정 주석 블록 제거 |
| 2026-05-16 (이 review 후) | Related Files / Change Scope | `incrementViewCount`에 `clearAutomatically = true` 명시 (read-your-write 보장) |

---

## 4. Learning notes

- **`task-context-flow` Skill의 첫 실전 적용** — 추출 직후 곧바로 활용된 사례. Skill의 7단계가 큰 마찰 없이 적용됨.
- **잠정안 채택률 100%** (10 + 7 = 17개 결정 전부 권장안). 두 가지 해석 가능:
  - (a) Task Context 초안의 결정 트리가 사용자 의도와 잘 정렬돼 있었음
  - (b) AI가 권장하면 사용자가 자연스럽게 따르는 anchoring 편향 가능성
  - 향후 베이스라인 비교 (적용 전/후 잘못된 가정 횟수) 도입 시 (a)/(b) 구분 가능. 현재는 트래킹 부족.
- **신규 패턴 2개 도입 명시화**: atomic UPDATE 쿼리 (`@Modifying @Query` + `clearAutomatically`)와 도메인 예외 매핑 (`PostNotFoundException` → `RequestValidationExceptionHandler`). 둘 다 구현 후 `.claude/rules/conventions.md`에 반영 검토 (재발 패턴이면 컨벤션으로 승격).
- **`task-context-flow` Skill 자체 개선 후보**: 잠정안 채택률 100% 사례가 누적되면 "AI 추측이 너무 잘 맞는 패턴 — anchoring 의심" 경고를 Skill에 추가할 수 있음.

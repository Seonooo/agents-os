# Task Context

## Goal

`GET /posts`에 **검색 필터 `?q=`** 를 추가한다. 검색어는 게시글 검색에 사용되며, 기존 페이지네이션(`?page`, `?size`) 및 서버 고정 정렬(`createdAt DESC, id DESC`)과 조합 가능하다. `?q=` 미지정 또는 빈 문자열이면 기존 동작(전체 목록)과 동일.

## Background

이번 task는 두 가지 목적을 동시에 수행한다.

1. **기능 추가**: MVP의 `GET /posts`에 첫 검색 능력 도입. 게시글 누적 시 목록 스크롤만으로는 원하는 글을 찾기 어렵다는 한계 해소.
2. **`.agent-os/loop.md` 검증**: 직전 retro의 action item으로 `loop.md`에 task-context-flow / tdd-endpoint / spring-boot-4-verify Skill을 step 3·7에 인라인 링크로 박았다 (`f26b7bf` 이후). 이번 task는 그 새 loop이 실제 작동하는지 검증하는 첫 실전 사례. 따라서 **변경 폭은 최소화**하고, AI 첫 plan과 사람이 줄인 plan의 차이 / Reviewer·Tester 모자에서 잡힌 failure mode를 `2026-05-23-post-search.review.md`의 `## Loop Validation Record` 섹션에 기록한다.

`hard-rules.md`의 *"Out of MVP scope: Pagination/search/filter beyond the existing `?page`, `?size`"* 조항을 **이번 task 한정으로 명시 해제**한다 (`?q=` 한 파라미터에 한해). 다른 검색·필터 파라미터(`?author=`, `?from=`, `?to=` 등)는 그대로 금지.

## Related Files / Areas

**변경 후보 (will touch)**
- `src/main/java/com/example/board/post/PostController.java` — `list(...)` 메서드 시그니처에 `@RequestParam(required=false) String q` 추가
- `src/main/java/com/example/board/post/PostService.java` — `list(int page, int size, String q)` 시그니처 확장 (또는 오버로드)
- `src/main/java/com/example/board/post/PostRepository.java` — 검색용 derived query 또는 `@Query` 신규 메서드
- `src/test/java/com/example/board/post/PostServiceTest.java` — 검색 단위 테스트 추가
- `src/test/java/com/example/board/post/PostControllerE2ETest.java` — `?q=` E2E + REST Docs 스니펫

**참고용 (reference only, 변경 없음)**
- `Post.java`, `PostResponse.java`, `PostCreateRequest.java`, `PostUpdateRequest.java` — 응답 계약 불변
- `PageResponse.java` — 응답 구조 불변, content 길이만 검색 결과에 따라 달라짐
- `ErrorResponse.java`, `ErrorCode.java`, `RequestValidationExceptionHandler.java` — 검증 실패 응답 계약 그대로
- `db/migration/V1__create_posts.sql`, `V2__add_posts_deleted_at.sql` — 스키마 변경 없음

**도메인 영역**: `com.example.board.post`

## Existing Patterns

기존 컨벤션 그대로 (POST/GET/GET-by-id/PUT/DELETE에서 확립):

- 컨트롤러 메서드 파라미터에 직접 `@Min/@Max/@NotBlank/@Size` (Spring 6.1+ MVC 메서드 검증, `@Validated` 클래스 어노테이션 사용 안 함)
- `@Transactional(readOnly = true)` 읽기 메서드
- 정렬은 서버 고정 (`PostService.LIST_SORT`) — `?q=` 결과도 동일 정렬, **relevance score 도입 안 함**
- Soft delete는 `@SQLRestriction("deleted_at IS NULL")`이 자동 처리 — 검색 쿼리에 명시 불필요 (단, `@Query` HQL을 쓰면 자동 적용 X → 명시 필요. soft-delete gotcha 참고)
- DTO는 record, 컨트롤러는 얇게 서비스에 위임
- 검증 실패 → `400 VALIDATION_FAILED` + `errors[].field`
- TDD 4라운드 (`tdd-endpoint` Skill)
- E2E는 Testcontainers Postgres

## Change Scope

Can change:

- `PostController.list(...)` — 파라미터 1개 추가 (`q`), 기존 동작 보존
- `PostService.list(...)` — 시그니처 확장 또는 오버로드 (어느 쪽이든 기존 호출자 호환)
- `PostRepository` — 검색용 쿼리 메서드 1개 추가
- `PostServiceTest`, `PostControllerE2ETest` — 테스트 메서드 추가만

Do not change:

- `Post.java`, `PostResponse.java`, `PostCreateRequest.java`, `PostUpdateRequest.java`, `PageResponse.java`
- `ErrorResponse.java`, `ErrorCode.java`, `RequestValidationExceptionHandler.java`
- `BoardApplication.java`, `application.yml`, `build.gradle.kts`
- `support/TestcontainersConfiguration.java`, `BoardApplicationTests.java`
- `src/main/resources/db/migration/**` — 스키마 변경 없음, 새 인덱스 없음
- 기존 29개 테스트 메서드 본문 (회귀로 확인만)

## Constraints

- **Security**: 사용자 입력 문자열을 LIKE 쿼리에 사용 — JPA 파라미터 바인딩으로 SQL injection 차단. `%`·`_` 와일드카드 문자가 사용자 입력에 포함되면 SQL 와일드카드로 그대로 동작 (known MVP behavior — escape 처리 안 함). PII 없음.
- **Performance**: 검색은 `LOWER(title) LIKE ?` 풀 스캔. MVP 데이터량에선 수용 가능. **인덱스 추가 금지** (hard-rules: V2 외 추가 마이그레이션 task 범위 외). 페이지 메타용 `COUNT(*)`도 동일 조건으로 한 번 더 실행됨. **검색은 단일 `Page<Post>` 쿼리로 처리 — N+1 패턴(예: `findAllById` 후속 호출) 금지.**
- **Operations**: Hibernate SQL 로그에 `WHERE LOWER(title) LIKE ? AND deleted_at IS NULL` 형태 확인. `format_sql: true` 그대로. (Spring Data가 derived `containingIgnoreCase`를 `lower(...)` LIKE로 변환, `@SQLRestriction`이 `deleted_at IS NULL` 자동 추가.)
- **Compatibility**: `?q=` 미지정 시 기존 응답 바이트-동일. 페이지네이션 응답 구조(`PageResponse`) 불변. 기존 29개 테스트 그대로 통과.
- **Time**: loop.md 검증이 본 목적이므로 빠른 사이클(1~2시간) 안에 끝낸다. 기능 깊이는 최소.

## Acceptance Criteria

### `?q=` 미지정 (회귀)
- `GET /posts` 응답이 기존과 동일 (페이지·정렬·content)
- `GET /posts?page=1&size=10` 도 기존 동작 유지

### `?q=` 빈 문자열
- `GET /posts?q=` → 미지정과 동일 처리 (200, 전체 목록). 빈 문자열을 400으로 거부하지 않음.

### `?q=` 검색 성공
- `GET /posts?q=spring` → `200 OK` + `PageResponse`
  - 매칭 범위: **`title` only** (content·author 제외 — MVP 단순화)
  - 매칭 방식: **case-insensitive partial LIKE** (Spring Data derived `containingIgnoreCase` = `LOWER(title) LIKE LOWER('%q%')`)
  - 정렬: 기존 그대로 (`createdAt DESC, id DESC`) — relevance 도입 안 함
  - 페이지 메타: `totalElements`는 **필터된 결과의 총 개수**, `totalPages` 동일

### `?q=` 매칭 0건
- `200 OK` + `{content: [], page: 0, size: 20, totalElements: 0, totalPages: 0}`

### `?q=` 검증 실패
- 길이 상한 `@Size(max=100)`. 초과 시 → `400 VALIDATION_FAILED` + `errors[].field="q"`

### Soft delete 회귀
- `?q=`로 검색되는 결과에 soft-deleted post 포함 안 됨 (HQL `@Query`라면 명시적 `AND deletedAt IS NULL` 필요)

### 조합
- `GET /posts?q=hello&page=1&size=5` → 필터된 결과에서 page 1, size 5
- `GET /posts?q=hello&size=200` → `400` (size 범위 위반 우선)

### 회귀
- 기존 29개 E2E 테스트 + 서비스 단위 테스트 모두 통과
- POST·GET-by-id·PUT·DELETE 동작 불변
- REST Docs 기존 스니펫 모두 재생성 가능

## Verification

- **Tests to run**:
  - `./gradlew test --tests "com.example.board.post.*"`
  - 전체: `./gradlew test`
- **Logs to check**:
  - 검색 SELECT: `... WHERE (LOWER(title) LIKE ? OR LOWER(content) LIKE ?) AND deleted_at IS NULL ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?` (잠정안 기준)
  - 카운트 SELECT: 동일 WHERE 절로 `COUNT(*)` 한 번
  - `?q=` 미지정: 기존 `SELECT ... ORDER BY ... LIMIT ? OFFSET ?` (변경 없음)
- **API/UI to check**:
  ```bash
  # seed
  curl -X POST http://localhost:8080/posts -H "Content-Type: application/json" \
       -d '{"author":"alice","title":"Spring Boot 4","content":"new release"}'
  curl -X POST http://localhost:8080/posts -H "Content-Type: application/json" \
       -d '{"author":"bob","title":"Hello","content":"world"}'

  # 미지정 — 회귀
  curl -i "http://localhost:8080/posts"
  # 기대: 기존 응답과 동일

  # 빈 q — 잠정안: 미지정과 동일
  curl -i "http://localhost:8080/posts?q="

  # 매칭
  curl -i "http://localhost:8080/posts?q=spring"
  # 기대: 200, content에 "Spring Boot 4"만, totalElements=1

  # 매칭 0건
  curl -i "http://localhost:8080/posts?q=zzz"
  # 기대: 200, content=[], totalElements=0

  # 조합
  curl -i "http://localhost:8080/posts?q=o&page=0&size=1"
  # 기대: 200, content 길이 1, totalElements=2 (Hello, Boot)

  # 길이 초과 (q=101자)
  curl -i "http://localhost:8080/posts?q=$(printf 'a%.0s' {1..101})"
  # 기대: 400, errors[].field="q"
  ```

## Forbidden

- **`hard-rules.md`의 "Pagination/search/filter beyond `?page`, `?size`" 조항 — `?q=` 한 파라미터에 한해 이번 task 한정으로 해제.** 다른 검색·필터 파라미터(`?author=`, `?from=`, `?to=`, `?category=` 등) 추가는 그대로 금지.
- 새 의존성 추가 금지
- 새 Flyway 마이그레이션 추가 금지 (스키마 변경 없음)
- 인덱스 추가 금지 (V3 도입 금지)
- `sort` 쿼리 파라미터 도입 금지 — relevance score 도 도입 안 함
- `?q=` 외 검색 파라미터 도입 금지 (위와 동일)
- `PostResponse` 필드 변경 금지 (highlight·snippet 필드 추가 금지)
- 페이지네이션 응답 구조(`PageResponse`) 변경 금지
- Spring Data `Specification`, QueryDSL 등 신규 추상화 도입 금지 — 단순 `@Query` 또는 derived method로 충분
- Native SQL 사용 금지 (HQL/JPQL 또는 derived query만)
- 클라이언트 입력을 String concat으로 쿼리에 넣기 금지 (반드시 파라미터 바인딩)
- 검색 결과 캐싱 도입 금지 (MVP 범위 외)
- 비동기·스트리밍 응답 도입 금지
- 인증/권한 도입 금지
- **N+1 패턴 금지** — 검색은 단일 `Page<Post>` 쿼리로 처리. `findAllById`, 후속 fetch loop 등 2-step 패턴 금지.
- **와일드카드 escape 도입 금지** (MVP 한정 결정) — 사용자 입력의 `%`/`_`는 SQL 와일드카드로 그대로 동작
- **`title` 외 필드(`content`, `author`) 검색 도입 금지** (이번 task 한정)

## Open Questions (resolved)

- **Q-A**: `?q=` 빈 문자열 → **미지정과 동일** 처리. (잠정안 채택)
- **Q-D**: 매칭 방식 → **case-insensitive partial LIKE** (derived `containingIgnoreCase`). (잠정안 채택)
- **Q-E**: `q` 길이 상한 → **`@Size(max=100)`**. (잠정안 채택)
- **Q-F**: 유니코드 케이스 폴딩 → **영문만 case-insensitive** 가정. Postgres `LOWER` locale 영향은 MVP 범위 외. (잠정안 채택)
- ~~Q-B (escape)~~ → drop (Forbidden 참조)
- ~~Q-C (검색 범위)~~ → title only (Acceptance Criteria 참조)

# Task Context

## Goal

`PUT /posts/{id}`로 게시글의 `title`과 `content`를 수정하고, `DELETE /posts/{id}`로 게시글을 **soft delete** 한다. 두 작업 모두 `viewCount`는 보존. soft-deleted post는 모든 조회 경로(GET, GET list, GET-by-id, PUT, DELETE)에서 보이지 않으며 viewCount도 증가시키지 않는다.

## Background

MVP 게시판의 CRUD 완성. 지금까지: POST(생성), GET `/posts`(목록), GET `/posts/{id}`(상세). 이번 작업으로 **U**(update)와 **D**(delete) 추가.

**이전 Task Context들의 `Forbidden`("글 수정·삭제 추가 금지") 및 `hard-rules.md`의 "Out of MVP scope: Post update / delete endpoints" 조항을 명시적으로 해제**한다. 두 조항 모두 *"without an explicit task"* 단서를 두므로 위반이 아니라 **계획된 해제**.

**Soft delete 채택 (사용자 검토에서 hard delete 잠정안 뒤집음).** Cascading effects:
- 신규 V2 Flyway 마이그레이션 (`deleted_at TIMESTAMP WITH TIME ZONE NULL` 컬럼 추가).
- `Post` 엔티티: `deletedAt` 필드 + `delete(Instant)` 도메인 메서드 + 클래스 레벨 `@SQLRestriction("deleted_at IS NULL")`.
- `PostRepository.incrementViewCount` 쿼리에 `AND p.deletedAt IS NULL` 명시 추가 (`@Modifying @Query`는 `@SQLRestriction` 영향 안 받음).
- `PostNotFoundException` + `RESOURCE_NOT_FOUND` 코드를 **첫 재사용** (count 2 도달 → Skill 후보 충족).

자세한 결정 경위는 `2026-05-16-post-update-delete.review.md` 참조.

## Related Files / Areas

**변경 후보 (will touch)**
- `src/main/java/com/example/board/post/PostController.java` — `@PutMapping("/{id}")`, `@DeleteMapping("/{id}")` 추가
- `src/main/java/com/example/board/post/PostService.java` — `update(Long id, PostUpdateRequest)`, `delete(Long id)` 추가
- `src/main/java/com/example/board/post/Post.java` — `deletedAt` 필드 + getter + `update(...)` / `delete(...)` 메서드 + `@SQLRestriction` 어노테이션
- `src/main/java/com/example/board/post/PostRepository.java` — `incrementViewCount` 쿼리에 `AND p.deletedAt IS NULL` 추가
- `src/main/java/com/example/board/post/PostUpdateRequest.java` — **신규** record (`@NotBlank @Size` 어노테이션)
- `src/main/resources/db/migration/V2__add_posts_deleted_at.sql` — **신규** Flyway 마이그레이션
- `src/test/java/com/example/board/post/PostServiceTest.java` — update/delete 단위 테스트
- `src/test/java/com/example/board/post/PostControllerE2ETest.java` — PUT/DELETE E2E + soft delete 회귀 테스트

**참고용 (reference only, 변경 없음)**
- `PostNotFoundException.java`, `ErrorCode.java` — 그대로 재사용
- `RequestValidationExceptionHandler.java` — `handlePostNotFound` 그대로 재사용
- `PostCreateRequest.java`, `PostResponse.java` — 그대로 (deletedAt 노출 안 함)

**도메인 영역**: `com.example.board.post`

## Existing Patterns

POST/GET/GET-by-id에서 확립된 모든 컨벤션 그대로:
- 생성자 주입, 필드 주입 금지
- DTO는 `record` (`PostUpdateRequest` 신규, `PostResponse` 재사용)
- Lombok 미사용
- 컨트롤러 얇게, 비즈니스 규칙은 서비스에
- 검증: `@NotBlank @Size` (Spring 6.1+ MVC 검증, `@Validated` 없이)
- 404: `PostNotFoundException` → `ErrorResponse(RESOURCE_NOT_FOUND)` (도메인 예외 매핑 — **재사용**)
- 단위 테스트: Mockito + AssertJ. E2E: Testcontainers Postgres
- TDD 4라운드 (`tdd-endpoint` Skill)

신규 패턴 (이번 도입):
- **Soft delete via `@SQLRestriction`** — Hibernate 6+ 어노테이션 (`@Where` 후속). 클래스 레벨에 `@SQLRestriction("deleted_at IS NULL")` → 모든 entity 기반 조회 자동 필터. `@Modifying @Query` 쿼리는 자동 적용 X, 명시적 필터 추가 필요.
- **Mutable 도메인 메서드** — `post.update(...)`, `post.delete(...)` (setter 미노출). 첫 사례.
- **Flyway V2 마이그레이션** — 첫 V1 이후 추가.

## Change Scope

Can change:

- `PostController.java` — `@PutMapping("/{id}")`, `@DeleteMapping("/{id}")` 메서드만 추가
- `PostService.java` — `update(...)`, `delete(...)` public 메서드 추가
- `Post.java` — `deletedAt` 필드 + getter + `update(...)` + `delete(...)` 메서드 + 클래스 `@SQLRestriction`
- `PostRepository.java` — `incrementViewCount` 쿼리에 `AND p.deletedAt IS NULL` 추가 (다른 메서드 변경 금지)
- `PostUpdateRequest.java` — 신규 record
- `V2__add_posts_deleted_at.sql` — 신규 Flyway 마이그레이션
- `PostServiceTest.java`, `PostControllerE2ETest.java` — 테스트 메서드 추가

Do not change:

- `PostCreateRequest`, `PostResponse`, `PageResponse`, `ErrorResponse`, `ErrorCode`
- `PostRepository`의 `findAll(Pageable)` 시그니처 (자동 필터로 충분)
- `RequestValidationExceptionHandler` — 기존 핸들러 본문 변경 금지
- `Post.java`의 기존 생성자·withId·기존 getter — 그대로
- `BoardApplication.java`, `application.yml`, `build.gradle.kts`
- `support/TestcontainersConfiguration.java`, `BoardApplicationTests.java`
- 기존 23개 테스트의 모든 메서드

## Constraints

- **Security**: 인증·인가 범위 외 — 누구나 누구의 글도 수정·삭제 가능. **MVP의 알려진 보안 한계**. PII 없음. `deletedAt`은 응답 노출 안 함.
- **Performance**: PUT은 `findById` + dirty checking UPDATE. DELETE는 `findById` + dirty checking UPDATE(deleted_at). `@SQLRestriction`이 모든 조회에 `WHERE deleted_at IS NULL` 자동 추가 — 인덱스 없이도 MVP 데이터량에서 수용 가능.
- **Operations**: Hibernate SQL 로그에 UPDATE 가시. soft delete된 행은 DB에 남아있음 (감사 추적 가능).
- **Compatibility**: 기존 23개 테스트 그대로 통과 — DELETE를 호출하지 않으므로 영향 없음. `PostResponse`, `PostCreateRequest` JSON 계약 불변.
- **Time**: 학습 일정 내.

## Acceptance Criteria

### PUT `/posts/{id}` 성공
- 요청 body `{title, content}` (둘 다 필수)
- HTTP `200 OK` + 갱신된 `PostResponse` (7필드)
- `title`, `content`는 요청 값으로 갱신
- `updatedAt`은 서버 시점으로 갱신 (요청에 있어도 무시)
- `createdAt`, `id`, `author`, `viewCount`는 보존
- 요청 body에 `author` 필드가 있어도 무시 (수정 불가, 거부도 아님)

### PUT 검증 실패
- `title` 누락·공백만·길이 초과(`> 80`) → `400` + `ErrorResponse(VALIDATION_FAILED, errors[].field="title")`
- `content` 누락·공백만·길이 초과(`> 5000`) → `400` + 동일 패턴
- 비숫자 id → `400` (Spring 기본 type-mismatch)

### PUT 자원 없음 / soft-deleted
- 존재하지 않는 id → `404` + `ErrorResponse(RESOURCE_NOT_FOUND)`
- soft-deleted 된 id → `404` (`@SQLRestriction`이 findById 결과를 빈 Optional로 만듦)

### DELETE `/posts/{id}` 성공
- 존재하는 id → HTTP `204 No Content` (body 없음)
- DB에 행은 남아있고 `deleted_at`만 현재 시각으로 설정
- 같은 id를 두 번 DELETE → 첫 호출 `204`, 두 번째 호출 `404 RESOURCE_NOT_FOUND`

### DELETE 자원 없음 / 잘못된 id
- 존재하지 않는 id → `404` + `ErrorResponse(RESOURCE_NOT_FOUND)`
- 비숫자 id → `400` (Spring 기본)

### Soft delete 회귀 영향
- DELETE된 post는 `GET /posts` (페이지 목록)에 포함되지 않음. `totalElements` 카운트 감소.
- DELETE된 post에 `GET /posts/{id}` → `404`
- DELETE된 post에 viewCount 증가 시도 → `incrementViewCount`가 0 행 affect → `PostNotFoundException` → 404

### 동시성
- 동시 PUT N회: last-write-wins (no `@Version`)
- 동시 DELETE N회: 첫 번째 204, 나머지는 race 결과에 따라 204 또는 404 가능 (acceptable — DB UPDATE의 `WHERE deleted_at IS NULL` 가드는 없으므로 다중 DELETE가 모두 성공할 수도 있음. 클라이언트 관점에서 차이 없음.)

### 회귀
- 기존 23개 테스트 모두 통과
- `PostResponse`, `PostCreateRequest` JSON 형식 불변
- POST·GET·GET-by-id 동작 불변 (DELETE 호출 안 함)

## Verification

- **Tests to run**:
  - `./gradlew test --tests "com.example.board.post.*"`
  - 전체: `./gradlew test`
- **Logs to check**:
  - V2 Flyway 마이그레이션 적용: `Migrating schema "public" to version "2 - add posts deleted at"`
  - PUT: Hibernate `SELECT ... WHERE id=? AND deleted_at IS NULL` + `UPDATE posts SET title=?, content=?, updated_at=? WHERE id=?`
  - DELETE: `SELECT ... WHERE id=? AND deleted_at IS NULL` + `UPDATE posts SET deleted_at=? WHERE id=?`
  - GET/GET-list: 모든 SELECT에 `WHERE deleted_at IS NULL` 자동 추가됨
- **API/UI to check**:
  ```bash
  # 글 생성
  ID=$(curl -s -X POST http://localhost:8080/posts \
       -H "Content-Type: application/json" \
       -d '{"author":"alice","title":"Original","content":"Body"}' | jq -r .id)

  # PUT — title/content 수정
  curl -i -X PUT http://localhost:8080/posts/$ID \
       -H "Content-Type: application/json" \
       -d '{"title":"Updated","content":"New body"}'
  # 기대: 200, body의 title=Updated, viewCount 그대로, updatedAt 갱신

  # PUT 검증 실패 (title 누락)
  curl -i -X PUT http://localhost:8080/posts/$ID \
       -H "Content-Type: application/json" \
       -d '{"content":"only content"}'
  # 기대: 400, ErrorResponse(VALIDATION_FAILED, errors[].field=title)

  # DELETE — 정상 (soft)
  curl -i -X DELETE http://localhost:8080/posts/$ID
  # 기대: 204 No Content

  # DELETE 후 GET — 404 (soft-deleted 안 보임)
  curl -i http://localhost:8080/posts/$ID
  # 기대: 404 RESOURCE_NOT_FOUND

  # DELETE 후 GET list — 제외
  curl -i http://localhost:8080/posts
  # 기대: 200, content에 $ID 없음, totalElements 감소

  # DELETE 같은 id 재시도
  curl -i -X DELETE http://localhost:8080/posts/$ID
  # 기대: 404 RESOURCE_NOT_FOUND

  # PUT soft-deleted id
  curl -i -X PUT http://localhost:8080/posts/$ID \
       -H "Content-Type: application/json" \
       -d '{"title":"x","content":"y"}'
  # 기대: 404 RESOURCE_NOT_FOUND
  ```

## Forbidden

- 새 의존성 추가 금지
- `PostCreateRequest` 변경 금지 (POST 계약 보존)
- `PostResponse` 필드 구성 변경 금지 (`deletedAt` 노출 X — soft-deleted post는 응답에 절대 등장 안 함)
- `author` 필드 수정 허용 금지 (PUT body에 있어도 무시, 거부 X)
- `viewCount` 수정·리셋 허용 금지 (PUT/DELETE 사이드 이펙트로 변경 안 됨)
- `createdAt` 수정 허용 금지 (immutable)
- 인증/권한 도입 금지 (의도된 한계)
- 새 `ErrorCode` 값 추가 금지 (`RESOURCE_NOT_FOUND`, `VALIDATION_FAILED`만)
- `@Version` (optimistic locking) 도입 금지
- 비동기 / fire-and-forget 처리 도입 금지
- **Hard delete 도입 금지** (`repo.deleteById` 사용 금지)
- **`deletedAt` 외 audit 컬럼 추가 금지** (`deleted_by`, `delete_reason` 등 MVP 범위 외)
- **Soft-deleted post 복원 endpoint 도입 금지** (MVP 범위 외 — 별도 task 필요)
- `application.yml`의 `ddl-auto`, `format_sql` 변경 금지
- `RequestValidationExceptionHandler`의 기존 메서드 본문 변경 금지
- `Post`의 기존 생성자·withId·기존 getter 시그니처 변경 금지
- `PostRepository`에 신규 `@Modifying` 쿼리 추가 금지 (`incrementViewCount` 필터 추가만 허용)
- V2 외 추가 Flyway 마이그레이션 금지 (이번 task 범위)

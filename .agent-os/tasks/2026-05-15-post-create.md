# Task Context

## Goal

`POST /posts` 엔드포인트로 게시글 하나를 받아 PostgreSQL에 영속화하고, 생성된 자원을 응답 본문으로 반환한다. MVP의 두 핵심 기능(목록 조회 / 글 작성) 중 글 작성 측.

## Background

학습용 MVP 게시판의 **첫 도메인 기능**. 이전까지의 작업은 Spring Boot init(스모크 테스트 통과)에 그쳤다. 인증은 의도적으로 범위 밖이라 작성자 식별은 요청 본문의 `author` 문자열로 한정한다.

이 작업이 **앞으로 모든 도메인 기능의 기준 패턴**이 되므로 (다음 작업 후보: `GET /posts`, 추후 글 수정·삭제 등), 컨벤션 결정에 신중을 기한다.

## Related Files / Areas

**변경 후보 (will touch)**
- `src/main/java/com/example/board/post/` — 신규 패키지
- `src/test/java/com/example/board/post/` — 신규 테스트 패키지
- `src/main/resources/db/migration/V1__create_posts.sql` — 신규 (Flyway 마이그레이션)
- `build.gradle.kts` — Flyway 의존성 2줄 추가 (`flyway-core` + `flyway-database-postgresql`)
- `src/main/java/com/example/board/BoardApplication.java` — `Clock @Bean` 한 줄 추가 (테스트 가능성)

**참고용 (reference only)**
- `src/main/resources/application.yml` — `ddl-auto: validate`, `format_sql: true`
- `src/test/java/com/example/board/BoardApplicationTests.java` — `@SpringBootTest` + `@Import(TestcontainersConfiguration)` 예시
- `src/test/java/com/example/board/support/TestcontainersConfiguration.java` — Postgres `@ServiceConnection`

**도메인 영역**: `com.example.board.post`

## Existing Patterns

프로젝트 첫 도메인 기능이라 별도 선례 없음. `AGENTS.md`의 일반 컨벤션을 적용한다:

- 생성자 주입만 사용, 필드 주입 금지
- DTO는 Java 21 `record` (`PostCreateRequest`, `PostResponse`)
- JPA 엔티티는 클래스 — `Post` (JPA 제약상 record 불가)
- **Lombok 미사용**, 명시적 getter/생성자
- 컨트롤러는 얇게, 비즈니스 규칙은 서비스에
- E2E: `@SpringBootTest(webEnvironment=RANDOM_PORT)` + `@AutoConfigureMockMvc` + `@Import(TestcontainersConfiguration.class)`
- 단위 테스트: JUnit 5 + Mockito + AssertJ
- 시간 의존 로직은 `Clock` 주입으로 결정적 테스트 가능

## Change Scope

**Can change**
- `src/main/java/com/example/board/post/**` (신규)
- `src/test/java/com/example/board/post/**` (신규)
- `src/main/resources/db/migration/V1__create_posts.sql` (신규)
- `build.gradle.kts` — Flyway 의존성 2줄만
- `BoardApplication.java` — `Clock @Bean` 한 줄만

**Do not change**
- `application.yml`의 `ddl-auto` 값 (유지: `validate`)
- `support/TestcontainersConfiguration.java`
- `BoardApplicationTests.java` (이미 통과한 init 스모크 테스트)
- `settings.gradle.kts`, `gradle/wrapper/**`, `.gitignore`
- `build.gradle.kts`의 그 외 영역 (플러그인 블록, 기존 의존성 등)

## Constraints

- **Security**: 인증·인가 범위 외. `author`는 입력값만 받으며 위변조 가능(의도된 한계, MVP 학습용). 입력 길이 상한으로 페이로드 폭주 1차 방어. JSON 응답이라 HTML escape 불요. 로그·에러 응답에 자격증명·PII 포함 금지.
- **Performance**: 단일 INSERT. 인덱스는 PK만으로 충분. 명시적 SLO 없음 — 로컬에서 응답 < 200ms 정도면 OK.
- **Operations**: Hibernate SQL 로그 활성(`format_sql: true`). `ddl-auto: validate` 유지 — V1 마이그레이션 없으면 부팅 실패해야 정상.
- **Compatibility**: 신규 엔드포인트, 기존 계약 없음. URL `/posts`는 향후 `GET /posts` 등에서 공유 예정.
- **Time**: 학습 일정 내. 정확한 마감 없음.

## Acceptance Criteria

### Post 도메인
- `Post` 엔티티는 7개 필드를 가진다: `id (Long, BIGSERIAL)`, `author (String)`, `title (String)`, `content (String)`, `createdAt (Instant)`, `updatedAt (Instant)`, `viewCount (long)`.
- 생성 시:
  - `createdAt == updatedAt` (둘 다 서버 시점, `Clock` 주입으로 결정적)
  - `viewCount == 0`

### 성공 응답
- `POST /posts`에 유효한 JSON body `{author, title, content}` 요청 시:
  - HTTP `201 Created`
  - `Location: /posts/{id}` 헤더
  - 응답 body에 7개 필드(`id, author, title, content, createdAt, updatedAt, viewCount`) 모두 포함
  - `createdAt`/`updatedAt`은 ISO-8601 `Instant` (UTC)

### 검증 실패 응답 (Bean Validation)
- `author`: 누락·공백만·길이 초과(`> 30`) → `400 Bad Request`
- `title`: 누락·공백만·길이 초과(`> 80`) → `400`
- `content`: 누락·공백만·길이 초과(`> 5000`) → `400`
- 응답 body는 Spring Boot 4 기본 `ProblemDetail`(`application/problem+json`) 형식이며 **위반 필드명을 포함**한다.

### 기타
- 동일한 valid 요청을 N회 보내면 매번 새로운 `id`가 발급된다 (멱등성은 명시 범위 밖).
- 통합 테스트(E2E)가 실제 Postgres(Testcontainers)에서 통과한다.
- V1 Flyway 마이그레이션이 부팅 시 깔끔하게 적용된다.

## Verification

- **Tests to run**:
  - `./gradlew test --tests "com.example.board.post.*"`
  - 전체 회귀: `./gradlew test`
- **Logs to check**:
  - Flyway 적용 로그: `Migrating schema "public" to version "1 - create posts"` 등
  - Hibernate INSERT 로그가 7개 컬럼(`author, title, content, created_at, updated_at, view_count`)으로 실행되는지 (`format_sql: true` 가독성)
  - 에러 응답에 stack trace나 내부 경로 노출 없는지
- **API/UI to check**:
  ```bash
  # Success
  curl -i -X POST http://localhost:8080/posts \
       -H "Content-Type: application/json" \
       -d '{"author":"alice","title":"Hello","content":"World"}'
  # 기대: 201 Created
  #       Location: /posts/{id}
  #       body: {"id":1,"author":"alice","title":"Hello","content":"World",
  #              "createdAt":"2026-...","updatedAt":"2026-...","viewCount":0}

  # Validation failure
  curl -i -X POST http://localhost:8080/posts \
       -H "Content-Type: application/json" \
       -d '{"author":"alice","content":"World"}'
  # 기대: 400 Bad Request
  ```

## Forbidden

- Lombok 도입 금지
- `application.yml`의 `ddl-auto` 변경 금지 (`validate` 유지)
- **스키마 마이그레이션 없이 엔티티 추가 금지** — V1 Flyway 마이그레이션 필수
- 컨트롤러에 비즈니스 로직 작성 금지 (서비스로 위임)
- 필드 주입(`@Autowired private ...`) 사용 금지
- 테스트 없이 프로덕션 코드 제출 금지 — Red → Green 순서 엄수
- 인증/권한 기능 도입 금지 (범위 외)
- 페이지네이션·검색·정렬·글 수정·글 삭제 추가 금지 (MVP 범위 외)
- **Spring REST Docs 스니펫 작성 금지** — 별도 작업으로 분리 (Q6)
- `viewCount` 증가 로직 구현 금지 — `GET /posts/{id}` 등 별도 작업
- `BoardApplication` 시그니처 변경 금지 (`Clock @Bean` 추가만 허용)
- V1 SQL에 PK 외 인덱스 추가 금지 (`created_at` 등 추가 인덱스는 필요 시 V2로 분리)

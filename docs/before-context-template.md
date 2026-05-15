# Before Context Template — `POST /posts` 작업 계획

> 이 문서는 **`.agent-os/context-template.md`가 존재하지 않는 상태**에서, `AGENTS.md`만 읽힌 AI가 사용자의 한 줄 요청("POST /posts 작성 계획 잡아줘")을 받고 자력으로 산출한 계획서입니다.
> 이후 1-2에서 컨텍스트 템플릿을 정한 뒤, 같은 작업을 템플릿으로 입력했을 때의 계획서(After)와 비교하기 위한 **기준선(baseline)** 입니다.
> 따라서 이 문서를 읽을 때는 "내용이 옳은가"보다 **"AI가 정보 부족을 어떻게 드러내는가, 어떤 가정을 깔고 어떤 질문을 남겼는가"** 를 봐주세요.

---

## 0. 메타

- 작성 시점: Spring Boot init만 완료된 상태 (`BoardApplication`, `application.yml`, Testcontainers 스모크 테스트 통과)
- 입력: 사용자 발화 한 줄 — "POST /posts 작업 계획을 잡아달라"
- 참조: 프로젝트 루트 `AGENTS.md` (영문 정본)
- 산출 형식: AGENTS.md "Workflow" 1~4단계 (이해 → 점검 → 패턴 요약 → 계획 제안). 5단계(Red) 이후는 사용자 승인 전이라 미진행.

---

## 1. 작업 이해 (Restatement)

요청 본문으로 게시글 하나를 받아 영속화하고 생성된 자원을 반환한다. 인증은 범위 밖이므로 작성자 식별은 요청 본문의 이름 문자열로 한정한다. MVP의 두 핵심 기능(목록 조회 / 글 작성) 중 하나.

## 2. 점검한 파일

| 파일 | 확인한 사실 |
|---|---|
| `build.gradle.kts` | `spring-boot-starter-webmvc`, `data-jpa`, `validation`, `restdocs-mockmvc`, Testcontainers 모두 포함 → **새 의존성 추가 불필요** (단, Q4 결과에 따라 Flyway는 별도 추가 가능) |
| `BoardApplication.java` | `@SpringBootApplication` 단일 모듈 |
| `application.yml` | `jpa.hibernate.ddl-auto: validate` → 스키마 관리 책임이 외부에 있음. 현재 마이그레이션 도구 없음 (Q4) |
| `src/test/.../support/TestcontainersConfiguration.java` | `@Import`로 재사용 가능한 `@ServiceConnection` Postgres |

## 3. 기존 패턴 요약

**없음.** 이번이 첫 도메인 기능. 이 작업이 향후 모든 도메인 기능의 기준 패턴이 됨 → 컨벤션 결정에 추가 가중치.

## 4. Open Questions ("Ask, don't guess" — AGENTS.md 원칙)

| # | 질문 | 후보 / 잠정안 |
|---|---|---|
| Q1 | Post 필드 구성은? | `id, author, title, content, createdAt` 5개로 시작. `updatedAt`은 수정 기능 추가 시. |
| Q2 | `id` 전략? | Postgres `BIGSERIAL` + JPA `Long` (가장 단순) vs UUID |
| Q3 | 검증 규칙? | 임시 제안: `author` 1–50, `title` 1–100, `content` 1–10,000자. 실제 값 확정 필요 |
| Q4 | 스키마 관리? | (A) Flyway 도입 + `V1__create_posts.sql` (권장, `ddl-auto: validate` 유지) / (B) `ddl-auto: update` 임시 / (C) Flyway 도입을 선행 작업으로 분리 |
| Q5 | 성공 응답? | `201 Created` + `Location: /posts/{id}` 헤더 + body로 생성된 자원 전체 반환 |
| Q6 | REST Docs 스니펫? | 이번 작업에 포함(TDD 사이클 안에서 자연스러움) vs 별도 작업 |

> Open Question이 6개 남은 이유 자체가 이 문서의 핵심 관찰점입니다. 컨텍스트 템플릿이 있었다면 사용자가 작업 요청을 던지기 전에 이 정보의 상당수를 미리 채울 수 있었을 것입니다.

## 5. 잠정 계획

전제: Q1=OK / Q2=Long+BIGSERIAL / Q3=제안값 / Q4=A(Flyway 도입) / Q5=OK / Q6=포함

### TDD 순서

**① Red — 실패하는 테스트 먼저**

- `src/test/java/com/example/board/post/PostControllerE2ETest.java`
  - `@SpringBootTest(webEnvironment=RANDOM_PORT) @AutoConfigureMockMvc @AutoConfigureRestDocs @Import(TestcontainersConfiguration.class)`
  - `valid_request_returns_201_with_location_and_body()`
  - `missing_title_returns_400()` — Bean Validation 검증
  - `whitespace_only_title_returns_400()` — `@NotBlank` 트림 동작
  - `oversized_content_returns_400()` — `@Size(max=10000)` 경계
- `src/test/java/com/example/board/post/PostServiceTest.java` (단위)
  - `create_persists_post_with_server_side_timestamp()` — `Clock`을 주입해 결정적 시간 검증
  - Mockito로 `PostRepository`만 모킹

**② Green — 최소 구현**

- `Post.java` — `@Entity` 클래스 (record 아님; JPA 제약). 필드: `Long id`, `String author`, `String title`, `String content`, `Instant createdAt`
- `PostRepository.java` — `extends JpaRepository<Post, Long>`
- `PostCreateRequest.java` — **record** + `@NotBlank @Size` 어노테이션
- `PostResponse.java` — **record**
- `PostService.java` — 생성자 주입, `Clock` 주입(테스트 가능성)
- `PostController.java` — `@PostMapping("/posts")` → `ResponseEntity<PostResponse>` with `Location`
- `BoardApplication`에 `Clock` `@Bean` 추가 (기본 `Clock.systemUTC()`)
- Flyway 의존성 + `src/main/resources/db/migration/V1__create_posts.sql`

**③ Refactor — 그린 상태에서**

- 매핑 로직(`Post` ↔ `PostResponse`)을 record 정적 팩토리로 추출
- REST Docs 스니펫(요청 필드, 응답 필드, 응답 헤더 설명) 작성

### 신규 파일 트리

```
src/main/java/com/example/board/post/
├── Post.java
├── PostRepository.java
├── PostService.java
├── PostController.java
├── PostCreateRequest.java
└── PostResponse.java
src/main/resources/db/migration/
└── V1__create_posts.sql
src/test/java/com/example/board/post/
├── PostControllerE2ETest.java
└── PostServiceTest.java
```

### 리스크 / 실패 모드 (AGENTS.md 워크플로 9단계)

- `@NotBlank` + 공백 입력: trim 후 빈 문자열도 거름 — 테스트로 명시
- 타임존: `Instant` + Postgres `timestamptz` — 클라이언트 타임존 독립
- 큰 페이로드: `@Size(max=10000)`이 1차 방어. 더 상위 계층 한계(`spring.codec.max-in-memory-size` 등)는 별도 결정
- 동시 INSERT: `BIGSERIAL`이 단조 증가 보장 — 충돌 없음
- 마이그레이션 누락: `ddl-auto: validate` → V1 누락 시 부팅 실패 (빠른 피드백)
- XSS: API는 JSON만 → 응답 escape 불요. 렌더링 계층 도입 시 재검토

### 범위 외 (Out of Scope)

- 글 수정·삭제, 페이지네이션, 검색, 정렬, 사용자 인증, rate limiting

### 예상 diff 크기

- 프로덕션 ~200 LOC / 테스트 ~150 LOC / SQL 1개 / 의존성 2줄(Flyway)

---

## 6. 이 baseline에서 관찰할 것 (1-2 작성용 입력)

다음 컨텍스트 템플릿(`.agent-os/context-template.md`)을 설계할 때, 이 문서에서 **AI가 추측 대신 질문으로 남긴 항목**들을 항목화하면 템플릿 필드의 후보가 됩니다.

- Q1, Q3 → "도메인 필드 정의 / 검증 규칙" 슬롯
- Q2 → "ID/키 전략" 슬롯
- Q4 → "스키마·마이그레이션 관리 방침" 슬롯
- Q5 → "API 응답 계약 (상태 코드, 헤더, 바디)" 슬롯
- Q6 → "산출물 범위 (테스트·문서까지 포함 여부)" 슬롯

또한 이 문서에서 **AI가 스스로 점검한 사실들** (build.gradle 의존성, `ddl-auto` 값, Testcontainers 설정의 존재)도 템플릿이 사전에 명시한다면 "점검 단계"가 짧아집니다.

이 두 축 — **(a) 사용자가 미리 채워줄 필드** + **(b) AI가 자동으로 확인해야 할 사실** — 이 다음 템플릿의 두 섹션이 될 가능성이 높습니다.

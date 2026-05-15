# Task Context Review — `POST /posts`

- **Target Task Context**: `.agent-os/tasks/2026-05-15-post-create.md`
- **Reviewed**: 2026-05-16
- **Procedure**: 실습 spec 4·5단계 — AI가 `AGENTS.md` + 작성된 Task Context를 다시 읽고 요약 + 질문 → 사람이 교정 → 흔적 기록

---

## 1. AI Summary (사전 이해)

### Task goal
요청 본문(`author`, `title`, `content`)으로 새 게시글을 PostgreSQL에 INSERT하고, 생성된 자원(7개 필드 전체)을 `201 Created` + `Location` 헤더와 함께 반환한다. MVP 게시판의 첫 도메인 기능이며 향후 모든 도메인 기능의 기준 패턴이 된다.

### Change scope
- **신규**: `com.example.board.post` 패키지(엔티티/리포지터리/서비스/컨트롤러/요청·응답 record), 매칭되는 test 패키지, `V1__create_posts.sql`
- **수정 (최소)**: `build.gradle.kts`에 Flyway 의존성 2줄, `BoardApplication.java`에 `Clock @Bean` 한 줄
- **금지**: `application.yml`, init 테스트, Testcontainers 설정, wrapper 일체

### Acceptance criteria (핵심)
- Success: HTTP `201` + `Location: /posts/{id}` + body 7필드 `{id, author, title, content, createdAt, updatedAt, viewCount}`
- 생성 시점에 `createdAt == updatedAt`, `viewCount == 0`
- Validation: `author ≤ 30`, `title ≤ 80`, `content ≤ 5000`, `@NotBlank` (공백만도 차단) → 위반 시 `400`
- Flyway V1 적용된 상태에서 Testcontainers Postgres E2E 통과

### Risks watched (AGENTS.md workflow 9단계)
- `@NotBlank` + 공백 입력 trim 후 거름 — 명시적 테스트 추가
- 타임존: `Instant` + Postgres `timestamptz` — 클라이언트 타임존 독립
- 동시 INSERT: `BIGSERIAL` 단조 증가 → 충돌 없음
- 마이그레이션 누락: `ddl-auto: validate` → V1 부재 시 부팅 실패가 자연스러운 가드
- 컬럼명: Hibernate 기본 `SpringPhysicalNamingStrategy` → `created_at, updated_at, view_count` 자동 매핑 (`application.yml`에 별도 명명 전략 없음 확인됨)

### Interpretive points (Task Context에 명시되지 않아 AI가 채워 넣은 해석)
| # | AI 해석 |
|---|---|
| I1 | `author`는 자유 입력 닉네임. 중복 허용. 외부 사용자 테이블·세션과 무관 |
| I2 | `viewCount`는 향후 `GET /posts/{id}` 같은 상세 조회에서 증가 |
| I3 | 생성 시 `updatedAt = createdAt` — 서비스에서 `clock.instant()` 1회 호출해 둘 다 할당 |
| I4 | `viewCount` 타입은 `long` 프리미티브 (non-null 보장) |
| I5 | `Clock @Bean`은 `BoardApplication` 클래스 안에 직접 추가 |
| I6 | 400 응답 body는 Spring Boot 4 기본 `ProblemDetail` |
| I7 | `@Valid`는 컨트롤러 메서드 파라미터에만 |
| I8 | `Instant` 직렬화는 Jackson 기본 ISO-8601 문자열 |

### Outstanding questions (Task Context에서도 결정되지 않은 사항)
- **Q-A**: 이미 추가된 `restdocs-mockmvc` 의존성 처리
- **Q-B**: 400 응답에 위반 필드명 노출 정책
- **Q-C**: V1 SQL의 인덱스 정책

---

## 2. Human Review (2026-05-16)

### I1~I8 — 해석 검증
**모두 OK. 교정 사항 없음.**

→ AGENTS.md + 채워진 Task Context의 조합으로 AI 해석 정확도가 100%였다는 기록. 컨벤션을 사전에 문서화한 효과를 확인.

### Q-A/B/C — 권장안으로 확정

| 항목 | 결정 | 적용 위치 |
|---|---|---|
| Q-A | `restdocs-mockmvc` 의존성 **그대로 유지** | 변경 없음. Forbidden의 "REST Docs 스니펫 작성 금지"는 이미 의존성 제거가 아니라 사용 금지를 의미. |
| Q-B | 400 응답은 Spring Boot 4 기본 `ProblemDetail` 형식, **위반 필드명 포함** | Task Context `Acceptance Criteria → 검증 실패 응답`에 반영 (2026-05-16 추가) |
| Q-C | V1 SQL 인덱스는 **PK만**. 추가 인덱스는 V2로 분리 | Task Context `Forbidden`에 반영 (2026-05-16 추가) |

---

## 3. Trace of changes applied to Task Context

| 시점 | 위치 | 변경 |
|---|---|---|
| 2026-05-15 | Acceptance Criteria, Forbidden, Decisions Log | Q1~Q6 결정 1차 반영 |
| 2026-05-16 | Task Context 본문에서 메타 주석·Decisions Log 제거 → 템플릿 9개 섹션 구조로 정리 | 구조 정합성 확보 |
| 2026-05-16 | Acceptance Criteria → 검증 실패 응답 | Q-B 결과 추가: "응답 body는 `ProblemDetail` 형식이며 위반 필드명을 포함" |
| 2026-05-16 | Forbidden | Q-C 결과 추가: "V1 SQL에 PK 외 인덱스 추가 금지" |

---

## 4. Learning notes

- **컨벤션을 AGENTS.md에 미리 박아두니** AI가 채워 넣은 해석(I1~I8)에서 8/8 모두 의도와 일치. 만약 컨벤션이 없었다면 ID 전략·DTO 형식·시간 처리 등에서 추측 오차가 생겼을 가능성이 높음.
- **그럼에도 Q-A/B/C 세 개가 추가로 드러남**. 이는 컨텍스트 템플릿이 완전하지 않다는 신호.
  - 차후 템플릿 개선 후보: "응답 body 형식 (성공/실패)", "스키마 인덱스 정책", "도입은 했으나 이 작업에서 쓰지 않을 라이브러리의 처리 방침"
- **메타 주석·Decisions Log 같은 정보**는 Task Context 본문이 아니라 별도 review 파일(이 파일)에 두는 편이 깔끔. 본문은 항상 "현재의 단일 진실"로 유지.
- **AGENTS.md "Ask, don't guess"** 원칙이 실제로 작동: AI가 추측 대신 8개 해석점·3개 질문을 명시적으로 surface → 교정·확정 비용 ≪ 잘못 구현 후 되돌리는 비용.

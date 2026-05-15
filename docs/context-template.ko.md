# Task Context 템플릿 (한글 레퍼런스)

> 이 문서는 `.agent-os/context-template.md`의 한글 해설본입니다.
> **실제로 AI가 읽는 정본은 `.agent-os/context-template.md`** 이며, 두 문서가 충돌할 경우 정본을 따릅니다.
> 이 문서는 (1) 템플릿 각 섹션이 왜 존재하는지, (2) 어떻게 채우는 것이 좋은지에 대한 사람용 가이드입니다.

---

## 이 템플릿은 언제 쓰는가

단일 작업(엔드포인트 1개 추가, 버그 수정 1건 등) 단위로 채워서 AI에게 전달합니다.
AI는 **`AGENTS.md` + 채워진 Task Context** 두 가지를 함께 읽고, 구현 전에 먼저:

1. 작업 목표·변경 범위·리스크·완료 기준을 **요약**
2. 부족한 정보에 대해 **질문** (`AGENTS.md`의 "Ask, don't guess" 원칙)

사람은 이 요약과 질문을 받고 오해를 교정한 뒤에야 구현(TDD Red → Green → Refactor)을 허용합니다.

---

## 사용 절차

1. `.agent-os/context-template.md`를 복사해 작업별 파일을 만듭니다.
   - 예: `.agent-os/tasks/2026-05-15-post-create.md`
2. 모든 섹션을 채웁니다. 빈 칸을 남기지 말고, 해당 사항이 없으면 `N/A` 라고 명시합니다.
   - 빈 칸은 "해당 없음"이 아니라 "AI가 추측한다"는 뜻이 되기 쉽습니다.
3. AI에게 다음을 보냅니다:

   ```text
   Read AGENTS.md and this Task Context.
   Summarize the task goal, change scope, risks, and acceptance criteria.
   Do not implement yet. Ask questions if context is missing.
   ```

4. AI의 요약과 질문을 받습니다. 잘못 이해한 부분을 교정합니다.
5. 교정 흔적을 기록합니다 (예: Task Context 파일 하단에 "Corrections" 섹션).
6. 그 다음에야 구현으로 들어갑니다.

---

## 섹션별 가이드

### Goal — 목표
- 한두 문장. **사용자/시스템 관점**에서 무엇이 달성되는지.
- ✅ 좋은 예: "POST /posts 로 게시글 하나를 받아 영속화하고 생성된 자원을 반환한다."
- ❌ 나쁜 예: "PostController 를 만든다." — 구현 수단이지 목표가 아닙니다.

### Background — 배경
- 왜 **지금** 필요한가? 어떤 의사결정·요청·이전 작업에서 비롯되었나?
- AI가 트레이드오프(단순 vs 확장성, 보수 vs 적극)를 판단할 때 이 정보에 의존합니다.

### Related Files / Areas — 관련 파일/영역
- AI가 점검 단계에서 **첫 번째로 읽을 파일들**.
- 분류해서 적으면 좋습니다:
  - 변경 후보 (will touch)
  - 참고용 기존 패턴 (reference only)
  - 관련 도메인 영역
- 경로는 `src/` 기준 또는 절대 경로.

### Existing Patterns — 기존 패턴
- 이 작업에 적용되는 이 프로젝트의 컨벤션을 명시.
- 이 프로젝트의 기본 컨벤션 (모두 `AGENTS.md`에 명문화됨):
  - 생성자 주입, 필드 주입 금지
  - DTO는 Java 21 `record`
  - JPA 엔티티는 클래스
  - **Lombok 미사용**
  - 컨트롤러는 얇게, 비즈니스 규칙은 서비스에
  - 통합/E2E는 Testcontainers + `@ServiceConnection`
  - API 문서는 Spring REST Docs 스니펫
- 이 작업만의 추가 패턴이 있다면 함께 기재.

### Change Scope — 변경 범위
- **"Do not change" 가 더 중요할 때가 많습니다.** AI의 무관한 cleanup 충동을 차단합니다.
- 예:
  - Can change: `src/main/java/com/example/board/post/**`, `src/test/java/com/example/board/post/**`
  - Do not change: `application.yml`의 `ddl-auto` 값, `BoardApplication.java`

### Constraints — 제약
- 5축 모두 채우되, 해당 없으면 `N/A`.
- 각 축의 의미:
  - **Security**: 인증·인가·민감 정보·XSS·SQLi 등.
  - **Performance**: 응답 시간, 처리량, 메모리, 인덱스.
  - **Operations**: 로깅·모니터링·롤백·마이그레이션.
  - **Compatibility**: 기존 API 계약, DB 스키마, 클라이언트.
  - **Time**: 마감, 우선순위.

### Acceptance Criteria — 완료 조건
- **측정 가능**, **검증 가능**, **그대로 테스트 케이스로 옮길 수 있는** 형태로.
- ✅ 좋은 예: "valid body 요청 시 `201 Created` + `Location: /posts/{id}` 헤더 + 본문에 생성된 자원 전체 반환"
- ❌ 나쁜 예: "잘 동작해야 한다."

### Verification — 검증 방법
- 누가/무엇이 통과를 판정하는가. **자동 검증을 우선**합니다.
- 예:
  - Tests to run: `./gradlew test --tests "com.example.board.post.*"`
  - Logs to check: 애플리케이션 로그에 SQL이 의도대로 찍히는지 (`format_sql: true`)
  - API/UI to check: `curl -X POST localhost:8080/posts ...`

### Forbidden — 금지 사항
- AI가 흔히 빠지는 함정 + 이 작업 한정 금지.
- 예:
  - "Lombok 도입 금지"
  - "스키마 마이그레이션 없이 엔티티 추가 금지"
  - "기존 패턴과 다른 어노테이션 사용 금지"
  - "테스트 없이 구현 제출 금지"

---

## 자주 묻는 질문

**Q. 매번 다 채워야 하나요? 너무 번거롭습니다.**
A. 작은 변경(타이포 수정 등)에는 부담스러울 수 있습니다. 단, "**작은 변경이라고 생각했는데 사실 컨텍스트가 모자랐던 경우**"가 AI 사고의 가장 흔한 원인입니다. 의심스러우면 채워주세요. 빈 채로 두는 것보다 `N/A`라고 적는 것이 항상 낫습니다.

**Q. 두 사람이 같은 작업을 다르게 채우면요?**
A. 정답은 없습니다. 다만 **Acceptance Criteria 와 Forbidden** 두 섹션만이라도 일관되게 채우면 AI 결과의 변동성이 크게 줄어듭니다.

**Q. AI가 요약 단계에서 엉뚱한 질문을 하면요?**
A. 그 질문 자체가 **컨텍스트가 부족하다는 증거**입니다. 해당 섹션을 보강한 뒤 다시 요약을 요청합니다.

# 에이전트 지침 (AGENTS.md 한글 레퍼런스)

> 이 문서는 프로젝트 루트의 `AGENTS.md`를 한글로 옮긴 사람용 가이드입니다.
> **실제로 AI(Claude Code)가 자동 로드하는 정본은 `CLAUDE.md` → `AGENTS.md` import 체인**입니다. 두 문서가 충돌하면 영문 정본을 따릅니다.

## 역할

이 프로젝트는 TDD 연습입니다. **실패하는 테스트 먼저 → 최소 구현 → 그린 상태에서 리팩터링** 순서.

## 프로젝트

- **목적**: MVP 게시판. 범위 — `GET /posts` (페이지네이션), `POST /posts`. 사용자 인증은 범위 밖.
- **스택**: Java 21, Spring Boot 4.0.6, Spring Data JPA, PostgreSQL, Gradle Kotlin DSL, JUnit 5, Mockito, AssertJ, Testcontainers, Spring REST Docs
- **빌드·테스트**: `./gradlew test` (Testcontainers는 Docker 실행 필요)
- **진입점**: `src/main/java/com/example/board/BoardApplication.java`
- **도메인 코드**: `src/main/java/com/example/board/post/`
- **설정**: `src/main/resources/application.yml`
- **마이그레이션**: Flyway, `src/main/resources/db/migration/V*.sql`

## 컨벤션·규칙 (자동 로드)

Claude Code가 `.claude/rules/`에서 자동으로 로드합니다 — 매 세션마다.

- `.claude/rules/conventions.md` — 코드 컨벤션 (record DTO, 생성자 주입, ErrorResponse 계약, Spring 6.1+ MVC 검증, PageResponse, 테스트 셋업, Jackson 3.x 등)
- `.claude/rules/hard-rules.md` — 금지 사항 (시크릿/PII, 스키마 정책, 의존성 승인, 파일 잠금, MVP 범위 외 기능)

다른 코딩 에이전트(Codex 등)는 `AGENTS.md`를 읽으면서 위 두 rule 파일도 함께 참고하면 됩니다.

## 작업 워크플로

비자명한 작업은 컨텍스트 파일을 거칩니다:

1. `.agent-os/context-template.md`를 복사해 `.agent-os/tasks/YYYY-MM-DD-<slug>.md`로 작성
2. AI는 `AGENTS.md` + 작성된 Task Context를 읽고 목표·범위·리스크·완료 기준을 요약하며 부족한 부분을 질문
3. 사람은 확인·교정 → 흔적은 `.agent-os/tasks/<slug>.review.md`에 기록
4. 그 후 TDD로 구현

## 확장 가이드 (필요할 때만 명시 로드)

자동 로드되지 않습니다 — 위험한 작업·워크플로 점검·신규 합류자 시에만 명시적으로 읽혀주세요.

- `docs/agents/principles.md` — 작업 원칙, AI/사람 역할, 도구 제약, 보안 세부
- `docs/agents/workflow.md` — 9단계 작업 워크플로 + 리뷰 체크리스트

## 정보 계층 요약

| 파일 | 자동 로드? | 사용처 |
|---|---|---|
| `CLAUDE.md` | YES (Claude Code) | `@AGENTS.md` import 한 줄 |
| `AGENTS.md` | YES (import 경유) | 프로젝트 사실 + 다른 영역 가이드 |
| `.claude/rules/*.md` | YES (Claude Code) | 컨벤션·금지 사항 |
| `.claude/settings.local.json` | YES (강제력) | 권한 deny — 파일 잠금 강제 |
| `docs/agents/*.md` | NO | 필요 시 명시 호출 |
| `.agent-os/tasks/*.md` | NO | 작업별 컨텍스트 |

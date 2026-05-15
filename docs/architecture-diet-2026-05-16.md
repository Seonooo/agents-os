# Architecture Diet — Before / After (2026-05-16)

이 문서는 Week 1 회고에서 발견한 문제를 토대로 단행한 **에이전트 가이드 아키텍처 전면 재구성**의 영구 기록입니다.

## 1. Executive summary

| 측면 | Before | After |
|---|---|---|
| 핵심 문제 | `AGENTS.md`가 **Claude Code가 자동 로드하지 않는 파일**이라 사실상 사람용 문서였음 | `CLAUDE.md` → `@AGENTS.md` 체인으로 **실제 자동 로드** |
| 단일 파일 비대 | AGENTS.md 104줄에 일반론·워크플로·체크리스트 다 들어 있음 | 9개 섹션 → 6개 파일로 분리 (항상 로드 / on-demand) |
| 강제력 | 텍스트 정책뿐 (강제력 0) | `.claude/settings.local.json` `permissions.deny`로 파일 잠금 강제 |
| Skill·rules 미사용 | `.claude/rules/` 미존재 | `conventions.md` + `hard-rules.md` (Claude Code 공식 메커니즘) |
| 외부 도구 호환 | AGENTS.md만 있음 | Codex 등은 AGENTS.md 직접 / Claude Code는 CLAUDE.md 경유 — 양쪽 호환 |

## 2. 정량 측정

### 2-1. 파일 크기 비교

| 분류 | 파일 | Before (줄/자) | After (줄/자) | 비고 |
|---|---|---|---|---|
| 정본 | `AGENTS.md` | 104 / 7,507 | **38 / 1,855** | −63% lines, −75% chars |
| 신규 진입점 | `CLAUDE.md` | — | 1 / 11 | `@AGENTS.md` import |
| 신규 자동 로드 | `.claude/rules/conventions.md` | — | 55 / 2,881 | 컨벤션 분리 |
| 신규 자동 로드 | `.claude/rules/hard-rules.md` | — | 40 / 1,782 | 금지 사항 분리 |
| 신규 on-demand | `docs/agents/principles.md` | — | 51 / 4,353 | 옛 Working Principles + Tooling + Security 보존 |
| 신규 on-demand | `docs/agents/workflow.md` | — | 38 / 2,715 | 옛 Workflow 9단계 + Review Checklist 보존 |
| 한글 레퍼런스 | `docs/AGENTS.ko.md` | 108 / 9,168 | 54 / 3,005 | 새 슬림본 미러 |

### 2-2. "AI에 매 세션 자동 주입되는 총 토큰"

| 항목 | Before | After |
|---|---|---|
| 자동 로드 파일 | 사실상 0 ⚠️ (AGENTS.md는 Claude Code 자동 로드 대상 아님 — 매 작업 시 수동 Read로 끌어옴) | CLAUDE.md (11자) + AGENTS.md (1,855자) + rules/conventions.md (2,881자) + rules/hard-rules.md (1,782자) = **6,529자** |
| 추정 토큰 (≈ 3.8 chars/token) | ~0 자동 / 매 작업 수동 ~1,975 | **~1,718 자동** |

> Before가 0이었다는 사실 자체가 가장 큰 발견.
> "AGENTS.md 다이어트" 라는 표면적 목표보다, **자동 로드 체계 구축**이 진짜 가치였습니다.

### 2-3. On-demand 분리 효과

| 분류 | 합계 |
|---|---|
| Before에서 AGENTS.md 안에 묶여 있던 일반론 (Working Principles, Workflow, Review Checklist 등) | 약 75줄 / 5,500자 |
| After에서 `docs/agents/` 로 옮긴 양 | 89줄 / 7,068자 (보존·재구성) |
| 매 세션 자동 주입에서 제거된 효과 | 약 5,500자 → 0 (필요 시만 명시 호출) |

## 3. 구조적 diff

### 3-1. Before (단일 파일 + 한글 사본)

```
board/
├── AGENTS.md                   ← 104줄, 자동 로드 안 됨
└── docs/
    ├── AGENTS.ko.md            ← 108줄, 사람용 사본
    └── context-template.ko.md
```

### 3-2. After (3계층 분리)

```
board/
├── CLAUDE.md                              [Tier 1: Claude Code 자동 로드 진입점]
├── AGENTS.md                              [Tier 1: cross-tool 정본, 38줄]
├── .claude/
│   ├── settings.local.json                [Tier 1.5: permissions.deny 강제]
│   └── rules/                             [Tier 1: Claude Code rules 메커니즘]
│       ├── conventions.md                 (55줄)
│       └── hard-rules.md                  (40줄)
├── docs/
│   ├── AGENTS.ko.md                       [사람용 한글 레퍼런스 (AI 미로딩)]
│   └── agents/                            [Tier 2: on-demand]
│       ├── principles.md                  (51줄)
│       └── workflow.md                    (38줄)
├── .agent-os/
│   ├── context-template.md                [작업별 컨텍스트 템플릿]
│   └── tasks/...                          [작업별 인스턴스 + review]
└── (src/, gradle/, ...)
```

### 3-3. 어떤 내용이 어디로 갔나

| Before AGENTS.md 섹션 | After 위치 | 자동 로드? |
|---|---|---|
| Role | AGENTS.md (1줄로 압축) | YES |
| Project Context | AGENTS.md (그대로) | YES |
| **Working Principles** (10+ 줄) | `docs/agents/principles.md` | **NO** |
| Security and Privacy | `docs/agents/principles.md` 안 통합 + `hard-rules.md` 발췌 | NO (전체) / YES (요약) |
| **Workflow 9단계** | `docs/agents/workflow.md` | **NO** |
| What AI Can Do / Human Must Decide | `docs/agents/principles.md` | NO |
| **Tooling Constraints** (세션·컴팩션 등) | `docs/agents/principles.md` | NO |
| **Review Checklist** | `docs/agents/workflow.md` | NO |
| (없음 — 새로 정리) | `.claude/rules/conventions.md` | YES |
| (없음 — 새로 정리) | `.claude/rules/hard-rules.md` | YES |
| (없음 — 새로 정리) | `.claude/settings.local.json` `permissions.deny` | YES (강제) |

## 4. Notion 진단 vs After

| Notion 진단 (회고에서 인용) | Before 상태 | After 상태 |
|---|---|---|
| AGENTS.md 50줄 안쪽 | ❌ 104줄 | ✅ 38줄 |
| 일반론·교과서적 윤리 빼고 프로젝트 사실만 | ❌ Working Principles 11줄 | ✅ on-demand로 분리 |
| 매 작업마다 채우는 풀 컨텍스트 템플릿은 무겁다 | ❌ context-template.md 풀 9섹션만 존재 | ⚠️ 변경 없음 — 이후 분리 검토 (action item) |
| Skill 분리 의식 | ❌ 후보 메모 0개 | ⚠️ 아직 안 함 — 후보 메모 시작 (action item) |
| 권한·금지는 settings.json `permissions.deny` | ❌ AGENTS.md 텍스트뿐 | ✅ 파일 잠금 강제 |
| CLAUDE.md는 import 또는 symlink | ❌ CLAUDE.md 없음 | ✅ `@AGENTS.md` 한 줄 |
| 비대해지면 위에서 아래로 옮긴다 | — | ✅ 실제 수행 (Tier 1→2) |

## 5. 최신 자료 리서치 (2026-05 기준)

### 5-1. 산업 컨센서스 (검증 결과 우리 아키텍처와 일치)

- **CLAUDE.md 200줄 룰**: "200줄 이상이면 Claude가 절반을 무시한다. 가차없이 가지치기하라" (Anthropic 공식 권고 + 커뮤니티 베스트 프랙티스 일치). 우리는 정본(AGENTS.md+rules) 모두 200줄 미만.
- **Path-scoped rules는 `.claude/rules/*.md` + YAML `paths` frontmatter**: 공식 메커니즘 확정. 우리는 path-scoping 아직 사용 안 함 — 모두 항상 로드. 차후 도메인이 늘면 `paths: ["src/**/post/**"]` 패턴 도입 검토.
- **AGENTS.md ↔ CLAUDE.md 호환**: `@AGENTS.md` import 또는 `ln -s AGENTS.md CLAUDE.md` (Windows에선 import 권장 — Administrator 권한 필요). 우리는 `@AGENTS.md` import 채택.

### 5-2. Skill 시스템 (2025 말 표준화, 2026 본격 확산)

- **Progressive Disclosure**: Skill은 폴더 단위. `name` + `description`만 시작 시 로드 → 매치 시 `SKILL.md` 본문 로드 → 필요 시 번들 파일 로드. **항상 로드되는 rules보다 토큰 효율 우수**.
- **활용 권고**: 두 번째로 같은 프롬프트가 반복되면 Skill 후보. **우리의 TDD 4-round 패턴(POST, GET, error-contract에서 3회 반복)이 명백한 Skill 후보**.
- **권장 사이즈**: `SKILL.md` 본문 500줄 미만. 우리 워크플로는 훨씬 짧게 가능.

### 5-3. Context engineering 핵심 개념 (Anthropic 공식 용어와 우리 회고 정렬)

- **Context contamination/poisoning**: 초기 모델 응답의 부정확한 정보가 컨텍스트에 남아 이후 출력을 오염. 우리도 Week 1 회고에서 동일 용어로 식별.
- **Context pollution**: 무관·중복 정보로 인한 추론 정확도 저하. AGENTS.md 비대화의 본질이 이것.
- **Tradeoff 수치 (산업 평균)**: 컨텍스트 최적화 시 운영비 60–80% 절감, task 완료율 15–30% 개선, context-related 실패 50–70% 감소.
- **Context window 한도**: 1M 토큰 모델이 등장했지만, **약 1M 부근에서 명백한 성능 하한**이 관측됨. "토큰을 많이 채울수록 좋다"는 거짓.

### 5-4. Subagent 격리

- 복잡 에이전트는 sub-task 간 context isolation으로 cross-contamination 방지. 위험 작업(스키마 마이그레이션·보안)에는 fresh subagent 권장.
- 우리 프로젝트는 단일 세션 위주 — 위험 작업 진행 시 subagent 격리 패턴 도입 검토.

### 5-5. 영향력 있는 출처

- [Best practices for Claude Code (공식)](https://code.claude.com/docs/en/best-practices)
- [How Claude Code rules actually work](https://joseparreogarcia.substack.com/p/how-claude-code-rules-actually-work)
- [Stop Bloating Your CLAUDE.md: Progressive Disclosure](https://alexop.dev/posts/stop-bloating-your-claude-md-progressive-disclosure-ai-coding-tools/)
- [Equipping agents for the real world with Agent Skills (Anthropic)](https://www.anthropic.com/engineering/equipping-agents-for-the-real-world-with-agent-skills)
- [Agent Skills - Claude API Docs (공식)](https://platform.claude.com/docs/en/agents-and-tools/agent-skills/overview)
- [Effective context engineering for AI agents (Anthropic)](https://www.anthropic.com/engineering/effective-context-engineering-for-ai-agents)
- [CLAUDE.md, AGENTS.md & Copilot Instructions](https://www.deployhq.com/blog/ai-coding-config-files-guide)
- [Skill authoring best practices (공식)](https://platform.claude.com/docs/en/agents-and-tools/agent-skills/best-practices)

## 6. 다음 액션 후보

### 즉시 가치 큰 것
- [ ] **Skill 후보 시작**: `.claude/skills/tdd-endpoint/SKILL.md` 만들기 — POST/GET 작업의 4라운드 TDD 패턴 (`Red 테스트 → Green 최소 구현 → Refactor → 회귀`). 발견: 우리는 이미 3번 반복했음.
- [ ] **`/memory` 검증**: 다음 세션에서 실제로 CLAUDE.md, AGENTS.md, `.claude/rules/*` 4개 모두 자동 로드되는지 확인.
- [ ] **권한 deny 검증**: `gradlew` 편집 시도 → 차단되는지 확인.

### 중장기
- [ ] **context-template 분리**: 4줄 인라인 버전 + 풀 9섹션 버전. 캐주얼 작업에 풀 적용은 무겁다는 회고 지속 적용.
- [ ] **Path-scoped rule 도입**: 도메인이 늘면 `paths: ["src/**/post/**"]` 같은 frontmatter 활용. 지금은 1도메인이라 미적용.
- [ ] **Subagent 격리**: 스키마 변경 같은 위험 작업 시.

## 7. 변경에 대한 가역성 (Reversibility)

이 문서가 commit 되는 시점에 git이 초기화됩니다. 이후의 모든 변경은 git history로 추적 가능. 다이어트 이전 상태로 돌아가고 싶다면:
- `git log` → 첫 커밋 직전이 다이어트 후이므로, 다이어트 전 상태는 별도 archive 필요 시 사용자가 요청.
- 또는 이 문서의 §3-3 표를 보고 수동 복원 가능.

## 8. 한 줄 평

> 다이어트는 **AGENTS.md를 줄이는 일이 아니라 그것이 실제로 AI에 닿는 길을 만드는 일**이었다.
> Before에는 잘 쓴 문서가 AI에 닿지 않았고, After에는 짧은 문서가 자동으로 닿는다.

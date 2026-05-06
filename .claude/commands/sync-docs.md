---
description: 코드 변경분(git diff)을 분석해 PRD/TDD 갱신 필요 섹션을 자동 추출하고 패치를 미리보기로 제안 (자동 Edit·git 작업 금지)
argument-hint: [--last-commit | --branch | --since <commit-sha>]
allowed-tools: Bash, Read, Edit, Grep, Glob
---

사용자가 `/sync-docs $ARGUMENTS` 형태로 호출했다. 코드 변경분을 분석해 `docs/PRD.md` / `docs/TDD.md` 와의 어긋남을 식별하고, 갱신 패치를 **미리보기로만** 제안한다. **자동 Edit·git 작업 금지** — 모든 적용은 사용자 승인 후.

---

## 0. 분석 범위 결정

`$ARGUMENTS` 파싱:

| 인자 | 의미 | git 명령 |
|------|------|---------|
| (빈 인자) | 작업 트리 변경분(uncommitted) + 최근 1 커밋 | `git status --short` + `git diff HEAD~1 HEAD --name-status` |
| `--last-commit` | 최근 1 커밋만 | `git diff HEAD~1 HEAD --name-status` |
| `--branch` | 현재 브랜치 vs `origin/main` | `git diff origin/main...HEAD --name-status` |
| `--since <sha>` | 해당 커밋부터 HEAD까지 | `git diff <sha>..HEAD --name-status` |

---

## 1. 변경 파일 추출 + 카테고리 분류

각 변경 파일을 루트 [`CLAUDE.md`](../../CLAUDE.md)의 "문서 동기화 트리거" 표에 따라 분류:

| 매칭 패턴 | 카테고리 | 갱신 대상 |
|----------|---------|----------|
| `dugout-api/.../domain/*/entity/*.kt` | **DB 스키마** | `docs/TDD.md` § DB 스키마 |
| `dugout-api/.../domain/*/controller/*.kt` | **API 변경** | `docs/TDD.md` § API 설계 |
| `dugout-api/.../domain/*/dto/*.kt` (시그니처 변경) | **API 변경** | `docs/TDD.md` § API 설계 |
| `dugout-api/.../domain/{새이름}/` 신규 디렉토리 | **새 도메인** | `docs/PRD.md` + `docs/TDD.md` 둘 다 |
| `dugout-ai/**/*.py` | **AI 알고리즘** | `docs/TDD.md` § AI 알고리즘 |
| `dugout-ios/.../Features/*/Presentation/Views/*.swift` 신규 | **사용자 시나리오** | `docs/PRD.md` § 사용자 여정 |
| `dugout-ios/.../Core/DesignSystem/Components/DG*.swift` | **디자인 시스템** | iOS 디자인 가이드 (있다면) |

추가 키워드 분류 (라인 단위):
- `Hungarian`, `ELO`, `K=`, `BALANCED`, `COMPETITIVE`, `recommend`, `predict` 등 등장 → **AI 알고리즘** 카테고리 추가

---

## 2. 카테고리별 변경 내용 추출

### 2-1. DB 스키마
변경된 `*Entity.kt` 또는 새 entity 파일을 Read하여 추출:
- 새 컬럼: `@Column(name = "...")` + `val foo: Type`
- 인덱스: `@Table(indexes = ...)`
- 제약: `nullable`, `unique`, `length`
- 관계: `@ManyToOne`, `@OneToMany`, `@JoinColumn`

`docs/TDD.md`의 "DB 스키마" 섹션을 grep으로 찾아 해당 테이블 부분과 대조.

### 2-2. API 변경
변경된 `*Controller.kt` 에서:
- 매핑: `@GetMapping`, `@PostMapping`, `@PatchMapping`, `@DeleteMapping` + path
- 시그니처: 메서드 인자(`@Valid @RequestBody`, `@PathVariable`, `@RequestParam`) + 반환(`ResponseEntity<...>`)
- DTO 변경: 필드 추가/타입 변경

`docs/TDD.md`의 "API 설계" 섹션과 대조.

### 2-3. 새 도메인
`domain/{이름}/` 가 비교 시점에 신규로 등장한 경우:
- 5폴더(controller/service/repository/entity/dto) 확인
- TDD.md, PRD.md에 해당 도메인 섹션 부재 확인
- 추가 제안: DB 스키마 신규 테이블 + API 설계 신규 endpoint 그룹 + PRD 기능 한 줄

### 2-4. AI 알고리즘
키워드 그렙 (`Hungarian`, `ELO`, `K=`, `BALANCED`, `COMPETITIVE`, `recommend`, `predict`).
TDD.md "AI 알고리즘" 섹션과 대조.

### 2-5. 사용자 시나리오 (iOS)
신규 `*View.swift` 파일 + 같은 PR의 ViewModel 시그니처:
- View 이름·접근 경로 추정
- PRD.md "사용자 여정" 섹션의 시나리오 표와 대조 (없으면 추가 제안)

---

## 3. 패치 미리보기 출력 형식

각 갱신 후보를 다음 블록으로 출력:

```
[N] {대상 파일} / {섹션}
    {한 줄 요약}

    --- 현재 ---
    (해당 줄 또는 "(섹션 부재)")

    +++ 제안 +++
    (추가/수정할 텍스트 — 마크다운 표 또는 항목)
```

---

## 4. 사용자 승인 단계 (필수)

모든 미리보기를 보여준 뒤 다음 질문을 출력:

> N개 패치 후보 중 적용할 항목을 알려주세요.
> 예: `1, 3` / `all` / `skip`

응답 받기 전 **절대 Edit하지 않음**. 응답 후에만 Edit으로 적용.

---

## 5. 안전장치

| 조건 | 동작 |
|------|------|
| 변경된 파일 0개 | "동기화 필요 변경 없음" 보고 후 종료 |
| `docs/PRD.md` 또는 `docs/TDD.md` 미존재 | 안내 후 중단 |
| 변경 파일 30개 초과 | "범위가 큽니다. `/sync-docs --last-commit` 등으로 좁혀주세요" |
| 패치 후보 0개 | "갱신 필요 없음 — 코드와 문서가 일치" 보고 |
| 변경 라인이 주석·import만 | 카테고리 분류에서 제외 |

---

## 6. 최종 결과 보고 형식

```markdown
## 📋 /sync-docs 결과

**분석 범위**: <범위>
**변경 파일**: <개수>개

| 카테고리 | 개수 |
|---------|------|
| DB 스키마 | N |
| API 변경 | N |
| 새 도메인 | N |
| AI 알고리즘 | N |
| 사용자 시나리오 | N |
| 디자인 시스템 | N |

---

### 🔍 동기화 필요 항목 N개

[1] {대상 / 섹션}
    {요약}
    --- 현재 ---
    ...
    +++ 제안 +++
    ...

[2] ...

---

승인할 항목을 알려주세요. 예: "1, 3" / "all" / "skip"
```

---

## 7. 절대 금지

- ❌ 자동 Edit (모든 패치는 사용자 승인 후)
- ❌ 자동 git add / commit / push / 머지
- ❌ 사용자에게 묻지 않고 PRD/TDD 임의 수정
- ❌ 변경되지 않은 코드를 추측해서 가짜 섹션 추가
- ❌ 한국어 도메인 용어를 자유 번역 (글로서리 스킬 따를 것)
- ❌ 코드 변경분과 무관한 일반 권고 (예: "이 부분 리팩토링 권장") — `sync-docs`는 동기화 작업만

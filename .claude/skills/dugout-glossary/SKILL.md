---
name: dugout-glossary
description: Use when naming new identifiers (classes, fields, files, packages, DB tables, API paths, enum values) anywhere in the dugout codebase. Enforces the Korean ↔ English domain glossary (Team, Match, Attendance, Lineup, Fee, Matching, Mercenary, Ground, Division, Rating, etc.) and blocks free translation, romanization, or arbitrary abbreviations.
---

# Dugout 도메인 용어 글로서리 (강제 적용)

루트 [`CLAUDE.md`](../../../CLAUDE.md) 의 "핵심 도메인 용어" 표가 단일 진실의 원천이다.
코드 식별자는 **영문 칼럼만 사용한다.** 자유 번역·로마자(`yongbyeong` 등)·임의 줄임말 금지.

---

## 1. 핵심 매핑 (변형 금지)

| 한국어 | 식별자 | 비고 |
|--------|--------|------|
| 팀 | `Team` | 클래스/엔티티/패키지 |
| 경기 | `Match` | 클래스/엔티티/패키지 |
| 출석 | `Attendance` | 클래스/엔티티/패키지 |
| 라인업 | `Lineup` | 타순+포지션 통합 개념 |
| 회비 | `Fee` | 클래스/엔티티/패키지 |
| 매칭 | `Matching` | 팀 간 매칭 (행위) — `Match`와 구분 |
| 용병 | `Mercenary` | 클래스/엔티티/패키지 |
| 구장 | `Ground` | 사회인 기준 (Stadium ❌) |
| 부수 | `Division` | 실력 등급 (1부~4부) |
| 레이팅 | `Rating` | ELO 점수 |
| 사용자 | `User` | 인증 주체 |

## 2. 포지션 코드 (영문 약어만)

`P`, `C`, `1B`, `2B`, `3B`, `SS`, `LF`, `CF`, `RF`, `DH`

- enum raw 값·필드값은 **반드시 약어**
- 한국어/영어 풀네임("투수", "Pitcher") 표시는 **UI/응답 레이어 표시용 매핑** 함수에서만 (`displayName: String`)

---

## 3. 자주 발생하는 자유 번역 위반 → 수정

| ❌ 잘못된 식별자 | ✅ 올바른 식별자 | 이유 |
|----------------|----------------|------|
| `Yongbyeong`, `MercSearch` | `Mercenary`, `MercenarySearch` | 로마자 금지 |
| `MatchUp`, `GameMatching` | `Matching` | 팀 매칭은 항상 `Matching` |
| `Game`, `Schedule` | `Match` | 경기 = `Match` |
| `BattingOrder` | `Lineup` | 라인업은 타순+포지션 통합 |
| `Dues`, `MoneyPayment`, `Membership` | `Fee` | 회비 = `Fee` |
| `Stadium`, `Field` | `Ground` | 구장 = `Ground` |
| `SkillLevel`, `Tier`, `Rank` | `Division` | 부수 = `Division` |
| `Pitcher`, `Catcher` (필드명) | `P`, `C` (enum 값) | 포지션은 표준 약어 |
| `KakaoTalkAlim`, `Alimtalk` | `KakaoAlimtalk` | 카카오 알림톡 공식 영문 표기 |
| `Foreigner`, `Guest` | `Mercenary` | 용병 = `Mercenary` |

---

## 4. 파생 용어 패턴 (글로서리 외 결정 시 따를 것)

새 용어가 글로서리 표에 없다면, 먼저 아래 패턴에서 일치 여부를 확인:

| 한국어 개념 | 식별자 패턴 | 예 |
|-----------|-----------|-----|
| `OO 상태` | `OOStatus` (enum) | `AttendanceStatus`, `MatchStatus`, `FeeStatus` |
| `OO 모드` | `OOMode` (enum) | `LineupMode` (BALANCED, COMPETITIVE) |
| `OO 응답` | `OOResponse` (DTO) | `TeamResponse`, `LineupResponse` |
| `OO 요청` | `OORequest` (DTO) | `MatchRequest` |
| `OO 검색` / 필터 | `OOSearch` 또는 `OOFilter` | `MercenarySearch` |
| `OO 추천` | `OORecommendation` 또는 `OOSuggestion` | `LineupRecommendation` |
| `OO 매너 점수` | `MannerRating` | (Rating에 흡수) |
| `OO 알림` | `OONotification` | `MatchNotification` |
| `OO 멤버` | `OOMember` | `TeamMember` |
| `OO 권한` | `OORole` | `TeamRole` (CAPTAIN, MEMBER) |

---

## 5. 새 도메인 용어를 추가할 때 (워크플로우)

1. 루트 [`CLAUDE.md`](../../../CLAUDE.md)의 "핵심 도메인 용어" 표에 **한국어·영어·설명** 1행 추가
2. 같은 PR에서 코드 + 글로서리 동시 갱신 (둘 중 하나만 오면 리뷰어가 reject)
3. 기존 용어와 의미 충돌 점검 — 특히 비슷한 의미의 신규 용어는 **분리·통합 결정** 후 진행 (예: `Match` vs `Matching`)
4. 글로서리 추가가 부담스러우면 4번 표의 파생 패턴으로 흡수 검토

---

## 6. 셀프 체크리스트 (코드 작성 후)

새 식별자가 한 개라도 등장했다면 자문:

- [ ] 1번 표의 영문 칼럼 그대로 사용했는가?
- [ ] 한국어 → 영어 변환을 임의로 했다면 글로서리에 등록할 가치가 있는가?
- [ ] 한글 식별자(`출석상태`)·로마자(`chulseok`)·임의 약어(`Att`, `Mch`)를 쓰지 않았는가?
- [ ] 기존 용어(`Match`)와 의미가 충돌하지 않는가? (`Matching`과 분리 주의)
- [ ] 포지션은 표준 약어인가?

---

## 7. 위반 탐지 명령 (PR 전 자체 점검)

```bash
# 7-1. 식별자에 한글 포함 여부 (있으면 위반)
grep -rE '[가-힣]' \
  dugout-api/src/main/kotlin \
  dugout-ios/Features dugout-ios/Core \
  --include="*.kt" --include="*.swift" \
  | grep -vE '\".*[가-힣].*\"|//|/\*|^\s*\*' \
  | head -20

# 7-2. 로마자/임의 약어 의심 (yongbyeong, maeching, chulseok, hoebi, gujang, busu 등)
grep -rEi 'yongbyeong|maeching|chulseok|hoebi|gujang|busu|mercSearch|matchUp' \
  dugout-api/src dugout-ios \
  --include="*.kt" --include="*.swift"

# 7-3. 자주 틀리는 영문 (Stadium/Game/Tier/Rank 등)
grep -rE 'class\s+(Stadium|Game|Tier|Rank|Dues|BattingOrder|Foreigner|Guest)\b' \
  dugout-api/src dugout-ios \
  --include="*.kt" --include="*.swift"
```

위 3개 명령 모두 **출력이 비어 있어야 통과**. 한 줄이라도 나오면 글로서리 위반 — 1·3·4번 표를 보고 수정.

---

## 8. 적용 범위

이 글로서리는 다음 모든 곳에 적용된다:

| 레이어 | 적용 대상 |
|--------|-----------|
| **dugout-api** | 패키지·클래스·메소드·필드·JPA 테이블/컬럼·ErrorCode·`/api/v1/*` 경로 |
| **dugout-ios** | 모듈명(`Dugout{Class}Feature`)·타입·프로퍼티·파일명 |
| **dugout-ai** (예정) | Pydantic 모델·FastAPI 경로·feature 컬럼명 |
| **docs/** | PRD.md / TDD.md 본문에 영문 식별자 등장 시 1번 표와 일치 |

> 이 스킬은 단독으로 동작한다. 자동으로 코드를 고치지 않고, "이런 명명 규칙이 있다" 라는 **참조 지식**으로 사용된다. 위반은 PR 리뷰 또는 위 7번 검색 명령으로 탐지한다.

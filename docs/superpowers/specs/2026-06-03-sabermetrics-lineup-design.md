# 세이버매트릭스 기반 타순 추천 — 설계 문서

- 작성일: 2026-06-03
- 상태: 설계 확정 (구현 전)
- 관련: F4 AI 라인업 추천 확장, 신규 도메인 `record`

---

## 0. 배경과 핵심 판단

사용자 요구: "세이버매트릭스 기준으로 AI 라인업을 추천하고 싶다."

조사·분석 결과 핵심 결론:

1. **세이버매트릭스는 ML이 아니라 선형 모델 + 집계다.** wOBA/wRC+/FIP 등은 전부 raw 이벤트 로그(타석별 결과)에 가중치를 곱한 공식이다. "알고리즘화 가능한가"는 질문이 아니다 — 이미 알고리즘이다.
2. **진짜 병목은 데이터다.** 현재 앱은 `matches`에 최종 점수만 저장한다(`result_home/result_away`). 타석별 결과·선수별 기록을 한 줄도 수집하지 않는다. 세이버매트릭스는 이벤트 로그의 함수이므로, 수집 레이어 없이는 불가능하다.
3. **경쟁사 크롤링은 폐기.** 남의 앱 사용자 기록은 내 로스터와 연결 불가하고 PII·ToS 리스크가 크다. 자기 팀 데이터를 테스트 시드로 쓰는 것도, 어차피 픽스처는 마스킹해야 하므로 **합성 픽스처**가 우월하다. 운영 경로엔 크롤링이 영원히 못 들어간다(신규 팀마다 크롤링 불가).
4. **사회인 야구 표본에선 정통 세이버매트릭스가 거짓 정밀도다.** wOBA 안정화에 ~200타석, BABIP에 ~800인플레이타구가 필요한데 사회인은 시즌 50~100타석이 고작이다. MLB 득점환경에서 뽑은 선형 가중치도 사회인엔 안 맞는다. 타순 최적화 실익은 MLB 162경기 기준 1~2승인데, 20경기 시즌에선 노이즈에 묻힌다.

→ **채택 방향**: 정통 wOBA/마르코프가 아니라, 세이버매트릭스의 *검증된 통찰만* 취하고 작은 표본 노이즈는 통계적으로 누르는 **"The Book lite" + shrinkage** 접근.

---

## 1. 스코프

### 1-1. 포함

- 타석 단위(L2) 기록 수집 레이어 (신규 도메인 `record`)
- raw 카운트 집계 (dugout-api)
- shrinkage 보정 지표(OBP/SLG/ISO) + The Book lite 타순 엔진 (dugout-ai)
- 기존 헝가리안 수비 배치·공정성·모드와의 통합
- 합성 테스트 픽스처

### 1-2. 제외 (YAGNI — 명시적 잠금)

- 투수 지표(FIP 등) — 라인업은 타격, 별개 작업
- wOBA / 마르코프 시뮬레이션 / WAR — 거짓 정밀도로 기각
- 수비 지표(UZR/DRS/OAA) — 트래킹 데이터 부재로 불가
- L3 풀 기록(주자상황·아웃카운트) — 입력 마찰 과다, 보류
- 경쟁사 크롤링 — 폐기
- 선수 단위 ELO — 팀 ELO와 별개, 보류

---

## 2. 데이터 모델 (dugout-api)

신규 도메인 패키지 `domain/record/`. 영문 식별자는 구현 시 `dugout-glossary` 스킬로 재검증("기록"이 현재 글로서리에 없음).

```kotlin
// domain/record/entity/PlateAppearance.kt
// BaseEntity 상속, soft delete, 테이블 plate_appearances (snake_case)
class PlateAppearance(
    matchId: Long,            // 어느 경기
    teamMemberId: Long,       // 누가 (타자)
    result: BattingResult,    // 결과
    rbi: Int = 0,
)

enum class BattingResult {
    SINGLE, DOUBLE, TRIPLE, HOME_RUN,   // 안타류
    WALK, HIT_BY_PITCH,                 // 출루 (타수 제외)
    SACRIFICE_FLY,                      // 희생플라이
    STRIKEOUT, IN_PLAY_OUT,             // 아웃
    REACHED_ON_ERROR,                   // 실책출루
}
```

타석 단위(L2)이므로 주자상황·아웃카운트는 받지 않는다(그건 L3). 버튼 하나당 한 타석.

**`REACHED_ON_ERROR` 별도 분리 결정**: 사회인 야구는 실책이 잦다. 안타로 치면 타격 지표가 부풀고, 단순 아웃으로 치면 출루 사실이 사라진다. 세이버매트릭스 정의대로 **OBP·SLG 계산에선 아웃 취급**(타자 능력 아님)하되 데이터로는 보존해 향후 BABIP·운 분석 여지를 둔다.

집계는 별도 테이블 없이 **쿼리 집계**로 선수별 raw 카운트(타석수, 1B, 2B, 3B, HR, BB, HBP, SF, K, IN_PLAY_OUT, ROE)를 뽑는다. 기간 필터는 `matchId` 조인.

---

## 3. 데이터 흐름과 책임 분리

```
[입력]  POST /api/v1/records/plate-appearances              (dugout-api)
          → PlateAppearance 저장

[추천]  POST /api/v1/lineup/recommend  (기존 엔드포인트 확장)
          ① API: 선발 후보별 raw 카운트 집계 + 팀 합계
          ② AI 서비스로 카운트 전달 (LineupRecommendRequest 확장)
          ③ AI: shrinkage 보정 → 수비배치(헝가리안, 기존) → 타순(The Book lite, 신규)
          ④ 라인업 응답
```

**책임 경계**: 세이버매트릭스 판단을 AI 서비스 한 곳에 모은다.

- **dugout-api** = raw 카운트 집계만. 통계 해석 안 함. (데이터 주인)
- **dugout-ai** = shrinkage·보정지표·타순 결정 전부. (라인업 알고리즘 주인)

shrinkage 프라이어(팀 평균)는 AI가 받은 카운트를 합산해 자체 계산 → API는 평균 통계를 몰라도 된다. 수비 배치는 기존 헝가리안 그대로, 신규는 타순 레이어뿐.

---

## 4. 보정 지표 + 타순 엔진 (dugout-ai)

### 4-1. raw 카운트 → 기본 지표

실책출루는 아웃 취급(§2).

```
PA(타석)   = 1B+2B+3B+HR+BB+HBP+SF+K+IN_PLAY_OUT+ROE
AB(타수)   = PA − BB − HBP − SF
H(안타)    = 1B+2B+3B+HR
OBP        = (H + BB + HBP) / (AB + BB + HBP + SF)
SLG        = (1B + 2·2B + 3·3B + 4·HR) / AB
ISO        = SLG − (H/AB)        # 순수 장타력 (안타 거품 제거)
```

`ISO`를 따로 두는 이유: SLG는 단타 많은 똑딱이도 높다. ISO는 장타 순수 신호라 클린업 선정에 쓴다.

### 4-2. shrinkage 보정 (필수 안전장치)

각 지표를 팀 평균 쪽으로 끌어당긴다. `k=50` 가상 타석(튜닝 가능 상수):

```
adj_OBP = (선수출루합 + 팀평균OBP × k) / (선수분모 + k)
adj_ISO = (선수ISO × PA + 팀평균ISO × k) / (PA + k)
```

5타석짜리 선수는 거의 팀평균으로 수축, 200타석 베테랑은 거의 자기기록 유지. **라인업이 노이즈로 출렁이는 것을 막는 핵심 장치**라 선택지가 아니라 필수.

### 4-3. The Book lite 타순 슬로팅

선발 9명(수비배치 완료)을 두 점수로 평가:

```
leadoff_score  = adj_OBP
cleanup_score  = 0.7·adj_ISO + 0.3·adj_OBP
overall_score  = adj_OBP + 0.5·adj_ISO
```

슬롯 채우는 순서 (세이버매트릭스 발견 그대로):

1. **2번** ← `overall_score` 최고 (*The Book 핵심 반전: 2번에 최고 타자*, 누적 타석·득점기회 최다)
2. **1번** ← 남은 중 `leadoff_score` 최고 (순수 출루형)
3. **4번** ← 남은 중 `cleanup_score` 최고 (최고 장타)
4. **3번** ← 남은 중 `overall_score` 최고
5. **5번** ← 남은 중 `cleanup_score` 최고
6. **6~9번** ← 나머지를 `adj_OBP` 내림차순

각 배치에 이유 문자열("출루율 .420으로 2번 배치")을 응답에 담아 설명 가능성을 확보한다.

### 4-4. 콜드 스타트

기록 0인 신규 팀·선수는 shrinkage가 전원을 팀평균으로 수축 → 점수 동률 → 기존 `hungarian.py`의 좌우타 교차 로직(`_interleave_batting_order`)으로 자동 폴백. 기록이 쌓일수록 세이버매트릭스 타순이 서서히 드러난다.

### 4-5. 모드와의 관계

- **수비 배치 + 출전권**(선발/벤치) = 기존 BALANCED/COMPETITIVE + 공정성 그대로.
- **타순**(선발 9명 순서) = 두 모드 공통으로 보정지표 기반.

근거: 타순 재배열은 출전 시간을 거의 안 뺏는다(9명 다 친다). 공정성의 본질은 "벤치에 박히지 않게"이지 타순 번호가 아니다.

---

## 5. 에러 핸들링 (조직 표준 필수)

**dugout-api (Kotlin)** — `ErrorCode` enum + `BusinessException`, `GlobalExceptionHandler` 통과:

- `RECORD_MATCH_NOT_FOUND` — 존재하지 않는 경기에 타석 기록
- `RECORD_MEMBER_NOT_IN_TEAM` — 해당 경기 팀 소속 아닌 선수
- `RECORD_INVALID_RESULT` — `BattingResult` enum 외 값 (역직렬화 단계 차단)
- 집계 쿼리는 빈 결과를 0 카운트로 정상 처리

**dugout-ai (Python)** — `HTTPException` + 명시적 `try/except`:

- 선발 후보 < 9명 → `400`
- `AB=0` 등 분모 0 → `0.0` 처리(`ZeroDivisionError` 차단)
- 보정지표 계산 실패 → 콜드스타트 폴백으로 degrade (500 미발생)

---

## 6. 테스트

합성 픽스처가 핵심. 의도적 케이스 설계:

| 픽스처 | 검증 |
|---|---|
| 고출루·저장타 타자 (200타석) | 1~2번 배치 |
| 고장타·저출루 타자 (200타석) | 4번 배치 |
| 5타석 .900 OPS 노이즈 선수 | shrinkage 수축 → 상위타순 미배치 |
| 종합 최고타자 | **2번** 배치 (The Book 반전 검증) |
| 전원 기록 0 (신규팀) | 좌우타 교차 폴백 |

- **dugout-ai**: `pytest` — 슬로팅·shrinkage 수학·콜드스타트. `ruff`/`mypy` 통과.
- **dugout-api**: 집계 쿼리 정확성, 타석 기록 검증·권한, `ErrorCode` 매핑.

---

## 7. 문서 동기화 (CLAUDE.md 트리거)

- 새 entity `PlateAppearance` → TDD.md DB 스키마 (`plate_appearances`)
- 새 endpoint (`POST /records/plate-appearances`, `/lineup/recommend` 확장) → TDD.md API 설계
- 새 도메인 패키지 `domain/record/` → PRD.md 기능 명세 + TDD.md 패키지 구조
- 타순 알고리즘 변경 → TDD.md AI 알고리즘 (The Book lite + shrinkage)

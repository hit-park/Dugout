# AI 라인업 품질 검증 키트 (하위 프로젝트 A) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 세이버매트릭스 타순 엔진이 The Book lite 슬로팅과 shrinkage를 규칙대로 수행함을, 특성을 심은 합성 데이터로 반증 가능하게 검증하고 gameone 실데이터로 데모한다.

**Architecture:** 모든 코드는 `dugout-ai`(Python). 아키타입 생성기와 gameone 크롤러가 공통 `StatLine`(타석 결과 집계)을 만들고, `StatLine` → `AttendeeProfile`로 변환해 기존 엔진 `app.services.batting_order`에 **직접** 먹인다(api 우회, stateless 엔진). 검증은 pytest, 데모는 CLI 스크립트.

**Tech Stack:** Python 3.12, pytest, BeautifulSoup4(HTML 파싱), httpx(이미 의존성, fetch), 기존 `batting_order` 엔진.

**중요 제약:** 이 작업은 포트폴리오용이다. `docs/PRD.md`·`docs/TDD.md`를 **갱신하지 않는다**(나중 런칭 대비 분리 관리). 기존 엔진(`batting_order.py`, `hungarian.py`, `schemas/lineup.py`)도 **수정하지 않는다** — 주변 도구만 추가한다. gameone 실명은 적재 즉시 마스킹한다.

**참조 스펙:** `docs/superpowers/specs/2026-06-16-ai-lineup-validation-kit-design.md`

---

## File Structure

```
dugout-ai/
├── requirements.txt                      # 수정: beautifulsoup4 추가
├── app/tooling/
│   ├── __init__.py                       # 신규 (빈 파일)
│   ├── statline.py                       # 신규: StatLine, PlayerLine, to_attendee_profile
│   ├── archetypes.py                     # 신규: Archetype enum + 타석결과 가중치
│   ├── synthetic.py                      # 신규: 결정적 합성 생성기
│   └── gameone.py                        # 신규: 크롤러(fetch+parse+마스킹)
├── tests/
│   ├── test_synthetic.py                 # 신규: 생성기 단위 테스트
│   ├── test_gameone.py                   # 신규: 파서/마스킹/실데이터 스모크
│   └── test_lineup_validation.py         # 신규: The Book + shrinkage + 콜드스타트
└── scripts/
    └── demo_lineup.py                    # 신규: 엔진 직결 데모 CLI
```

각 파일 책임:
- `statline.py` — 두 데이터 소스의 공통 자료형 + 엔진 입력 변환. 순수 함수, 외부 의존 없음.
- `archetypes.py` — 아키타입별 타석 결과 확률 가중치 정의. 데이터만.
- `synthetic.py` — (아키타입, 표본크기, seed) → `StatLine`. 결정적 샘플링.
- `gameone.py` — club_idx → 마스킹된 `PlayerLine` 목록. 네트워크/파싱 에러 변환.
- 테스트 3종 — 엔진 직결 검증.
- `demo_lineup.py` — 합성/실데이터를 엔진에 먹여 추천 타순 + reason 출력.

모든 작업 전제: `cd dugout-ai && source .venv/bin/activate` (또는 `pip install -r requirements.txt`).

---

### Task 0: 의존성 + 패키지 스켈레톤

**Files:**
- Modify: `dugout-ai/requirements.txt`
- Create: `dugout-ai/app/tooling/__init__.py`

- [ ] **Step 1: beautifulsoup4 의존성 추가**

`dugout-ai/requirements.txt`의 `httpx==0.28.1` 줄 바로 다음에 추가:

```
beautifulsoup4==4.12.3
```

- [ ] **Step 2: 설치**

Run: `cd dugout-ai && pip install -r requirements.txt`
Expected: `Successfully installed beautifulsoup4-4.12.3 soupsieve-...`

- [ ] **Step 3: tooling 패키지 생성**

Create `dugout-ai/app/tooling/__init__.py` as an empty file (내용 없음).

- [ ] **Step 4: 커밋**

```bash
git add dugout-ai/requirements.txt dugout-ai/app/tooling/__init__.py
git commit -m "chore(ai): 검증 키트용 tooling 패키지 + beautifulsoup4 의존성"
```

---

### Task 1: StatLine + 엔진 입력 변환

**Files:**
- Create: `dugout-ai/app/tooling/statline.py`
- Test: `dugout-ai/tests/test_synthetic.py` (이 태스크에선 StatLine 변환만)

- [ ] **Step 1: 실패 테스트 작성**

Create `dugout-ai/tests/test_synthetic.py`:

```python
from app.schemas.lineup import AttendeeProfile
from app.tooling.statline import PlayerLine, StatLine, to_attendee_profile


def test_statline_defaults_are_zero():
    line = StatLine()
    assert line.singles == 0
    assert line.reached_on_errors == 0


def test_to_attendee_profile_maps_all_counts():
    line = StatLine(singles=3, doubles=1, walks=2, strikeouts=4, in_play_outs=5)
    profile = to_attendee_profile(line, user_id=7, primary_position="SS")
    assert isinstance(profile, AttendeeProfile)
    assert profile.user_id == 7
    assert profile.primary_position == "SS"
    assert profile.singles == 3
    assert profile.doubles == 1
    assert profile.walks == 2
    assert profile.strikeouts == 4
    assert profile.in_play_outs == 5
    assert profile.sub_positions == []


def test_player_line_holds_label_and_statline():
    pl = PlayerLine(label="박**(61)", statline=StatLine(singles=2))
    assert pl.label == "박**(61)"
    assert pl.statline.singles == 2
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `cd dugout-ai && pytest tests/test_synthetic.py -v`
Expected: FAIL — `ModuleNotFoundError: No module named 'app.tooling.statline'`

- [ ] **Step 3: statline.py 구현**

Create `dugout-ai/app/tooling/statline.py`:

```python
"""합성 생성기와 gameone 크롤러의 공통 자료형 + 엔진 입력 변환."""

from dataclasses import dataclass

from app.schemas.lineup import AttendeeProfile


@dataclass(frozen=True)
class StatLine:
    """타석 결과 집계 카운트. AttendeeProfile의 raw 카운트 필드와 1:1."""

    singles: int = 0
    doubles: int = 0
    triples: int = 0
    home_runs: int = 0
    walks: int = 0
    hit_by_pitch: int = 0
    sacrifice_flies: int = 0
    strikeouts: int = 0
    in_play_outs: int = 0
    reached_on_errors: int = 0


@dataclass(frozen=True)
class PlayerLine:
    """표시 라벨(마스킹된 이름 등) + StatLine. 데모/크롤러 출력 단위."""

    label: str
    statline: StatLine


def to_attendee_profile(
    line: StatLine,
    *,
    user_id: int,
    primary_position: str = "DH",
    sub_positions: list[str] | None = None,
    bats_left: bool = False,
    bench_ratio_recent: float = 0.0,
) -> AttendeeProfile:
    return AttendeeProfile(
        user_id=user_id,
        primary_position=primary_position,
        sub_positions=sub_positions or [],
        bats_left=bats_left,
        bench_ratio_recent=bench_ratio_recent,
        singles=line.singles,
        doubles=line.doubles,
        triples=line.triples,
        home_runs=line.home_runs,
        walks=line.walks,
        hit_by_pitch=line.hit_by_pitch,
        sacrifice_flies=line.sacrifice_flies,
        strikeouts=line.strikeouts,
        in_play_outs=line.in_play_outs,
        reached_on_errors=line.reached_on_errors,
    )
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `cd dugout-ai && pytest tests/test_synthetic.py -v`
Expected: PASS (3 passed)

- [ ] **Step 5: 커밋**

```bash
git add dugout-ai/app/tooling/statline.py dugout-ai/tests/test_synthetic.py
git commit -m "feat(ai): StatLine/PlayerLine + AttendeeProfile 변환 (검증 키트 공통 출구)"
```

---

### Task 2: 아키타입 정의

**Files:**
- Create: `dugout-ai/app/tooling/archetypes.py`
- Test: `dugout-ai/tests/test_synthetic.py` (추가)

- [ ] **Step 1: 실패 테스트 추가**

`dugout-ai/tests/test_synthetic.py` 상단 import에 추가:

```python
from app.tooling.archetypes import OUTCOMES, Archetype, weights
```

파일 끝에 테스트 추가:

```python
def test_outcomes_match_statline_fields():
    line = StatLine()
    for outcome in OUTCOMES:
        assert hasattr(line, outcome), f"{outcome} is not a StatLine field"
    assert len(OUTCOMES) == 10


def test_every_archetype_has_weight_for_every_outcome():
    for archetype in Archetype:
        w = weights(archetype)
        assert set(w.keys()) == set(OUTCOMES)
        assert all(v >= 0 for v in w.values())
        assert sum(w.values()) > 0
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `cd dugout-ai && pytest tests/test_synthetic.py -v`
Expected: FAIL — `ModuleNotFoundError: No module named 'app.tooling.archetypes'`

- [ ] **Step 3: archetypes.py 구현**

Create `dugout-ai/app/tooling/archetypes.py`:

```python
"""아키타입별 타석 결과 확률 가중치. 합성 데이터 생성용.

가중치는 합이 1일 필요 없다 — 샘플러가 정규화한다.
의도한 분리:
- OVERALL: 고출루 + 고장타 (종합 최고타자)
- PURE_ONBASE: 고출루 저장타 (순수 테이블세터)
- POWER: 저출루 고장타 (거포)
- AVERAGE: 팀 평균 근처 (충원용)
"""

from enum import Enum

OUTCOMES: tuple[str, ...] = (
    "singles",
    "doubles",
    "triples",
    "home_runs",
    "walks",
    "hit_by_pitch",
    "sacrifice_flies",
    "strikeouts",
    "in_play_outs",
    "reached_on_errors",
)


class Archetype(str, Enum):
    OVERALL = "OVERALL"
    PURE_ONBASE = "PURE_ONBASE"
    POWER = "POWER"
    AVERAGE = "AVERAGE"


_WEIGHTS: dict[Archetype, dict[str, float]] = {
    Archetype.OVERALL: {
        "singles": 18, "doubles": 7, "triples": 1, "home_runs": 7,
        "walks": 15, "hit_by_pitch": 1, "sacrifice_flies": 1,
        "strikeouts": 15, "in_play_outs": 33, "reached_on_errors": 2,
    },
    Archetype.PURE_ONBASE: {
        "singles": 20, "doubles": 3, "triples": 0, "home_runs": 1,
        "walks": 22, "hit_by_pitch": 1, "sacrifice_flies": 1,
        "strikeouts": 12, "in_play_outs": 38, "reached_on_errors": 2,
    },
    Archetype.POWER: {
        "singles": 10, "doubles": 8, "triples": 1, "home_runs": 8,
        "walks": 7, "hit_by_pitch": 1, "sacrifice_flies": 1,
        "strikeouts": 30, "in_play_outs": 32, "reached_on_errors": 2,
    },
    Archetype.AVERAGE: {
        "singles": 15, "doubles": 4, "triples": 1, "home_runs": 2,
        "walks": 8, "hit_by_pitch": 1, "sacrifice_flies": 1,
        "strikeouts": 18, "in_play_outs": 48, "reached_on_errors": 2,
    },
}


def weights(archetype: Archetype) -> dict[str, float]:
    """아키타입의 타석 결과 가중치 사본을 반환."""
    return dict(_WEIGHTS[archetype])
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `cd dugout-ai && pytest tests/test_synthetic.py -v`
Expected: PASS (5 passed)

- [ ] **Step 5: 커밋**

```bash
git add dugout-ai/app/tooling/archetypes.py dugout-ai/tests/test_synthetic.py
git commit -m "feat(ai): 합성 데이터 아키타입 4종 + 타석결과 가중치"
```

---

### Task 3: 결정적 합성 생성기

**Files:**
- Create: `dugout-ai/app/tooling/synthetic.py`
- Test: `dugout-ai/tests/test_synthetic.py` (추가)

- [ ] **Step 1: 실패 테스트 추가**

`dugout-ai/tests/test_synthetic.py` import에 추가:

```python
import pytest

from app.services import batting_order
from app.tooling import synthetic
```

파일 끝에 추가:

```python
def test_generate_is_deterministic_for_same_seed():
    a = synthetic.generate(Archetype.OVERALL, plate_appearances=100, seed=7)
    b = synthetic.generate(Archetype.OVERALL, plate_appearances=100, seed=7)
    assert a == b


def test_generate_total_outcomes_equals_pa():
    line = synthetic.generate(Archetype.POWER, plate_appearances=120, seed=3)
    total = (
        line.singles + line.doubles + line.triples + line.home_runs
        + line.walks + line.hit_by_pitch + line.sacrifice_flies
        + line.strikeouts + line.in_play_outs + line.reached_on_errors
    )
    assert total == 120


def test_generate_zero_pa_is_empty():
    assert synthetic.generate(Archetype.AVERAGE, plate_appearances=0, seed=1) == StatLine()


def test_generate_negative_pa_raises():
    with pytest.raises(ValueError):
        synthetic.generate(Archetype.AVERAGE, plate_appearances=-1, seed=1)


def test_archetypes_separate_in_expected_directions_at_large_sample():
    # 큰 표본에서 보정 지표가 의도한 방향으로 분리되는지 (생성기 품질 보증)
    pa = 2000
    overall = to_attendee_profile(synthetic.generate(Archetype.OVERALL, plate_appearances=pa, seed=1), user_id=1)
    onbase = to_attendee_profile(synthetic.generate(Archetype.PURE_ONBASE, plate_appearances=pa, seed=2), user_id=2)
    power = to_attendee_profile(synthetic.generate(Archetype.POWER, plate_appearances=pa, seed=3), user_id=3)
    average = to_attendee_profile(synthetic.generate(Archetype.AVERAGE, plate_appearances=pa, seed=4), user_id=4)

    team = [overall, onbase, power, average]
    team_obp, team_iso = batting_order._team_averages(team)
    obp: dict[int, float] = {}
    iso: dict[int, float] = {}
    for p in team:
        o, i = batting_order._adjusted(p, team_obp, team_iso)
        obp[p.user_id] = o
        iso[p.user_id] = i

    assert obp[2] > obp[3]                                   # 출루형 출루 > 거포 출루
    assert iso[3] > iso[2]                                   # 거포 장타 > 출루형 장타
    overall_score = {uid: obp[uid] + 0.5 * iso[uid] for uid in obp}
    assert overall_score[1] == max(overall_score.values())   # 종합형이 종합점수 최고
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `cd dugout-ai && pytest tests/test_synthetic.py -v`
Expected: FAIL — `ModuleNotFoundError: No module named 'app.tooling.synthetic'`

- [ ] **Step 3: synthetic.py 구현**

Create `dugout-ai/app/tooling/synthetic.py`:

```python
"""결정적 합성 타석 데이터 생성기.

(아키타입, 표본크기, seed) → StatLine. 주입된 seed로만 난수를 쓰므로
동일 입력은 항상 동일 출력 — 검증 테스트가 100% 재현된다.
"""

import random

from app.tooling.archetypes import OUTCOMES, Archetype, weights
from app.tooling.statline import StatLine


def generate(archetype: Archetype, *, plate_appearances: int, seed: int) -> StatLine:
    if plate_appearances < 0:
        raise ValueError("plate_appearances must be >= 0")

    w = weights(archetype)
    population = list(OUTCOMES)
    probs = [w[o] for o in population]
    rng = random.Random(seed)
    draws = rng.choices(population, weights=probs, k=plate_appearances)

    counts = {outcome: 0 for outcome in OUTCOMES}
    for outcome in draws:
        counts[outcome] += 1

    return StatLine(
        singles=counts["singles"],
        doubles=counts["doubles"],
        triples=counts["triples"],
        home_runs=counts["home_runs"],
        walks=counts["walks"],
        hit_by_pitch=counts["hit_by_pitch"],
        sacrifice_flies=counts["sacrifice_flies"],
        strikeouts=counts["strikeouts"],
        in_play_outs=counts["in_play_outs"],
        reached_on_errors=counts["reached_on_errors"],
    )
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `cd dugout-ai && pytest tests/test_synthetic.py -v`
Expected: PASS (10 passed). 만약 `test_archetypes_separate...`가 실패하면 가중치 분리가 부족한 것 — Task 2의 OVERALL/POWER home_runs·walks 격차를 더 벌린다(거의 발생 안 함).

- [ ] **Step 5: 커밋**

```bash
git add dugout-ai/app/tooling/synthetic.py dugout-ai/tests/test_synthetic.py
git commit -m "feat(ai): 결정적 합성 타석 생성기 (아키타입×표본크기×seed)"
```

---

### Task 4: 검증 — The Book lite 정확 슬롯

**Files:**
- Create: `dugout-ai/tests/test_lineup_validation.py`

- [ ] **Step 1: 실패 테스트 작성**

Create `dugout-ai/tests/test_lineup_validation.py`:

```python
"""세이버매트릭스 엔진 품질 검증 — 특성 심은 합성 데이터로 반증 가능하게.

설계: docs/superpowers/specs/2026-06-16-ai-lineup-validation-kit-design.md
"""

from app.services import batting_order
from app.tooling import synthetic
from app.tooling.archetypes import Archetype
from app.tooling.statline import StatLine, to_attendee_profile

POSITIONS = ["P", "C", "1B", "2B", "3B", "SS", "LF", "CF", "RF"]


def _distinct_archetype_starters(pa: int) -> list:
    """1=종합, 2=순수출루, 3=거포, 4~9=평범. seed는 uid 고정 → 결정적."""
    specs = [Archetype.OVERALL, Archetype.PURE_ONBASE, Archetype.POWER] + [Archetype.AVERAGE] * 6
    starters = []
    for idx, arch in enumerate(specs):
        uid = idx + 1
        line = synthetic.generate(arch, plate_appearances=pa, seed=100 + uid)
        starters.append(to_attendee_profile(line, user_id=uid, primary_position=POSITIONS[idx]))
    return starters


def test_the_book_slotting_at_large_sample():
    starters = _distinct_archetype_starters(pa=400)
    slots = batting_order.order(starters)
    assert slots is not None
    assert slots[1] == 2   # 종합 최고타자 → 2번 (The Book 반전)
    assert slots[2] == 1   # 순수 출루형 → 1번
    assert slots[3] == 4   # 거포 → 4번


def test_slots_six_to_nine_are_obp_descending():
    starters = _distinct_archetype_starters(pa=400)
    slots = batting_order.order(starters)
    assert slots is not None
    team_obp, team_iso = batting_order._team_averages(starters)
    adj_obp = {s.user_id: batting_order._adjusted(s, team_obp, team_iso)[0] for s in starters}
    tail = sorted((uid for uid, slot in slots.items() if slot >= 6), key=lambda u: slots[u])
    tail_obps = [adj_obp[uid] for uid in tail]
    assert tail_obps == sorted(tail_obps, reverse=True)
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `cd dugout-ai && pytest tests/test_lineup_validation.py -v`
Expected: 처음엔 import는 되지만, 만약 `app.tooling`이 비어 import 에러면 Task 1~3 완료 여부 확인. 정상이면 두 테스트 PASS 예상 — 그러나 TDD 절차상 먼저 실행해 기준선을 본다.

> 참고: 이 태스크는 엔진이 이미 존재하므로 "실패→통과" 대신 "엔진이 규칙을 지키는지 확인"이 목적이다. 반증 가능성은 Step 4에서 검증한다.

- [ ] **Step 3: 테스트 통과 확인**

Run: `cd dugout-ai && pytest tests/test_lineup_validation.py -v`
Expected: PASS (2 passed)

- [ ] **Step 4: 반증 가능성 확인(중요) — 일부러 깨보기**

`dugout-ai/app/services/batting_order.py`의 `take(2, overall)`과 `take(1, leadoff)` 두 줄 순서를 임시로 바꾼 뒤:

Run: `cd dugout-ai && pytest tests/test_lineup_validation.py::test_the_book_slotting_at_large_sample -v`
Expected: **FAIL** — 테스트가 규칙 위반을 잡아냄(반증 가능성 입증).
확인 후 **반드시 원복**(엔진 수정 금지 원칙). `git checkout dugout-ai/app/services/batting_order.py`

- [ ] **Step 5: 커밋**

```bash
git add dugout-ai/tests/test_lineup_validation.py
git commit -m "test(ai): The Book lite 정확 슬롯 검증 (종합→2 출루→1 거포→4)"
```

---

### Task 5: 검증 — shrinkage 메커니즘 + 콜드 스타트

**Files:**
- Modify: `dugout-ai/tests/test_lineup_validation.py` (추가)

- [ ] **Step 1: 실패 테스트 추가**

`dugout-ai/tests/test_lineup_validation.py` 끝에 추가:

```python
def test_shrinkage_pulls_extreme_player_toward_team_mean_at_small_sample():
    # 거포 1명을 평범형 8명 사이에 두고 PA 5 vs 200 비교.
    # 작은 표본일수록 보정 ISO가 팀 평균에 더 가깝다(극단값 불신).
    def build(power_pa: int) -> list:
        power = to_attendee_profile(
            synthetic.generate(Archetype.POWER, plate_appearances=power_pa, seed=42),
            user_id=1,
        )
        average = [
            to_attendee_profile(
                synthetic.generate(Archetype.AVERAGE, plate_appearances=200, seed=200 + i),
                user_id=i,
            )
            for i in range(2, 10)
        ]
        return [power, *average]

    small = build(5)
    large = build(200)
    small_obp, small_iso = batting_order._team_averages(small)
    large_obp, large_iso = batting_order._team_averages(large)
    _, adj_iso_small = batting_order._adjusted(small[0], small_obp, small_iso)
    _, adj_iso_large = batting_order._adjusted(large[0], large_obp, large_iso)

    assert abs(adj_iso_small - small_iso) < abs(adj_iso_large - large_iso)


def test_shrinkage_compresses_obp_spread_at_small_sample():
    def obp_spread(pa: int) -> float:
        starters = _distinct_archetype_starters(pa=pa)
        team_obp, team_iso = batting_order._team_averages(starters)
        adj_obps = [batting_order._adjusted(s, team_obp, team_iso)[0] for s in starters]
        return max(adj_obps) - min(adj_obps)

    assert obp_spread(5) < obp_spread(200)


def test_cold_start_returns_none():
    cold = [
        to_attendee_profile(StatLine(), user_id=i, primary_position=POSITIONS[i - 1])
        for i in range(1, 10)
    ]
    assert batting_order.order(cold) is None
```

- [ ] **Step 2: 테스트 실행**

Run: `cd dugout-ai && pytest tests/test_lineup_validation.py -v`
Expected: PASS (5 passed)
만약 `test_shrinkage_pulls_extreme...`가 실패하면 seed=42의 5타석 표본이 우연히 raw_iso를 팀평균보다 멀게 만든 경우 — seed를 다른 고정값(예: 7)으로 바꾸고, 바꾼 값을 코드에 박는다(여전히 결정적).

- [ ] **Step 3: 커밋**

```bash
git add dugout-ai/tests/test_lineup_validation.py
git commit -m "test(ai): shrinkage 평균수축/분산압축 + 콜드스타트 검증"
```

---

### Task 6: gameone 파서 + PII 마스킹 (오프라인)

**Files:**
- Create: `dugout-ai/app/tooling/gameone.py`
- Test: `dugout-ai/tests/test_gameone.py`

- [ ] **Step 1: 실패 테스트 작성**

Create `dugout-ai/tests/test_gameone.py`:

```python
"""gameone 크롤러 — 파싱/마스킹은 오프라인 합성 HTML로, 실데이터는 스모크로."""

from app.tooling.gameone import mask_name, parse_hitter_rows
from app.tooling.statline import StatLine

# 컬럼 순서(gameone 타자 랭킹): 순위0 이름1 타율2 게임수3 타석4 타수5 득점6
# 총안타7 1루타8 2루타9 3루타10 홈런11 루타12 타점13 도루14 도실15
# 희타16 희비17 볼넷18 고의4구19 사구20 삼진21 ... (24=출루율)
_HEADER = [
    "순위", "이름", "타율", "게임수", "타석", "타수", "득점", "총안타",
    "1루타", "2루타", "3루타", "홈런", "루타", "타점", "도루", "도실",
    "희타", "희비", "볼넷", "고의4구", "사구", "삼진", "병살", "장타율",
    "출루율", "도루성공률", "멀티히트", "OPS", "BB/K", "장타/안타",
]


def _row(values: dict[int, str]) -> str:
    cells = ["0"] * len(_HEADER)
    for idx, val in values.items():
        cells[idx] = val
    return "<tr>" + "".join(f"<td>{c}</td>" for c in cells) + "</tr>"


def _html(*rows: str) -> str:
    return f"<table><tbody>{''.join(rows)}</tbody></table>"


def test_mask_name_keeps_first_char_and_jersey():
    assert mask_name("홍길동(7)") == "홍**(7)"
    assert mask_name("김합성(61)") == "김**(61)"


def test_mask_name_without_jersey():
    assert mask_name("홍길동") == "홍**"


def test_parse_extracts_statline_and_masks_name():
    # 이름=홍길동(7) 타수=20 총안타=9 1루타5 2루타2 3루타1 홈런1 희비0 볼넷3 사구0 삼진4
    row = _row({1: "홍길동(7)", 5: "20", 7: "9", 8: "5", 9: "2", 10: "1", 11: "1", 18: "3", 21: "4"})
    rows = parse_hitter_rows(_html(row))
    assert len(rows) == 1
    pl = rows[0]
    assert pl.label == "홍**(7)"                  # 실명 마스킹
    assert pl.statline.singles == 5
    assert pl.statline.doubles == 2
    assert pl.statline.triples == 1
    assert pl.statline.home_runs == 1
    assert pl.statline.walks == 3
    assert pl.statline.strikeouts == 4
    # in_play_outs = 타수(20) - 안타(9) - 삼진(4) = 7
    assert pl.statline.in_play_outs == 7


def test_parse_skips_short_rows():
    short = "<tr><td>요약</td><td>3</td></tr>"
    good = _row({1: "김합성(11)", 5: "4", 7: "2", 8: "2", 21: "1"})
    rows = parse_hitter_rows(_html(short, good))
    assert len(rows) == 1
    assert rows[0].label == "김**(11)"


def test_parse_empty_table_returns_empty_list():
    assert parse_hitter_rows(_html()) == []
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `cd dugout-ai && pytest tests/test_gameone.py -v`
Expected: FAIL — `ModuleNotFoundError: No module named 'app.tooling.gameone'`

- [ ] **Step 3: gameone.py 구현 (파서+마스킹 부분)**

Create `dugout-ai/app/tooling/gameone.py`:

```python
"""gameone 사회인 야구 타자 랭킹 크롤러.

club_idx → 마스킹된 PlayerLine 목록. 실명(PII)은 적재 즉시 마스킹한다.
컬럼 인덱스는 라이브 DOM 기준이며, 사이트 개편 시 _COL_* 상수만 조정.
"""

import re

import httpx
from bs4 import BeautifulSoup

from app.core.errors import AIException
from app.tooling.statline import PlayerLine, StatLine

_BASE_URL = "https://www.gameone.kr/club/info/ranking/hitter"

# 0-based 컬럼 인덱스 (타자 랭킹 테이블)
_COL_NAME = 1
_COL_AB = 5
_COL_HITS = 7
_COL_SINGLES = 8
_COL_DOUBLES = 9
_COL_TRIPLES = 10
_COL_HOME_RUNS = 11
_COL_SAC_FLIES = 17
_COL_WALKS = 18
_COL_HBP = 20
_COL_STRIKEOUTS = 21
_COL_OBP = 24  # 최소 컬럼 수 가드 기준

_NAME_RE = re.compile(r"(.+?)\((\d+)\)\s*$")


def mask_name(raw: str) -> str:
    """실명을 첫 글자 + '**'로 마스킹. 등번호는 보존."""
    raw = raw.strip()
    m = _NAME_RE.match(raw)
    if m:
        name, jersey = m.group(1), m.group(2)
        return f"{name[0]}**({jersey})"
    return f"{raw[0]}**" if raw else "**"


def _to_int(text: str) -> int:
    try:
        return int(text.strip())
    except ValueError:
        return 0


def parse_hitter_rows(html: str) -> list[PlayerLine]:
    soup = BeautifulSoup(html, "html.parser")
    result: list[PlayerLine] = []
    for tr in soup.select("tr"):
        cells = [td.get_text(strip=True) for td in tr.find_all("td")]
        if len(cells) <= _COL_OBP:
            continue  # 헤더/요약/빈 행 스킵
        ab = _to_int(cells[_COL_AB])
        hits = _to_int(cells[_COL_HITS])
        strikeouts = _to_int(cells[_COL_STRIKEOUTS])
        statline = StatLine(
            singles=_to_int(cells[_COL_SINGLES]),
            doubles=_to_int(cells[_COL_DOUBLES]),
            triples=_to_int(cells[_COL_TRIPLES]),
            home_runs=_to_int(cells[_COL_HOME_RUNS]),
            walks=_to_int(cells[_COL_WALKS]),
            hit_by_pitch=_to_int(cells[_COL_HBP]),
            sacrifice_flies=_to_int(cells[_COL_SAC_FLIES]),
            strikeouts=strikeouts,
            in_play_outs=max(0, ab - hits - strikeouts),
            reached_on_errors=0,  # 페이지에 없음 → ROE는 아웃 취급과 일관
        )
        result.append(PlayerLine(label=mask_name(cells[_COL_NAME]), statline=statline))
    return result


def fetch_club_hitters(club_idx: int, *, timeout: float = 10.0) -> list[PlayerLine]:
    try:
        resp = httpx.get(_BASE_URL, params={"club_idx": club_idx}, timeout=timeout)
        resp.raise_for_status()
    except httpx.HTTPStatusError as e:
        raise AIException(
            code="GAMEONE_HTTP_ERROR",
            message=f"gameone 응답 오류: {e.response.status_code}",
            status_code=502,
        ) from e
    except httpx.RequestError as e:
        raise AIException(
            code="GAMEONE_UNREACHABLE",
            message="gameone 연결 실패",
            status_code=502,
        ) from e

    rows = parse_hitter_rows(resp.text)
    if not rows:
        raise AIException(
            code="GAMEONE_EMPTY",
            message="파싱된 타자 기록이 없습니다 (club_idx 확인 또는 DOM 변경)",
            status_code=404,
        )
    return rows
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `cd dugout-ai && pytest tests/test_gameone.py -v`
Expected: PASS (5 passed)

- [ ] **Step 5: 커밋**

```bash
git add dugout-ai/app/tooling/gameone.py dugout-ai/tests/test_gameone.py
git commit -m "feat(ai): gameone 타자 랭킹 파서 + 실명 마스킹 (오프라인 검증)"
```

---

### Task 7: gameone 실데이터 스모크 + DOM 검증

**Files:**
- Modify: `dugout-ai/tests/test_gameone.py` (추가)

- [ ] **Step 1: 라이브 DOM 1회 확인 (수동)**

Run: `cd dugout-ai && python -c "import httpx; r=httpx.get('https://www.gameone.kr/club/info/ranking/hitter', params={'club_idx':40837}, timeout=10); print(r.status_code); from bs4 import BeautifulSoup; s=BeautifulSoup(r.text,'html.parser'); rows=[[td.get_text(strip=True) for td in tr.find_all('td')] for tr in s.select('tr')]; rows=[x for x in rows if len(x)>24]; print(len(rows), 'rows'); print(rows[0] if rows else 'NO DATA')"`
Expected: `200`, 1개 이상 row, 첫 row의 인덱스 1=이름, 5=타수, 7=총안타 등이 `app/tooling/gameone.py`의 `_COL_*`와 일치하는지 **육안 확인**.
불일치 시 `_COL_*` 상수만 조정(엔진/다른 파일 수정 금지). 네트워크 안 되면 이 스텝은 스킵하고 Step 2로.

- [ ] **Step 2: 스모크 테스트 추가**

`dugout-ai/tests/test_gameone.py` import에 추가:

```python
import pytest

from app.core.errors import AIException
from app.services import batting_order
from app.tooling import gameone
from app.tooling.statline import to_attendee_profile
```

파일 끝에 추가:

```python
def test_fetch_real_club_40837_smoke():
    """실데이터 무결성 스모크. 네트워크 없으면 skip(품질 아닌 파이프라인 검증)."""
    try:
        rows = gameone.fetch_club_hitters(40837)
    except AIException as e:
        pytest.skip(f"gameone unavailable: {e.code}")

    assert len(rows) >= 1
    for pl in rows:
        assert "**" in pl.label  # 실명이 평문으로 남지 않음

    if len(rows) >= 9:
        starters = [
            to_attendee_profile(rows[i].statline, user_id=i + 1, primary_position="DH")
            for i in range(9)
        ]
        slots = batting_order.order(starters)
        # 기록이 하나라도 있으면 1~9 유일 배정, 전부 0이면 None(콜드스타트) — 둘 다 정상
        if slots is not None:
            assert sorted(slots.values()) == list(range(1, 10))
```

- [ ] **Step 3: 테스트 실행**

Run: `cd dugout-ai && pytest tests/test_gameone.py -v`
Expected: PASS 또는 `test_fetch_real_club_40837_smoke`가 SKIP(네트워크 없음). 둘 다 허용.

- [ ] **Step 4: 커밋**

```bash
git add dugout-ai/tests/test_gameone.py dugout-ai/app/tooling/gameone.py
git commit -m "test(ai): gameone 실데이터 스모크 (네트워크 없으면 skip)"
```

---

### Task 8: 엔진 직결 데모 CLI

**Files:**
- Create: `dugout-ai/scripts/demo_lineup.py`

- [ ] **Step 1: 데모 스크립트 작성**

Create `dugout-ai/scripts/demo_lineup.py`:

```python
"""라인업 추천 데모 (엔진 직결, api 우회).

사용법:
  python scripts/demo_lineup.py                  # 합성 PA=200 vs PA=5 비교
  python scripts/demo_lineup.py --pa 300         # 합성 표본 크기 지정
  python scripts/demo_lineup.py --club-idx 40837 # gameone 실데이터(마스킹)
"""

import argparse

from app.services import batting_order
from app.tooling import gameone, synthetic
from app.tooling.archetypes import Archetype
from app.tooling.statline import PlayerLine, to_attendee_profile

POSITIONS = ["P", "C", "1B", "2B", "3B", "SS", "LF", "CF", "RF"]


def _synthetic_lines(pa: int) -> list[PlayerLine]:
    specs = [Archetype.OVERALL, Archetype.PURE_ONBASE, Archetype.POWER] + [Archetype.AVERAGE] * 6
    return [
        PlayerLine(label=f"{arch.value}#{idx + 1}", statline=synthetic.generate(arch, plate_appearances=pa, seed=100 + idx + 1))
        for idx, arch in enumerate(specs)
    ]


def _render(title: str, lines: list[PlayerLine]) -> str:
    usable = lines[:9]
    starters = [
        to_attendee_profile(pl.statline, user_id=i + 1, primary_position=POSITIONS[i % len(POSITIONS)])
        for i, pl in enumerate(usable)
    ]
    out = [f"## {title}"]
    slots = batting_order.order(starters)
    if slots is None:
        out.append("(콜드 스타트 — 기록 없음, 좌우 교차 폴백)")
        return "\n".join(out)

    reason_by_uid = batting_order.reasons(starters) or {}
    label_by_uid = {i + 1: usable[i].label for i in range(len(usable))}
    for uid in sorted(slots, key=lambda u: slots[u]):
        out.append(f"{slots[uid]}번  {label_by_uid[uid]:<16} — {reason_by_uid.get(uid, '')}")
    return "\n".join(out)


def main() -> None:
    parser = argparse.ArgumentParser(description="라인업 추천 데모 (엔진 직결)")
    parser.add_argument("--club-idx", type=int, default=None, help="gameone club_idx")
    parser.add_argument("--pa", type=int, default=200, help="합성 데이터 표본 타석 수")
    args = parser.parse_args()

    if args.club_idx is not None:
        lines = gameone.fetch_club_hitters(args.club_idx)
        print(_render(f"gameone club {args.club_idx} (실데이터·마스킹)", lines))
    else:
        print(_render(f"합성 PA={args.pa} (특성 또렷)", _synthetic_lines(args.pa)))
        print()
        print(_render("합성 PA=5 (작은 표본 — shrinkage로 평탄화)", _synthetic_lines(5)))


if __name__ == "__main__":
    main()
```

- [ ] **Step 2: 합성 데모 실행 확인**

Run: `cd dugout-ai && python scripts/demo_lineup.py`
Expected: "합성 PA=200" 블록에서 `2번  OVERALL#1 — 종합 최고타자로 2번 배치...`, `1번 PURE_ONBASE#2`, `4번 POWER#3`가 출력되고, "합성 PA=5" 블록은 reason의 수치(.xxx)가 서로 더 평탄(중앙값 근처)함.

- [ ] **Step 3: 실데이터 데모 실행 확인 (네트워크 필요)**

Run: `cd dugout-ai && python scripts/demo_lineup.py --club-idx 40837`
Expected: 마스킹된 라벨(`박**(61)` 등)과 추천 타순 출력. 네트워크 불가 시 `AIException`(GAMEONE_UNREACHABLE) 메시지 — 정상 동작.

- [ ] **Step 4: 커밋**

```bash
git add dugout-ai/scripts/demo_lineup.py
git commit -m "feat(ai): 엔진 직결 라인업 추천 데모 CLI (합성/gameone)"
```

---

### Task 9: 전체 검증 + 린트/타입 + 포트폴리오 문서

**Files:**
- Create: `docs/superpowers/portfolio/ai-lineup-validation-results.md`

- [ ] **Step 1: 전체 테스트**

Run: `cd dugout-ai && pytest -v`
Expected: 전부 PASS (기존 테스트 + 신규, gameone 스모크는 PASS 또는 SKIP). 기존 테스트가 깨지면 안 됨.

- [ ] **Step 2: 린트**

Run: `cd dugout-ai && ruff check .`
Expected: `All checks passed!` (실패 시 `ruff check --fix .` 후 재확인)

- [ ] **Step 3: 타입 체크**

Run: `cd dugout-ai && mypy app/`
Expected: `Success: no issues found`. 신규 파일에서 에러 시 해당 파일만 수정.

- [ ] **Step 4: 포트폴리오 결과 문서 작성 (PRD/TDD 아님)**

Create `docs/superpowers/portfolio/ai-lineup-validation-results.md`:

```markdown
# AI 라인업 추천 — 품질 검증 결과 (포트폴리오)

> 정식 제품 문서(PRD/TDD)와 분리된 포트폴리오 산출물.

## 무엇을 검증했나
세이버매트릭스 타순 엔진(The Book lite + shrinkage k=50)을 **특성을 심은 합성 데이터**로 검증.

- The Book 슬로팅: 종합 최고타자 → 2번, 순수 출루형 → 1번, 거포 → 4번 (큰 표본에서 정확 슬롯 단언).
- shrinkage: 작은 표본(5타석)일수록 보정 지표가 팀 평균으로 수축(극단값 불신), 큰 표본(200타석)에서 또렷.
- 콜드 스타트: 기록 0이면 좌우 교차 폴백.
- 반증 가능성: 엔진의 슬로팅 순서를 일부러 바꾸면 검증 테스트가 실패함을 확인.

## 재현
```
cd dugout-ai && pytest tests/test_lineup_validation.py -v
python scripts/demo_lineup.py            # 합성 PA=200 vs PA=5
python scripts/demo_lineup.py --club-idx 40837   # 실데이터(마스킹)
```

## 한계 (정직하게)
- 합성 데이터는 "엔진이 설계 규칙대로 동작하는가"를 검증한다. "사회인 야구에서 이 타순이 득점을 늘리는가"는 실경기 결과가 쌓여야 검증 가능 — 별도 과제.
- gameone 표본은 경기수가 작아 대부분 shrinkage 폴백 구간. 실데이터는 "파이프라인이 진짜로 돈다"의 증거.
```

- [ ] **Step 5: 커밋**

```bash
git add docs/superpowers/portfolio/ai-lineup-validation-results.md
git commit -m "docs: AI 라인업 품질 검증 결과 (포트폴리오 — PRD/TDD 분리)"
```

---

## Self-Review 결과

- **Spec 커버리지**: 합성 생성기(Task 2-3) / gameone 크롤러(Task 6-7) / 공통 출구 StatLine(Task 1) / The Book 정확 슬롯(Task 4) / shrinkage 메커니즘(Task 5) / 콜드 스타트(Task 5) / 실데이터 스모크(Task 7) / 엔진 직결 데모(Task 8) / PII 마스킹(Task 6) / 결정성(Task 3) — 스펙 전 항목 매핑됨.
- **PRD/TDD 미갱신·엔진 미수정**: 계획 전반에서 강제. Task 4 Step 4는 일부러 깨본 뒤 `git checkout`으로 원복.
- **타입 일관성**: `StatLine`/`PlayerLine`/`to_attendee_profile`/`generate`/`parse_hitter_rows`/`fetch_club_hitters`/`mask_name` 시그니처가 정의 태스크와 사용 태스크에서 일치.
- **결정성 주의**: 합성 seed는 전부 코드에 고정. 드물게 표본 우연으로 단언이 흔들리면 seed만 다른 고정값으로 교체(Task 3 Step 4, Task 5 Step 2에 명시).
- **gameone DOM 리스크**: 라이브 DOM과 `_COL_*` 인덱스 불일치 가능 — Task 7 Step 1에서 1회 육안 확인 후 상수만 조정.

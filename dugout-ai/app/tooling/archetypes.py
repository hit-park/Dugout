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

"""세이버매트릭스 엔진 품질 검증 — 특성 심은 합성 데이터로 반증 가능하게.

설계: docs/superpowers/specs/2026-06-16-ai-lineup-validation-kit-design.md
"""

from app.schemas.lineup import AttendeeProfile
from app.services import batting_order
from app.tooling import synthetic
from app.tooling.archetypes import Archetype
from app.tooling.statline import StatLine, to_attendee_profile

POSITIONS = ["P", "C", "1B", "2B", "3B", "SS", "LF", "CF", "RF"]


def _distinct_archetype_starters(pa: int) -> list[AttendeeProfile]:
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


def test_shrinkage_pulls_extreme_player_toward_team_mean_at_small_sample():
    # 팀 평균을 고정해 표본 크기 효과만 격리한다.
    # 같은 seed의 5타석 표본은 200타석 표본의 접두부 → 표본 크기만 다르다.
    # 작은 표본일수록 보정 ISO가 (동일한) 팀 평균에 더 가깝게 수축된다.
    fixed_team_obp, fixed_team_iso = 0.320, 0.120
    small = to_attendee_profile(
        synthetic.generate(Archetype.POWER, plate_appearances=5, seed=42), user_id=1
    )
    large = to_attendee_profile(
        synthetic.generate(Archetype.POWER, plate_appearances=200, seed=42), user_id=1
    )
    _, adj_iso_small = batting_order._adjusted(small, fixed_team_obp, fixed_team_iso)
    _, adj_iso_large = batting_order._adjusted(large, fixed_team_obp, fixed_team_iso)

    assert abs(adj_iso_small - fixed_team_iso) < abs(adj_iso_large - fixed_team_iso)


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

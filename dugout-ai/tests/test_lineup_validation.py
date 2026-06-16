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

"""세이버매트릭스 타순 엔진 (The Book lite + shrinkage). TDD 3-3-1.

- 보정 OBP/SLG/ISO 계산 (실책출루 ROE는 아웃 취급)
- 작은 표본 노이즈는 평균 회귀(shrinkage, k=50)로 보정
- The Book lite 슬로팅: 2번에 종합 최고타자, 1번 출루형, 4번 장타형
- 콜드 스타트(기록 0): None 반환 → 호출 측이 좌우타 교차로 폴백
"""

from collections.abc import Callable
from dataclasses import dataclass

from app.schemas.lineup import AttendeeProfile

K_SHRINKAGE = 50  # 가상 타석 — 표본이 작을수록 팀 평균으로 수축


@dataclass(frozen=True)
class Components:
    pa: int
    ab: int
    hits: int
    on_base: int
    obp_denom: int
    total_bases: int


def _components(a: AttendeeProfile) -> Components:
    hits = a.singles + a.doubles + a.triples + a.home_runs
    pa = (
        hits
        + a.walks + a.hit_by_pitch + a.sacrifice_flies
        + a.strikeouts + a.in_play_outs + a.reached_on_errors
    )
    ab = pa - a.walks - a.hit_by_pitch - a.sacrifice_flies
    on_base = hits + a.walks + a.hit_by_pitch
    obp_denom = ab + a.walks + a.hit_by_pitch + a.sacrifice_flies
    total_bases = a.singles + 2 * a.doubles + 3 * a.triples + 4 * a.home_runs
    return Components(pa, ab, hits, on_base, obp_denom, total_bases)


def has_records(attendees: list[AttendeeProfile]) -> bool:
    return any(_components(a).pa > 0 for a in attendees)


def _team_averages(attendees: list[AttendeeProfile]) -> tuple[float, float]:
    total_on_base = total_obp_denom = total_tb = total_hits = total_ab = 0
    for a in attendees:
        c = _components(a)
        total_on_base += c.on_base
        total_obp_denom += c.obp_denom
        total_tb += c.total_bases
        total_hits += c.hits
        total_ab += c.ab
    team_obp = total_on_base / total_obp_denom if total_obp_denom else 0.0
    team_iso = (total_tb - total_hits) / total_ab if total_ab else 0.0
    return team_obp, team_iso


def _adjusted(a: AttendeeProfile, team_obp: float, team_iso: float) -> tuple[float, float]:
    c = _components(a)
    adj_obp = (c.on_base + team_obp * K_SHRINKAGE) / (c.obp_denom + K_SHRINKAGE)
    raw_iso = (c.total_bases - c.hits) / c.ab if c.ab else 0.0
    adj_iso = (raw_iso * c.pa + team_iso * K_SHRINKAGE) / (c.pa + K_SHRINKAGE)
    return adj_obp, adj_iso


def _format_avg(x: float) -> str:
    """야구 표기법: 소수점 3자리, 정수부 0 제거 (e.g. 0.420 → '.420')."""
    return f"{x:.3f}"[1:]  # '0.420' → '.420'


def order(starters: list[AttendeeProfile]) -> dict[int, int] | None:
    """선발 9명의 타순(user_id -> 1..9)을 반환. 기록 없으면 None(폴백 신호)."""
    if not has_records(starters):
        return None

    team_obp, team_iso = _team_averages(starters)
    adj = {
        s.user_id: _adjusted(s, team_obp, team_iso)
        for s in starters
    }

    def leadoff(uid: int) -> float:
        return adj[uid][0]                          # adj_obp

    def cleanup(uid: int) -> float:
        obp, iso = adj[uid]
        return 0.7 * iso + 0.3 * obp

    def overall(uid: int) -> float:
        obp, iso = adj[uid]
        return obp + 0.5 * iso

    remaining = [s.user_id for s in starters]
    slot_of: dict[int, int] = {}

    def take(slot: int, scorer: Callable[[int], float]) -> None:
        chosen = max(remaining, key=scorer)
        remaining.remove(chosen)
        slot_of[chosen] = slot

    take(2, overall)    # The Book 반전: 2번에 종합 최고타자
    take(1, leadoff)    # 순수 출루형
    take(4, cleanup)    # 최고 장타
    take(3, overall)
    take(5, cleanup)
    for i, uid in enumerate(sorted(remaining, key=leadoff, reverse=True)):
        slot_of[uid] = 6 + i        # 6~9번: adj_OBP 내림차순

    return slot_of


def reasons(starters: list[AttendeeProfile]) -> dict[int, str] | None:
    """선발 9명의 타순 이유 문자열(user_id -> 설명)을 반환. 기록 없으면 None.

    order()와 동일한 로직으로 adj_obp/adj_iso를 계산하고 각 슬롯에 맞는 이유 문자열 생성.
    """
    slot_of = order(starters)
    if slot_of is None:
        return None

    team_obp, team_iso = _team_averages(starters)
    adj = {s.user_id: _adjusted(s, team_obp, team_iso) for s in starters}

    result: dict[int, str] = {}
    for uid, slot in slot_of.items():
        obp, iso = adj[uid]
        obp_str = _format_avg(obp)
        iso_str = _format_avg(iso)
        if slot == 1:
            result[uid] = f"출루율 {obp_str}로 1번(테이블세터) 배치"
        elif slot == 2:
            result[uid] = f"종합 최고타자로 2번 배치 (출루 {obp_str}/순장타 {iso_str})"
        elif slot == 3:
            result[uid] = "종합 상위 타자로 3번 배치"
        elif slot == 4:
            result[uid] = f"장타력(ISO {iso_str})으로 4번 배치"
        elif slot == 5:
            result[uid] = "장타력으로 5번 배치"
        else:
            result[uid] = f"출루율 {obp_str}로 {slot}번 배치"
    return result

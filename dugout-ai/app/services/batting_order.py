"""세이버매트릭스 타순 엔진 (The Book lite + shrinkage). TDD 3-3-1.

- 보정 OBP/SLG/ISO 계산 (실책출루 ROE는 아웃 취급)
- 작은 표본 노이즈는 평균 회귀(shrinkage, k=50)로 보정
- The Book lite 슬로팅: 2번에 종합 최고타자, 1번 출루형, 4번 장타형
- 콜드 스타트(기록 0): None 반환 → 호출 측이 좌우타 교차로 폴백
"""

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

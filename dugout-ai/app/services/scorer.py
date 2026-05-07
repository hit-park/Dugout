"""매칭 가중 스코어 (TDD 3-4).

총점 = skill 40% + distance 25% + time 20% + manner 15%

skill_score (ELO 차이 기반):
  0~50: 100점 / 50~100: 100 - (diff-50)*0.4 / 100~200: 80 - (diff-100)*0.4 / 200+: max(0, 40 - (diff-200)*0.2)

distance_score:
  0~5km: 100 / 5~15: 100 - (dist-5)*2 / 15~30: 80 - (dist-15)*2 / 30+: max(0, 50 - (dist-30))

time_score = time_overlap_ratio * 100
manner_score = away_manner_score / 5.0 * 100
"""

from app.schemas.matching import (
    MatchingScoreBreakdown,
    MatchingScoreRequest,
    MatchingScoreResponse,
)


def compute(req: MatchingScoreRequest) -> MatchingScoreResponse:
    skill = _skill_score(abs(req.home_elo - req.away_elo))
    distance = _distance_score(req.distance_km)
    time = req.time_overlap_ratio * 100
    manner = (req.away_manner_score / 5.0) * 100

    total = (skill * 0.40) + (distance * 0.25) + (time * 0.20) + (manner * 0.15)

    return MatchingScoreResponse(
        total_score=round(total, 2),
        breakdown=MatchingScoreBreakdown(
            skill=round(skill, 2),
            distance=round(distance, 2),
            time=round(time, 2),
            manner=round(manner, 2),
        ),
    )


def _skill_score(diff: int) -> float:
    if diff <= 50:
        return 100.0
    if diff <= 100:
        return 100.0 - (diff - 50) * 0.4
    if diff <= 200:
        return 80.0 - (diff - 100) * 0.4
    return max(0.0, 40.0 - (diff - 200) * 0.2)


def _distance_score(dist_km: float) -> float:
    if dist_km <= 5:
        return 100.0
    if dist_km <= 15:
        return 100.0 - (dist_km - 5) * 2
    if dist_km <= 30:
        return 80.0 - (dist_km - 15) * 2
    return max(0.0, 50.0 - (dist_km - 30))

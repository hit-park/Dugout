"""용병 후보 필터링 + 점수 산출.

스코어:
  position_match: 매칭된 포지션 수 / needed_positions 길이 × 50점
  region_match:   매칭된 지역 수 / max(needed_regions, 1) × 30점
  rating_bonus:   rating / 5.0 × 20점
"""

from app.schemas.mercenary import (
    MercenaryMatch,
    MercenaryRecommendRequest,
    MercenaryRecommendResponse,
)


def recommend(req: MercenaryRecommendRequest) -> MercenaryRecommendResponse:
    needed_pos = set(req.needed_positions)
    needed_regions = set(req.needed_regions)
    needed_days = set(req.available_days)

    matches: list[MercenaryMatch] = []
    for cand in req.candidates:
        cand_pos = set(cand.positions)
        cand_regions = set(cand.regions)
        cand_days = set(cand.available_days)

        matched_pos = needed_pos & cand_pos
        matched_regions = needed_regions & cand_regions

        # 필수: 포지션 1개 이상 매칭
        if not matched_pos:
            continue
        # 지역 조건이 있으면 1개 이상 매칭 필수
        if needed_regions and not matched_regions:
            continue
        # 요일 조건이 있으면 1개 이상 매칭 필수
        if needed_days and not (needed_days & cand_days):
            continue

        position_score = (len(matched_pos) / len(needed_pos)) * 50
        region_score = (len(matched_regions) / max(len(needed_regions), 1)) * 30 if needed_regions else 30.0
        rating_bonus = (cand.rating / 5.0) * 20

        total = round(position_score + region_score + rating_bonus, 2)

        matches.append(
            MercenaryMatch(
                user_id=cand.user_id,
                nickname=cand.nickname,
                score=total,
                matched_positions=sorted(matched_pos),
                matched_regions=sorted(matched_regions),
            )
        )

    matches.sort(key=lambda m: m.score, reverse=True)
    return MercenaryRecommendResponse(request_id=req.request_id, matches=matches)

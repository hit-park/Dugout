"""헝가리안 알고리즘 기반 라인업 추천 (TDD 3-3).

1. 9개 필드 포지션(P/C/1B/2B/3B/SS/LF/CF/RF) × 출석자 적합도 매트릭스 생성
   - 주포지션: 1.0
   - 서브포지션: 0.7
   - 그 외: 0.2
   - BALANCED 모드: 최근 N경기 벤치 비율이 높을수록 ×1.2 가산
2. scipy.optimize.linear_sum_assignment 로 총 적합도 최대화 배정
3. 타순: 출석자 중 좌타/우타 교차로 1~9번 (단순화: 적합도 높은 순)
4. 9명 초과는 DH 또는 벤치 (is_bench=True)
"""

import numpy as np
from scipy.optimize import linear_sum_assignment  # type: ignore[import-untyped]

from app.core.errors import AIException
from app.schemas.lineup import (
    AttendeeProfile,
    LineupAssignment,
    LineupRecommendRequest,
    LineupRecommendResponse,
)
from app.services import batting_order as batting_engine

FIELD_POSITIONS: list[str] = ["P", "C", "1B", "2B", "3B", "SS", "LF", "CF", "RF"]


def recommend(req: LineupRecommendRequest) -> LineupRecommendResponse:
    if len(req.attendees) < len(FIELD_POSITIONS):
        raise AIException(
            code="INSUFFICIENT_ATTENDEES",
            message=f"라인업 추천에 최소 {len(FIELD_POSITIONS)}명이 필요합니다",
            status_code=400,
        )

    starters_indices, fairness_note = _solve_assignment(req)
    bench_indices = [i for i in range(len(req.attendees)) if i not in set(starters_indices.values())]

    entries: list[LineupAssignment] = []
    starters_sorted_by_position = [(pos, starters_indices[pos]) for pos in FIELD_POSITIONS]

    # 타순: 기록 있으면 세이버매트릭스(The Book lite), 없으면 좌우타 교차 폴백
    starter_users = [req.attendees[idx] for _, idx in starters_sorted_by_position]
    sabermetric_order = batting_engine.order(starter_users)

    if sabermetric_order is None:
        fallback_list = _interleave_batting_order(starter_users)
        order_of: dict[int, int] = {uid: fallback_list.index(uid) + 1 for uid in fallback_list}
    else:
        order_of = sabermetric_order

    for pos, idx in starters_sorted_by_position:
        attendee = req.attendees[idx]
        entries.append(
            LineupAssignment(
                user_id=attendee.user_id,
                position=pos,
                batting_order=order_of[attendee.user_id],
                is_bench=False,
            )
        )

    for idx in bench_indices:
        attendee = req.attendees[idx]
        entries.append(
            LineupAssignment(
                user_id=attendee.user_id,
                position="DH",
                batting_order=None,
                is_bench=True,
            )
        )

    return LineupRecommendResponse(
        match_id=req.match_id,
        is_ai_generated=True,
        source="AI",
        fairness_note=fairness_note,
        entries=entries,
    )


def _solve_assignment(req: LineupRecommendRequest) -> tuple[dict[str, int], str | None]:
    n = len(req.attendees)
    cost = np.zeros((n, len(FIELD_POSITIONS)))

    fairness_applied = False
    for i, attendee in enumerate(req.attendees):
        for j, pos in enumerate(FIELD_POSITIONS):
            base = _fitness(attendee, pos)
            if req.lineup_mode == "BALANCED" and attendee.bench_ratio_recent >= 0.4:
                base *= 1.2
                fairness_applied = True
            cost[i, j] = base

    # linear_sum_assignment minimizes — convert to maximize
    row_ind, col_ind = linear_sum_assignment(-cost)

    selected = {FIELD_POSITIONS[col_ind[i]]: int(row_ind[i]) for i in range(len(FIELD_POSITIONS))}
    note = "최근 벤치가 많은 선수에게 출전 가산점을 적용했습니다." if fairness_applied else None
    return selected, note


def _fitness(attendee: AttendeeProfile, position: str) -> float:
    if attendee.primary_position == position:
        return 1.0
    if position in attendee.sub_positions:
        return 0.7
    return 0.2


def _interleave_batting_order(starters: list[AttendeeProfile]) -> list[int]:
    """좌타/우타 교차 단순 구현. starters의 batting_order 결정.

    구현:  좌타 그룹 + 우타 그룹을 ZIP 으로 교차. 한쪽이 더 많으면 뒤에 붙임.
    """
    lefts = [s.user_id for s in starters if s.bats_left]
    rights = [s.user_id for s in starters if not s.bats_left]

    interleaved: list[int] = []
    for left, right in zip(lefts, rights, strict=False):
        interleaved.extend([left, right])
    interleaved.extend(lefts[len(rights):])
    interleaved.extend(rights[len(lefts):])
    return interleaved

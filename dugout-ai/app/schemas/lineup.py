from pydantic import BaseModel, Field


class AttendeeProfile(BaseModel):
    """라인업 추천 입력 - 출석자 1명의 프로필."""

    user_id: int
    primary_position: str            # 주포지션 (P/C/1B/2B/3B/SS/LF/CF/RF/DH)
    sub_positions: list[str] = Field(default_factory=list)
    bench_ratio_recent: float = Field(ge=0.0, le=1.0, default=0.0, description="최근 N경기 벤치 비율")
    bats_left: bool = False          # 좌타 여부 (타순 좌우 교차용)

    # 타석 기록(L2) raw 카운트 — 전부 0이면 콜드 스타트로 간주해 좌우타 교차 폴백
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


class LineupRecommendRequest(BaseModel):
    match_id: int
    attendees: list[AttendeeProfile] = Field(min_length=9)
    lineup_mode: str = Field(default="BALANCED", pattern="^(BALANCED|COMPETITIVE)$")


class LineupAssignment(BaseModel):
    user_id: int
    position: str
    batting_order: int | None = None
    is_bench: bool = False
    reason: str | None = None


class LineupRecommendResponse(BaseModel):
    match_id: int
    is_ai_generated: bool = True
    source: str = "AI"
    fairness_note: str | None = None
    entries: list[LineupAssignment]

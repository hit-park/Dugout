from pydantic import BaseModel, Field


class MatchingScoreRequest(BaseModel):
    """매칭 스코어 산출 입력 — 백엔드에서 미리 두 팀의 통계를 모아 전달."""

    home_elo: int
    away_elo: int
    distance_km: float = Field(ge=0.0)
    time_overlap_ratio: float = Field(ge=0.0, le=1.0, description="선호 시간 겹침 비율 0~1")
    away_manner_score: float = Field(ge=0.0, le=5.0)


class MatchingScoreBreakdown(BaseModel):
    skill: float = Field(ge=0.0, le=100.0)
    distance: float = Field(ge=0.0, le=100.0)
    time: float = Field(ge=0.0, le=100.0)
    manner: float = Field(ge=0.0, le=100.0)


class MatchingScoreResponse(BaseModel):
    total_score: float = Field(ge=0.0, le=100.0)
    breakdown: MatchingScoreBreakdown

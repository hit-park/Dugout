from pydantic import BaseModel, Field


class MercenaryCandidate(BaseModel):
    """식별자만 전달 — PII(닉네임 등)는 dugout-api 응답 매핑 단계에서만 사용."""

    user_id: int
    regions: list[str] = Field(default_factory=list)
    positions: list[str] = Field(default_factory=list)
    available_days: list[str] = Field(default_factory=list)
    rating: float = Field(ge=0.0, le=5.0, default=0.0)
    total_games: int = Field(ge=0, default=0)


class MercenaryRecommendRequest(BaseModel):
    request_id: int
    needed_positions: list[str]
    needed_regions: list[str] = Field(default_factory=list)
    available_days: list[str] = Field(default_factory=list)
    candidates: list[MercenaryCandidate] = Field(default_factory=list)


class MercenaryMatch(BaseModel):
    user_id: int
    score: float = Field(ge=0.0, le=100.0)
    matched_positions: list[str]
    matched_regions: list[str]


class MercenaryRecommendResponse(BaseModel):
    request_id: int
    matches: list[MercenaryMatch]

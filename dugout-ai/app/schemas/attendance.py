from pydantic import BaseModel, Field


class AttendanceContext(BaseModel):
    """출석 예측 입력. 백엔드에서 미리 집계한 통계를 그대로 전달받는 형태."""

    user_id: int
    match_id: int
    day_of_week_rate: float = Field(ge=0.0, le=1.0, description="해당 요일 출석률 0~1")
    recent_5_attendance_rate: float = Field(ge=0.0, le=1.0, description="최근 5경기 출석률 0~1")
    consecutive_absent_count: int = Field(ge=0)
    distance_km: float = Field(ge=0.0)
    has_bad_weather: bool = False
    avg_response_lag_hours: float = Field(ge=0.0, default=0.0)


class AttendancePredictionResponse(BaseModel):
    user_id: int
    match_id: int
    prediction: str  # "ATTEND" | "ABSENT"
    probability: float = Field(ge=0.0, le=1.0)
    confidence: float = Field(ge=0.0, le=1.0)
    reasons: list[str] = Field(default_factory=list)

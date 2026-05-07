"""규칙 기반 출석 예측 (TDD 3-2).

가중치:
  base 0.50
  + (요일 출석률 - 0.5) × 0.30
  + (최근 5경기 출석률 - 0.5) × 0.25
  - 날씨 페널티 × 0.15
  - 거리 페널티 × 0.10  (30km 초과부터)
  - 연속 불참 페널티 × 0.10  (3회 이상부터)
  - 응답 지연 페널티 × 0.05  (24h 초과부터)

데이터 부족 단계의 베이스라인. 팀당 50경기 누적 후 LightGBM으로 전환 예정.
"""

from app.schemas.attendance import AttendanceContext, AttendancePredictionResponse


def predict(ctx: AttendanceContext) -> AttendancePredictionResponse:
    score = 0.5
    reasons: list[str] = []

    day_delta = (ctx.day_of_week_rate - 0.5) * 0.30
    score += day_delta
    if abs(day_delta) > 0.05:
        reasons.append(f"요일 출석률 {ctx.day_of_week_rate:.0%}")

    recent_delta = (ctx.recent_5_attendance_rate - 0.5) * 0.25
    score += recent_delta
    if abs(recent_delta) > 0.05:
        reasons.append(f"최근 5경기 {ctx.recent_5_attendance_rate:.0%}")

    if ctx.has_bad_weather:
        score -= 0.15
        reasons.append("악천후 예보")

    if ctx.distance_km > 30:
        penalty = min((ctx.distance_km - 30) / 50, 1.0) * 0.10
        score -= penalty
        reasons.append(f"구장 거리 {ctx.distance_km:.0f}km")

    if ctx.consecutive_absent_count >= 3:
        score -= 0.10
        reasons.append(f"연속 불참 {ctx.consecutive_absent_count}회")

    if ctx.avg_response_lag_hours > 24:
        score -= 0.05
        reasons.append("응답 지연 평소보다 김")

    score = max(0.0, min(1.0, score))
    confidence = min(abs(score - 0.5) * 2, 0.95)
    prediction = "ATTEND" if score >= 0.5 else "ABSENT"

    return AttendancePredictionResponse(
        user_id=ctx.user_id,
        match_id=ctx.match_id,
        prediction=prediction,
        probability=score,
        confidence=confidence,
        reasons=reasons,
    )

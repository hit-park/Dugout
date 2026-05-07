from fastapi import APIRouter

from app.core.errors import AIException
from app.schemas.attendance import AttendanceContext, AttendancePredictionResponse
from app.services import rule_predictor

router = APIRouter(prefix="/api/attendance", tags=["attendance"])


@router.post("/predict", response_model=AttendancePredictionResponse)
def predict(ctx: AttendanceContext) -> AttendancePredictionResponse:
    try:
        return rule_predictor.predict(ctx)
    except AIException:
        raise
    except Exception as e:
        raise AIException(
            code="ATTENDANCE_PREDICT_FAILED",
            message="출석 예측 중 오류가 발생했습니다",
            status_code=500,
        ) from e

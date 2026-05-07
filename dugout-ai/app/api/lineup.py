from fastapi import APIRouter

from app.core.errors import AIException
from app.schemas.lineup import LineupRecommendRequest, LineupRecommendResponse
from app.services import hungarian

router = APIRouter(prefix="/api/lineups", tags=["lineup"])


@router.post("/recommend", response_model=LineupRecommendResponse)
def recommend(req: LineupRecommendRequest) -> LineupRecommendResponse:
    try:
        return hungarian.recommend(req)
    except AIException:
        raise
    except Exception as e:
        raise AIException(
            code="LINEUP_RECOMMEND_FAILED",
            message="라인업 추천 중 오류가 발생했습니다",
            status_code=500,
        ) from e

from fastapi import APIRouter

from app.core.errors import AIException
from app.schemas.mercenary import MercenaryRecommendRequest, MercenaryRecommendResponse
from app.services import mercenary_filter

router = APIRouter(prefix="/api/mercenary", tags=["mercenary"])


@router.post("/recommend", response_model=MercenaryRecommendResponse)
def recommend(req: MercenaryRecommendRequest) -> MercenaryRecommendResponse:
    try:
        return mercenary_filter.recommend(req)
    except AIException:
        raise
    except Exception as e:
        raise AIException(
            code="MERCENARY_RECOMMEND_FAILED",
            message="용병 추천 중 오류가 발생했습니다",
            status_code=500,
        ) from e

from fastapi import APIRouter

from app.core.errors import AIException
from app.schemas.matching import MatchingScoreRequest, MatchingScoreResponse
from app.services import scorer

router = APIRouter(prefix="/api/matching", tags=["matching"])


@router.post("/score", response_model=MatchingScoreResponse)
def score(req: MatchingScoreRequest) -> MatchingScoreResponse:
    try:
        return scorer.compute(req)
    except AIException:
        raise
    except Exception as e:
        raise AIException(
            code="MATCHING_SCORE_FAILED",
            message="매칭 스코어 산출 중 오류가 발생했습니다",
            status_code=500,
        ) from e

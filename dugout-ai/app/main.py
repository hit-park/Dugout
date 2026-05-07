from fastapi import FastAPI

from app.api import attendance, lineup, matching, mercenary
from app.core.errors import (
    AIException,
    ai_exception_handler,
    unhandled_exception_handler,
)

app = FastAPI(
    title="dugout-ai",
    version="0.1.0",
    description="Dugout AI 서비스 — 출석 예측 / 라인업 추천 / 매칭 스코어 / 용병 추천",
)

app.add_exception_handler(AIException, ai_exception_handler)  # type: ignore[arg-type]
app.add_exception_handler(Exception, unhandled_exception_handler)

app.include_router(attendance.router)
app.include_router(lineup.router)
app.include_router(matching.router)
app.include_router(mercenary.router)


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok", "service": "dugout-ai", "version": "0.1.0"}

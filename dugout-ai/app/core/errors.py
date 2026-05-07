from fastapi import Request
from fastapi.responses import JSONResponse


class AIException(Exception):
    """dugout-ai 도메인 에러.

    백엔드 ErrorCode 패턴과 1:1 호환되도록 code 문자열을 enum 이름과 동일하게 유지.
    예: code="INSUFFICIENT_ATTENDEES" ↔ Kotlin ErrorCode.INSUFFICIENT_ATTENDEES
    """

    def __init__(self, code: str, message: str, status_code: int = 400) -> None:
        super().__init__(message)
        self.code = code
        self.message = message
        self.status_code = status_code


async def ai_exception_handler(request: Request, exc: AIException) -> JSONResponse:
    return JSONResponse(
        status_code=exc.status_code,
        content={"code": exc.code, "message": exc.message},
    )


async def unhandled_exception_handler(request: Request, exc: Exception) -> JSONResponse:
    """예측 못 한 에러는 500으로 통일 — 내부 stack trace는 응답에 절대 노출 금지."""
    return JSONResponse(
        status_code=500,
        content={"code": "INTERNAL_ERROR", "message": "AI 서비스 내부 오류가 발생했습니다"},
    )

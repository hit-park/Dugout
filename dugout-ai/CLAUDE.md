# CLAUDE.md — dugout-ai (Python AI 서비스)

> 루트 [`/CLAUDE.md`](../CLAUDE.md) 의 모든 규칙이 적용된다. 이 파일은 AI 서비스 모듈 한정 추가 규칙.

## 빠른 시작

```bash
cd dugout-ai
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt

uvicorn app.main:app --reload --port 8001    # 로컬 실행 (포트 8001)
pytest                                        # 테스트
ruff check .                                  # 린트
mypy app/                                     # 타입 체크
```

## 기술 스택

- **Python 3.12+**
- **FastAPI 0.115+** + Uvicorn
- **Pydantic v2** (모든 DTO는 `BaseModel` 상속)
- **scipy** (헝가리안 알고리즘 — `linear_sum_assignment`)
- **numpy** (벡터 연산)
- **테스트**: pytest + httpx (TestClient)
- **린트/타입**: ruff + mypy

## 패키지 구조 (`app/`)

```
app/
├── main.py              # FastAPI 앱 + 라우터 등록 + /health
├── api/                 # 라우터 (얇게 — 비즈니스 로직 금지)
│   ├── attendance.py    # POST /api/attendance/predict
│   ├── lineup.py        # POST /api/lineups/recommend
│   ├── matching.py      # POST /api/matching/score
│   └── mercenary.py     # POST /api/mercenary/recommend
├── services/            # 알고리즘 (도메인 로직, 라이브러리 의존)
│   ├── rule_predictor.py    # 규칙 기반 출석 예측
│   ├── hungarian.py         # scipy 헝가리안 라인업
│   ├── scorer.py            # 가중 매칭 스코어 (40/25/20/15)
│   └── mercenary_filter.py  # 용병 후보 필터링
├── schemas/             # Pydantic v2 DTO (Request/Response)
└── core/                # 공통
    ├── config.py        # Settings (환경변수 → BaseSettings)
    └── errors.py        # AIException, error_handler
```

## API 설계 규칙

- 모든 endpoint는 `/api/*` prefix
- 요청/응답은 항상 Pydantic `BaseModel` (dict 직접 반환 금지)
- HTTP 상태: 성공 200, 검증 실패 422 (Pydantic 자동), 비즈니스 에러는 `AIException` 가공
- 라우터는 얇게 — 알고리즘은 `services/`에 위치
- 타임아웃 가드: 모든 알고리즘은 30초 이내 완료 (큰 입력은 422로 거절)

## 에러 핸들링 (필수 패턴)

루트 [CLAUDE.md](../CLAUDE.md) 보안 가드 5번에 따라 **모든 엔드포인트는 `try/except` + `HTTPException`** 일관 적용.

```python
# app/core/errors.py
class AIException(Exception):
    def __init__(self, code: str, message: str, status_code: int = 400):
        self.code = code
        self.message = message
        self.status_code = status_code

# 도메인 service에서 사용
raise AIException(code="INSUFFICIENT_ATTENDEES", message="출석자가 9명 미만입니다", status_code=400)
```

`main.py`의 exception handler가 `AIException`을 표준 응답으로 변환.

**금지**:
- raw `Exception("...")` 던지기
- `print(e)` 또는 stderr 출력만으로 에러 처리 (반드시 logger 사용)
- 알고리즘 함수가 `Optional[T]` 반환 후 호출 측에서 None 검사 (예외 던질 것)

## 도메인 용어

루트 [`CLAUDE.md`](../CLAUDE.md) "핵심 도메인 용어" 표 그대로 적용:
Team, Match, Attendance, Lineup, Fee, Matching, Mercenary, Ground, Division, Rating.

자유 번역·로마자·임의 약어 금지. ([`dugout-glossary` 스킬](../.claude/skills/dugout-glossary/SKILL.md))

## 개인정보·보안 가드

- AI 입력으로 들어오는 user_id, team_id 등은 식별자만 (이름·연락처 절대 미포함)
- 로그에 사용자 식별 정보 출력 금지 (id 정도만 OK)
- 모델 학습 데이터에 PII 절대 미포함

## 알고리즘 참고 (TDD.md 기준)

- **출석 예측 (rule_predictor)**: 요일률 0.30 + 최근5경기 0.25 + 날씨 0.15 + 거리 0.10 + 연속불참 0.10 + 응답속도 0.05
- **라인업 추천 (hungarian)**: 적합도 매트릭스 → `scipy.optimize.linear_sum_assignment(maximize=True)` → 좌우 타석 교차 + 공정성 보정
- **매칭 스코어 (scorer)**: skill 40% + distance 25% + time 20% + manner 15%
- **용병 추천 (mercenary_filter)**: 지역·포지션·시간대 필터 + rating 정렬

## 테스트 가이드

- 단위 테스트: `tests/test_<도메인>.py`
- TestClient로 라우터 통합 테스트
- 알고리즘 단위 테스트: 입력 → 결정론적 출력 검증
- 픽스처에 PII 절대 금지 (`user_id=1`, `team_id=10` 같은 합성 ID만)

## 보안 체크리스트 (PR 전 자체 점검)

- [ ] 모든 엔드포인트가 `try/except` + `HTTPException`인가?
- [ ] 응답에 PII가 평문으로 포함되지 않는가?
- [ ] 환경변수 (`AI_*`)는 `core/config.py`에서만 읽는가?
- [ ] 알고리즘이 큰 입력으로 30초 초과하지 않는가?
- [ ] 로그에 sensitive data 노출이 없는가?

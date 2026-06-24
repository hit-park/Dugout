# AI 라인업 웹 데모 — 설계 (포트폴리오 하위 프로젝트 B)

> 정식 제품 문서(PRD/TDD)와 분리된 포트폴리오 산출물. PRD/TDD를 갱신하지 않는다.
> 피벗 컨텍스트: A(검증 키트, 완료) → **B(웹 프론트엔드)** → D(배포). C(안드로이드)는 옵션.

## 목적

iOS 개발자가 웹까지 직접 만든다는 풀스택 어필 + 세이버매트릭스 AI 라인업이라는
독특한 자산을 브라우저에서 보여준다. 본질적으로 기존 `demo_lineup` CLI의 웹판이다.

비목표: 팀 운영 전체(로그인/출석/회비)의 웹 재현, iOS 앱 미러링. 단일 화면 데모에 집중한다.

## 범위 결정 (확정)

| 항목 | 결정 | 이유 |
|------|------|------|
| 범위 | AI 라인업 단일 데모 (한 화면) | 코드 최소 대비 가장 독특한 자산 노출 |
| 스택 | Vite + React + TS, 단일 페이지 | 취업/이직 실무 시그널. 라우터/Redux/UI 라이브러리 없음 |
| 입력 | 프리셋 합성 팀 + 토글 2개 | 9×10 stat 수동 입력은 노가다·버그밭. 토글이 와우 포인트 |
| 호출 대상 | AI FastAPI(`:8001`) 직결 | Kotlin 경유는 인증·DB가 필요해 데모엔 과함. AI 엔진이 주인공 |

## 아키텍처

```
dugout-web/ (Vite + React + TS, 단일 페이지)
  [모드 토글][표본 토글]
        → POST :8001/api/lineups/recommend  (AI 서비스 직결)
        → entries(position·batting_order·reason) + fairness_note 렌더
```

- 라우터·Redux·UI 컴포넌트 라이브러리 미사용. 상태는 `useState` 2~3개, 패칭은 `fetch` 한 번.
- `match_id`는 더미 `0`.

## 데이터 — 로직 중복 없음 (핵심)

프리셋 팀 stat을 TS로 다시 짜는 것은 Python 합성 아키타입 생성기의 재구현이라 금지.

- `dugout-ai`에 **기존 합성 생성기를 호출해 JSON으로 덤프하는 작은 스크립트** 추가.
- 표본 2종 픽스처 생성·커밋: `PA=200`(특성 또렷), `PA=5`(shrinkage 평탄화).
- 각 픽스처 = `LineupRecommendRequest.attendees` 형태의 `AttendeeProfile` 리스트(≥9명).
- 프런트는 이 정적 JSON 2개를 import만 한다. 아키타입 로직의 단일 출처는 Python.
- 토글 의미:
  - 표본 토글 → 어느 픽스처를 POST할지 (PA=200 / PA=5)
  - 모드 토글 → 요청의 `lineup_mode` (BALANCED / COMPETITIVE)
  - 둘 다 데이터 재생성 없이 즉시 재요청.

## 백엔드 변경 — 딱 하나

- FastAPI `app/main.py`에 **CORS 미들웨어 활성화** (`CORSMiddleware`, dev origin 허용).
  브라우저에서 호출하려면 필수 — 안 켜면 동작 자체가 막힌다. 생략 불가.
- 그 외 기존 코드(엔진/스키마/라우터) 무수정.

## 화면 (단일 페이지)

- 상단: 모드 토글(BALANCED/COMPETITIVE), 표본 토글(PA=200/PA=5).
- 결과 영역:
  - **타순 리스트**: 1~9번 + 각 슬롯 `reason` 문자열 + `fairness_note`.
  - **CSS 야구 다이아몬드**: 수비 포지션 라벨 9칸. SVG 라이브러리 없이 CSS 배치.
- 와우 포인트: 같은 화면에서 PA=200 ↔ PA=5 토글 시 라인업이 평탄화되는 게 보인다
  (검증 문서 인사이트의 시각화).

## 에러 핸들링

- 신뢰 경계는 `fetch` 하나.
- AIException 응답(`code`/`message`)을 화면에 그대로 표시.
- 네트워크 실패 시 사용자에게 보이는 에러 메시지 노출.
- 그 이상 방어 코드 없음(내부 신뢰).

## 테스트 — 한 개

- vitest 스모크 1개: 목(mock) `LineupRecommendResponse`를 주입했을 때 타순/포지션이
  렌더되는지(응답 → 뷰 매핑 검증).
- 프레임워크 픽스처·전체 스위트·E2E 없음. 비자명 로직은 매핑뿐이라 한 개로 충분.

## 로컬 실행

```
# 터미널 1 — AI 서비스
cd dugout-ai && uvicorn app.main:app --reload --port 8001

# 터미널 2 — 웹
cd dugout-web && npm run dev
```

## 한계 / 후속

- 데모는 AI 직결이라 인증·DB 경로를 거치지 않는다. "Kotlin 백엔드까지 엮은 풀스택"을
  보여주려면 D(배포)에서 백엔드를 띄울 때 경유 호출로 전환하는 게 순서.
- 픽스처는 합성 데이터. 실데이터(gameone) 연동은 A에서 별도 검증된 파이프라인이며 B 범위 밖.

# 웹 데모 정적 배포 — 설계 (포트폴리오 하위 프로젝트 D)

> 정식 제품 문서(PRD/TDD)와 분리된 포트폴리오 산출물. PRD/TDD를 갱신하지 않는다.
> 피벗 컨텍스트: A(검증 키트, 완료) → B(웹 데모, 완료) → **D(무료 배포)**. C(안드로이드)는 옵션.

## 목적

B에서 만든 AI 라인업 웹 데모를 무료로 공개 배포해, 채용 담당자가 링크 하나로 동작을 본다.

비목표: 라이브 백엔드 운영. 라인업 추천 출력이 결정적이라, 라이브 AI 서버 대신 미리 계산된
정적 응답으로 배포한다. (피벗 목표의 "실서버 배포" 시그널은 의도적으로 포기 — 무료 티어
콜드스타트로 데모가 고장난 것처럼 보이는 리스크를 피하는 선택.)

## 범위 결정 (확정)

| 항목 | 결정 | 이유 |
|------|------|------|
| 배포 형태 | 순수 정적 사이트 (백엔드 없음) | 출력이 결정적 → 응답 4개만 존재. 런타임 백엔드 불필요 |
| 호스팅 | GitHub Pages (Actions 워크플로) | 제3자 계정 불필요, 모든 게 레포에 살아 재현성 최상 |
| 응답 공급 | 빌드 전 미리 계산해 번들에 import | 런타임 fetch·CORS·콜드스타트 제거 |

## 아키텍처

```
dugout-ai/scripts/dump_web_responses.py
  커밋된 픽스처(pa200/pa5) × 모드(BALANCED/COMPETITIVE)
    → 기존 엔진 hungarian.recommend() 호출
    → dugout-web/src/responses.json  (4개 키, 각 LineupResponse)

dugout-web (정적 빌드)
  App: responses[`${sample}_${mode}`] 동기 룩업 → LineupView 렌더
    → npm run build (base=/Dugout/) → dist/
      → GitHub Actions → GitHub Pages (https://hit-park.github.io/Dugout/)
```

## 1. 응답 미리 계산 (dugout-ai)

- `scripts/dump_web_responses.py` 추가. B의 `dump_web_fixtures.py`와 같은 패턴.
- 입력: 커밋된 `dugout-web/src/fixtures/{pa200,pa5}.json`의 `attendees` (단일 출처 — 프런트가
  라벨을 매기는 그 픽스처와 동일 입력이라 분기 없음).
- 각 (픽스처 × 모드)에 대해 `LineupRecommendRequest(match_id=0, attendees=..., lineup_mode=...)`를
  만들어 `app.services.hungarian.recommend(req)` 호출, 결과를 `model_dump()`.
- 출력: `dugout-web/src/responses.json` =
  `{ "pa200_BALANCED": <LineupResponse>, "pa200_COMPETITIVE": ..., "pa5_BALANCED": ..., "pa5_COMPETITIVE": ... }`.
- 엔진 로직 재구현 금지 — 호출만. 생성된 JSON은 커밋(프런트가 Python 없이 빌드되도록).

## 2. 프런트 변경 (App.tsx만)

- `useEffect` + `recommend()` fetch 제거 → 동기 룩업 `responses[`${sample}_${mode}`]`로 교체.
- 로딩·에러 상태 제거 (4개 키가 모든 토글 조합을 덮어 룩업이 항상 성공).
- `LineupView`·토글 2개·픽스처(라벨용 import)는 그대로.
- `responses.json`은 `src/`에 두고 import(번들). 런타임 fetch·public 경로 안 씀.

## 3. api.ts 처리

- `LineupResponse` 타입은 계속 사용(응답 타이핑).
- `recommend()`는 배포 앱에서 미사용이 되지만 **삭제하지 않는다** — B에서 만든 타입 안전 API
  클라이언트 포트폴리오 산출물이며, 로컬에서 라이브 엔진을 띄워 검증할 때 쓸 수 있다.
  (ponytail 관점 데드코드지만, 포트폴리오 시그널을 우선한 의도적 잔존.)
- 백엔드 CORS(B에서 추가)도 무해하므로 그대로 둔다.

## 4. Vite / 배포 설정

- `vite.config.ts`에 `base: '/Dugout/'` 추가 (프로젝트 페이지 서브경로 — 에셋 URL 해석에 필수).
- `.github/workflows/deploy-web.yml`:
  - 트리거: `push` to `main`, `paths: ['dugout-web/**', '.github/workflows/deploy-web.yml']`.
  - 권한: `pages: write`, `id-token: write`. concurrency 그룹으로 중복 배포 방지.
  - job: checkout → setup-node(20) → `cd dugout-web && npm ci && npm run build`
    → `actions/configure-pages` → `actions/upload-pages-artifact`(path `dugout-web/dist`)
    → `actions/deploy-pages`.
- SPA 라우팅 없음 → 404 폴백 불필요.

## 5. 사용자 수동 단계 (gh 미로그인 + main 직접푸시 금지로 자동화 불가)

배포 spec의 일부로 명시하되, 코드가 아니라 사람이 수행:
1. 레포를 **public**으로 전환 (무료 GitHub Pages 조건).
2. Settings → Pages → Source = **GitHub Actions**.
3. main에 반영(merge 또는 push) → 워크플로 실행 → `https://hit-park.github.io/Dugout/` 라이브.

## 에러 핸들링

- 정적 룩업은 항상 성공(4키가 토글 전 조합 커버) — 런타임 에러 경로 없음.
- 유일한 실패 경계는 빌드: CI가 build 실패 시 시끄럽게 깨진다(deploy-pages 미실행).

## 테스트

- precompute 결정성 체크 1개(pytest): `responses.json`이 4개 키를 갖고, 각 응답의 entries가
  픽스처 출석자 수만큼이며 타순(batting_order)이 존재하는지 단언. (엔진 자체 테스트는 별도 존재.)
- `npm run build`가 `base=/Dugout/`로 정상 산출되는지 로컬 1회 검증(빌드 게이트).
- 기존 LineupView 스모크 테스트는 변경 없음(룩업으로 바뀐 App은 별도 테스트 안 함 — 동기
  룩업은 자명, ponytail로 단일 스모크 유지).

## 한계 / 후속

- **CI 워크플로 실그린은 푸시 후에만 검증 가능** — 자동 검증은 "표준 액션 조합 + 로컬 빌드
  성공"까지. 실제 Pages 배포 성공은 사용자가 푸시 후 확인.
- 라이브 백엔드가 필요해지면(실서버 시그널 복구) `recommend()` 경로가 이미 있으므로,
  `VITE_AI_BASE` 환경변수로 fetch/정적을 분기하는 하이브리드로 확장 가능 — 현재 범위 밖.

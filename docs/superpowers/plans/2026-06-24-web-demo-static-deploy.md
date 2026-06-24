# 웹 데모 정적 배포 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** B의 AI 라인업 웹 데모를 미리 계산한 정적 응답으로 GitHub Pages에 무료 배포한다 (포트폴리오 하위 프로젝트 D).

**Architecture:** 라인업 추천 출력이 결정적이라, (픽스처 2 × 모드 2) = 4개 응답을 기존 엔진으로 미리 계산해 `dugout-web/src/responses.json`에 커밋한다. 프런트는 런타임 fetch 대신 이 JSON을 동기 룩업하고, Vite로 정적 빌드해 GitHub Actions가 Pages에 배포한다.

**Tech Stack:** Python(기존 FastAPI 엔진 재사용) / Vite + React + TS / GitHub Actions + GitHub Pages.

## Global Constraints

- 순수 정적 배포. 런타임 백엔드·CORS·fetch 없음. 응답은 미리 계산해 `src/`에 번들 import.
- `responses.json` 형태: `{ "<sample>_<mode>": LineupResponse }`, 키 4개 (`pa200_BALANCED`, `pa200_COMPETITIVE`, `pa5_BALANCED`, `pa5_COMPETITIVE`).
- 엔진 로직 재구현 **금지** — `app.services.hungarian.recommend(req)` 호출만.
- `api.ts`의 `recommend()`는 **삭제하지 않는다** (B 포트폴리오 산출물). `LineupResponse`/`Mode`/`AttendeeProfile` 타입은 계속 사용.
- Vite `base`는 빌드 시 `/Dugout/` (GitHub Pages 프로젝트 서브경로), dev는 `/`.
- 배포 워크플로: `push` to `main`, `paths: ['dugout-web/**', '.github/workflows/deploy-web.yml']`, 공식 Pages 액션, 산출물 경로 `dugout-web/dist`.
- 라우터/Redux/UI 컴포넌트 라이브러리 금지. 단일 페이지.
- 포트폴리오 산출물 — `docs/PRD.md`·`docs/TDD.md` 미변경.
- 커밋 메시지 한글 + Conventional Commits.

---

### Task 1: 라인업 응답 미리 계산 스크립트 + 결정성/정합 테스트 (dugout-ai)

**Files:**
- Create: `dugout-ai/scripts/dump_web_responses.py`
- Create: `dugout-ai/tests/test_web_responses.py`
- Create(생성물): `dugout-web/src/responses.json`

**Interfaces:**
- Consumes: `app.schemas.lineup.{AttendeeProfile, LineupRecommendRequest}`, `app.services.hungarian.recommend` (기존). 입력 픽스처는 B가 커밋한 `dugout-web/src/fixtures/{pa200,pa5}.json`.
- Produces: `dugout-web/src/responses.json` = `{ "<sample>_<mode>": <LineupRecommendResponse.model_dump()> }`, 키 4개. Task 2의 App이 이 파일을 import해 동기 룩업한다.

이 task는 TDD다. 테스트가 "커밋된 responses.json == 엔진 재실행 출력"을 단언하므로, 테스트를 먼저 쓰면 파일이 없어 RED, 스크립트로 생성하면 GREEN이 된다.

- [ ] **Step 1: 실패하는 테스트 작성**

Create `dugout-ai/tests/test_web_responses.py`:
```python
import json
from pathlib import Path

from app.schemas.lineup import AttendeeProfile, LineupRecommendRequest
from app.services import hungarian

REPO = Path(__file__).resolve().parents[2]
FIXTURES = REPO / "dugout-web" / "src" / "fixtures"
RESPONSES = REPO / "dugout-web" / "src" / "responses.json"
SAMPLES = ("pa200", "pa5")
MODES = ("BALANCED", "COMPETITIVE")


def test_committed_responses_match_engine_and_are_valid_lineups():
    committed = json.loads(RESPONSES.read_text(encoding="utf-8"))
    assert set(committed) == {f"{s}_{m}" for s in SAMPLES for m in MODES}
    for sample in SAMPLES:
        fixture = json.loads((FIXTURES / f"{sample}.json").read_text(encoding="utf-8"))
        attendees = [AttendeeProfile(**a) for a in fixture["attendees"]]
        for mode in MODES:
            req = LineupRecommendRequest(match_id=0, attendees=attendees, lineup_mode=mode)
            fresh = hungarian.recommend(req).model_dump()
            # 커밋된 정적 응답이 엔진 출력과 일치해야 정적 배포가 정당(결정성 + 비-stale)
            assert committed[f"{sample}_{mode}"] == fresh
            orders = sorted(
                e["batting_order"] for e in fresh["entries"] if e["batting_order"] is not None
            )
            assert orders == list(range(1, len(orders) + 1))
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `cd dugout-ai && source .venv/bin/activate && pytest tests/test_web_responses.py -q`
Expected: FAIL — `responses.json` 없음(`FileNotFoundError`).

- [ ] **Step 3: 미리 계산 스크립트 작성**

Create `dugout-ai/scripts/dump_web_responses.py`:
```python
"""웹 정적 배포용 라인업 응답 미리 계산 (엔진 재사용, 결정적).

사용법: python scripts/dump_web_responses.py [출력파일]
기본 출력: <repo>/dugout-web/src/responses.json
"""

import json
import sys
from pathlib import Path

from app.schemas.lineup import AttendeeProfile, LineupRecommendRequest
from app.services import hungarian

REPO = Path(__file__).resolve().parents[2]
FIXTURES_DIR = REPO / "dugout-web" / "src" / "fixtures"
SAMPLES = ("pa200", "pa5")
MODES = ("BALANCED", "COMPETITIVE")


def _response(sample: str, mode: str) -> dict:
    fixture = json.loads((FIXTURES_DIR / f"{sample}.json").read_text(encoding="utf-8"))
    attendees = [AttendeeProfile(**a) for a in fixture["attendees"]]
    req = LineupRecommendRequest(match_id=0, attendees=attendees, lineup_mode=mode)
    return hungarian.recommend(req).model_dump()


def main() -> None:
    if len(sys.argv) > 1:
        out = Path(sys.argv[1])
    else:
        out = REPO / "dugout-web" / "src" / "responses.json"
    payload = {f"{s}_{m}": _response(s, m) for s in SAMPLES for m in MODES}
    out.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"wrote {out}: {len(payload)} responses")


if __name__ == "__main__":
    main()
```

- [ ] **Step 4: 응답 생성**

Run:
```bash
cd dugout-ai && source .venv/bin/activate
python -m scripts.dump_web_responses
```
Expected: `wrote .../dugout-web/src/responses.json: 4 responses`.

- [ ] **Step 5: 테스트 통과 + 타입 확인**

Run: `cd dugout-ai && source .venv/bin/activate && pytest tests/test_web_responses.py -q && mypy app/`
Expected: 1 passed. mypy 에러 없음(스크립트는 `app/` 밖이라 mypy 대상 아님 — app 회귀만 확인).

- [ ] **Step 6: Commit**

```bash
cd /Users/heetae/Documents/Source/Dugout
git add dugout-ai/scripts/dump_web_responses.py dugout-ai/tests/test_web_responses.py dugout-web/src/responses.json
git commit -m "feat(web): 라인업 응답 4종 미리 계산 + 결정성 검증 (정적 배포)"
```

---

### Task 2: 프런트를 정적 룩업으로 전환 + Vite base 설정 (dugout-web)

**Files:**
- Modify: `dugout-web/src/App.tsx` (전체 교체)
- Modify: `dugout-web/vite.config.ts`

**Interfaces:**
- Consumes: `responses.json` (Task 1); `LineupView` (B); `api.ts`의 타입 `AttendeeProfile`/`LineupResponse`/`Mode` (B); 픽스처 `pa200.json`/`pa5.json` (B).
- Produces: fetch 없이 정적 응답을 렌더하는 단일 페이지. `npm run build`가 `base=/Dugout/`로 산출.

- [ ] **Step 1: App.tsx를 동기 룩업으로 교체**

Replace `dugout-web/src/App.tsx` 전체:
```tsx
import { useState } from "react";
import { type AttendeeProfile, type LineupResponse, type Mode } from "./api";
import { LineupView } from "./LineupView";
import pa200 from "./fixtures/pa200.json";
import pa5 from "./fixtures/pa5.json";
import responses from "./responses.json";

type Sample = "pa200" | "pa5";
type Fixture = { attendees: AttendeeProfile[]; labels: Record<string, string> };
const FIXTURES: Record<Sample, Fixture> = {
  pa200: pa200 as Fixture,
  pa5: pa5 as Fixture,
};
const RESPONSES = responses as Record<string, LineupResponse>;

export default function App() {
  const [mode, setMode] = useState<Mode>("BALANCED");
  const [sample, setSample] = useState<Sample>("pa200");
  const fixture = FIXTURES[sample];
  const response = RESPONSES[`${sample}_${mode}`];

  return (
    <main>
      <h1>AI 라인업 추천 데모</h1>
      <div className="controls">
        <label>
          모드{" "}
          <select value={mode} onChange={(e) => setMode(e.target.value as Mode)}>
            <option value="BALANCED">BALANCED</option>
            <option value="COMPETITIVE">COMPETITIVE</option>
          </select>
        </label>
        <label>
          표본{" "}
          <select value={sample} onChange={(e) => setSample(e.target.value as Sample)}>
            <option value="pa200">PA=200 (특성 또렷)</option>
            <option value="pa5">PA=5 (shrinkage 평탄화)</option>
          </select>
        </label>
      </div>
      <LineupView response={response} labels={fixture.labels} />
    </main>
  );
}
```

참고: `recommend`는 더 이상 import하지 않는다(`api.ts`엔 그대로 남김 — 미사용 export는 tsc 에러 아님). `useEffect`/로딩/에러 상태는 제거된다(4키가 모든 토글 조합을 덮어 룩업이 항상 성공).

- [ ] **Step 2: vite.config.ts에 base 추가**

Replace `dugout-web/vite.config.ts` 전체:
```ts
/// <reference types="vitest" />
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig(({ command }) => ({
  base: command === "build" ? "/Dugout/" : "/",
  plugins: [react()],
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: "./src/setupTests.ts",
    passWithNoTests: true,
  },
}));
```

- [ ] **Step 3: 타입·테스트 확인**

Run: `cd dugout-web && npx tsc --noEmit && npm test`
Expected: 타입 에러 없음(미사용 import 없음). 기존 LineupView 스모크 1 passed.

- [ ] **Step 4: 정적 빌드 + base 경로 확인**

Run:
```bash
cd dugout-web && npm run build
grep -o '/Dugout/assets/[^"]*' dist/index.html | head -1
```
Expected: build 성공. `dist/index.html`의 에셋 URL이 `/Dugout/assets/...`로 시작(서브경로 반영).

- [ ] **Step 5: Commit**

```bash
cd /Users/heetae/Documents/Source/Dugout
git add dugout-web/src/App.tsx dugout-web/vite.config.ts
git commit -m "feat(web): fetch 제거하고 미리 계산된 정적 응답 룩업 + Pages base 경로"
```

---

### Task 3: GitHub Pages 배포 워크플로 (GitHub Actions)

**Files:**
- Create: `.github/workflows/deploy-web.yml`

**Interfaces:**
- Consumes: `dugout-web/` 빌드(Task 2). `dugout-web/package-lock.json` (npm ci용, B에서 커밋됨).
- Produces: main 푸시 시 dugout-web을 빌드해 GitHub Pages에 배포하는 워크플로. (실제 그린은 사용자가 푸시·Pages 활성화 후 확인.)

- [ ] **Step 1: 워크플로 파일 작성**

Create `.github/workflows/deploy-web.yml`:
```yaml
name: Deploy web demo

on:
  push:
    branches: [main]
    paths:
      - "dugout-web/**"
      - ".github/workflows/deploy-web.yml"
  workflow_dispatch:

permissions:
  contents: read
  pages: write
  id-token: write

concurrency:
  group: pages
  cancel-in-progress: true

jobs:
  build:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: dugout-web
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: "20"
          cache: npm
          cache-dependency-path: dugout-web/package-lock.json
      - run: npm ci
      - run: npm run build
      - uses: actions/configure-pages@v5
      - uses: actions/upload-pages-artifact@v3
        with:
          path: dugout-web/dist

  deploy:
    needs: build
    runs-on: ubuntu-latest
    environment:
      name: github-pages
      url: ${{ steps.deployment.outputs.page_url }}
    steps:
      - id: deployment
        uses: actions/deploy-pages@v4
```

- [ ] **Step 2: YAML 문법 검증**

Run:
```bash
cd /Users/heetae/Documents/Source/Dugout
python3 -c "import yaml; yaml.safe_load(open('.github/workflows/deploy-web.yml')); print('yaml ok')"
```
Expected: `yaml ok` (파싱 성공). 실제 CI 실행은 푸시 후에만 확인 가능 — 이 단계는 문법·구조 게이트.

- [ ] **Step 3: Commit**

```bash
cd /Users/heetae/Documents/Source/Dugout
git add .github/workflows/deploy-web.yml
git commit -m "ci(web): GitHub Pages 배포 워크플로"
```

---

## 배포 활성화 (사용자 수동 — 코드 아님)

이 플랜 실행 후 사용자가 직접 수행해야 라이브가 된다:
1. `hit-park/Dugout` 레포를 **public**으로 전환 (무료 Pages 조건).
2. GitHub 레포 Settings → Pages → Source = **GitHub Actions**.
3. 이 작업을 main에 반영(merge/push) → `deploy-web` 워크플로 실행 → `https://hit-park.github.io/Dugout/` 라이브.

---

## Self-Review

**Spec coverage:**
- 응답 미리 계산(엔진 재사용, 4키, src 번들) → Task 1. ✓
- 프런트 fetch 제거 → 동기 룩업 → Task 2 Step 1. ✓
- api.ts recommend() 잔존, 타입은 계속 사용 → Task 2 Step 1(주석 명시, recommend import 제거하되 파일 미수정). ✓
- Vite base 빌드 시 /Dugout/, dev /  → Task 2 Step 2. ✓
- GitHub Actions Pages 워크플로(paths 필터, 공식 액션, dist 경로) → Task 3. ✓
- 에러 경로 없음(정적 룩업 항상 성공) → Task 2에서 에러 상태 제거. ✓
- 테스트: precompute 결정성/정합 + build 게이트 + 기존 스모크 유지 → Task 1 Step 1, Task 2 Step 3-4. ✓
- 사용자 수동 단계 문서화 → "배포 활성화" 섹션. ✓
- PRD/TDD 미변경 → 어떤 task도 건드리지 않음. ✓

**Placeholder scan:** TBD/TODO/"적절히" 없음. 모든 코드 step에 완전한 코드.

**Type consistency:** `responses.json` 키 포맷 `${sample}_${mode}`가 Task 1 생성(`f"{s}_{m}"`)과 Task 2 룩업(`` `${sample}_${mode}` ``)에서 일치. `RESPONSES`는 `Record<string, LineupResponse>`로 캐스팅, `LineupResponse`는 api.ts(B) 타입. `SAMPLES`/`MODES` 값(`pa200`/`pa5`, `BALANCED`/`COMPETITIVE`)이 스크립트·테스트·App 토글에서 동일.

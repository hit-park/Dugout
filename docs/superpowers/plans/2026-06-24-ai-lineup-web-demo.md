# AI 라인업 웹 데모 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 세이버매트릭스 AI 라인업 추천을 브라우저 한 화면에서 보여주는 데모 웹앱을 만든다 (포트폴리오 하위 프로젝트 B).

**Architecture:** Vite + React + TS 단일 페이지가 프리셋 합성 팀(JSON 픽스처)을 AI FastAPI(`:8001`)의 `/api/lineups/recommend`에 직결로 POST하고, 타순·포지션·이유를 렌더한다. 픽스처는 dugout-ai의 기존 합성 생성기에서 덤프해 커밋하므로 아키타입 로직 중복이 없다.

**Tech Stack:** Vite, React 18, TypeScript, vitest + @testing-library/react (프런트) / 기존 FastAPI + Pydantic (백엔드, CORS 한 줄만 추가).

## Global Constraints

- 라우터 라이브러리·Redux·UI 컴포넌트 라이브러리 **금지**. 상태는 `useState`, 패칭은 `fetch`.
- 단일 페이지. 라우팅 없음.
- 호출 대상은 AI FastAPI 직결: `http://localhost:8001/api/lineups/recommend`. Kotlin 백엔드 경유 안 함.
- 요청 본문은 항상 `{ match_id: 0, attendees, lineup_mode }`. `match_id`는 더미 `0`.
- 프리셋 팀 stat은 TS로 재구현 **금지** — dugout-ai의 Python 생성기에서 덤프한 JSON만 사용.
- CORS는 dev origin(`http://localhost:5173`)만 허용. 운영 origin은 D(배포)에서 다룸.
- 비자명 로직(응답→뷰 매핑)에 vitest 스모크 **1개**. 전체 스위트·E2E 없음.
- 포트폴리오 산출물이므로 `docs/PRD.md`·`docs/TDD.md`를 갱신하지 않는다.
- 커밋 메시지는 한글 + Conventional Commits prefix.

---

### Task 1: dugout-web 스캐폴드 (Vite react-ts + vitest)

**Files:**
- Create: `dugout-web/` (Vite 생성)
- Modify: `dugout-web/package.json`, `dugout-web/vite.config.ts`
- Create: `dugout-web/src/App.tsx` (보일러플레이트 대체), `dugout-web/src/main.tsx`(생성물 유지)
- Delete: `dugout-web/src/App.css`, `dugout-web/src/assets/react.svg`, `dugout-web/public/vite.svg`

**Interfaces:**
- Consumes: 없음
- Produces: 부팅되는 빈 단일 페이지, `npm test`가 도는 vitest 환경.

- [ ] **Step 1: Vite 프로젝트 생성**

루트(`/Users/heetae/Documents/Source/Dugout`)에서:
```bash
npm create vite@latest dugout-web -- --template react-ts
cd dugout-web && npm install
```

- [ ] **Step 2: vitest + testing-library 설치**

```bash
cd dugout-web
npm install -D vitest @testing-library/react @testing-library/jest-dom jsdom
```

- [ ] **Step 3: vite.config.ts에 vitest 설정 추가**

`dugout-web/vite.config.ts` 전체를 아래로 교체:
```ts
/// <reference types="vitest" />
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: "./src/setupTests.ts",
  },
});
```

- [ ] **Step 4: 테스트 셋업 파일 생성**

Create `dugout-web/src/setupTests.ts`:
```ts
import "@testing-library/jest-dom";
```

- [ ] **Step 5: 보일러플레이트 제거 + 빈 App**

Delete: `dugout-web/src/App.css`, `dugout-web/src/assets/react.svg`, `dugout-web/public/vite.svg`.
Replace `dugout-web/src/App.tsx` 전체:
```tsx
export default function App() {
  return <main><h1>AI 라인업 추천 데모</h1></main>;
}
```
`dugout-web/src/main.tsx`에서 `import './App.css'`가 있으면 제거(없으면 생략). `index.css` import는 유지.

- [ ] **Step 6: package.json test 스크립트 추가**

`dugout-web/package.json`의 `scripts`에 추가:
```json
"test": "vitest run"
```

- [ ] **Step 7: 부팅·테스트 환경 확인**

Run: `cd dugout-web && npx tsc --noEmit && npm test`
Expected: 타입 에러 없음. vitest는 "No test files found" 로 정상 종료(테스트 파일은 Task 5에서 추가).

- [ ] **Step 8: Commit**

```bash
cd /Users/heetae/Documents/Source/Dugout
git add dugout-web
git commit -m "chore(web): dugout-web 스캐폴드 (Vite react-ts + vitest)"
```

---

### Task 2: 합성 라인업 픽스처 덤프 스크립트 (dugout-ai)

**Files:**
- Create: `dugout-ai/scripts/dump_web_fixtures.py`
- Create(생성물): `dugout-web/src/fixtures/pa200.json`, `dugout-web/src/fixtures/pa5.json`

**Interfaces:**
- Consumes: `app.tooling.synthetic.generate`, `app.tooling.archetypes.Archetype`, `app.tooling.statline.{PlayerLine, to_attendee_profile}` (기존).
- Produces: 픽스처 JSON 2개. 각 파일 형태 `{ "attendees": AttendeeProfile[], "labels": { "<user_id>": "<archetype label>" } }`. `attendees`는 그대로 `recommend` 요청의 `attendees`로 POST된다.

- [ ] **Step 1: 덤프 스크립트 작성**

Create `dugout-ai/scripts/dump_web_fixtures.py`:
```python
"""웹 데모용 합성 라인업 픽스처 덤프 (기존 생성기 재사용, 로직 중복 없음).

사용법: python scripts/dump_web_fixtures.py [출력디렉터리]
기본 출력: <repo>/dugout-web/src/fixtures/
"""

import json
import sys
from pathlib import Path

from app.tooling import synthetic
from app.tooling.archetypes import Archetype
from app.tooling.statline import PlayerLine, to_attendee_profile

POSITIONS = ["P", "C", "1B", "2B", "3B", "SS", "LF", "CF", "RF"]
SPECS = [Archetype.OVERALL, Archetype.PURE_ONBASE, Archetype.POWER] + [Archetype.AVERAGE] * 6


def _fixture(pa: int) -> dict:
    lines = [
        PlayerLine(
            label=f"{arch.value}#{i + 1}",
            statline=synthetic.generate(arch, plate_appearances=pa, seed=100 + i + 1),
        )
        for i, arch in enumerate(SPECS)
    ]
    attendees = [
        to_attendee_profile(pl.statline, user_id=i + 1, primary_position=POSITIONS[i % len(POSITIONS)])
        for i, pl in enumerate(lines)
    ]
    return {
        "attendees": [a.model_dump() for a in attendees],
        "labels": {str(i + 1): lines[i].label for i in range(len(lines))},
    }


def main() -> None:
    if len(sys.argv) > 1:
        out_dir = Path(sys.argv[1])
    else:
        out_dir = Path(__file__).resolve().parents[2] / "dugout-web" / "src" / "fixtures"
    out_dir.mkdir(parents=True, exist_ok=True)
    for name, pa in (("pa200", 200), ("pa5", 5)):
        path = out_dir / f"{name}.json"
        path.write_text(json.dumps(_fixture(pa), ensure_ascii=False, indent=2), encoding="utf-8")
        print(f"wrote {path}")


if __name__ == "__main__":
    main()
```

- [ ] **Step 2: 픽스처 생성**

Run:
```bash
cd dugout-ai && source .venv/bin/activate
python -m scripts.dump_web_fixtures
```
Expected: `wrote .../dugout-web/src/fixtures/pa200.json` 과 `.../pa5.json` 두 줄 출력.

- [ ] **Step 3: 픽스처 내용 검증**

Run:
```bash
cd /Users/heetae/Documents/Source/Dugout
python3 -c "import json; d=json.load(open('dugout-web/src/fixtures/pa200.json')); assert len(d['attendees'])==9, d; assert d['labels']['1']=='OVERALL#1', d['labels']; assert all('singles' in a for a in d['attendees']); print('pa200 ok')"
python3 -c "import json; d=json.load(open('dugout-web/src/fixtures/pa5.json')); assert len(d['attendees'])==9; print('pa5 ok')"
```
Expected: `pa200 ok` / `pa5 ok`.

- [ ] **Step 4: Commit**

```bash
cd /Users/heetae/Documents/Source/Dugout
git add dugout-ai/scripts/dump_web_fixtures.py dugout-web/src/fixtures
git commit -m "feat(web): 합성 라인업 픽스처 덤프 스크립트 + PA=200/5 픽스처"
```

---

### Task 3: AI 서비스 CORS 활성화 (dugout-ai)

**Files:**
- Modify: `dugout-ai/app/main.py`

**Interfaces:**
- Consumes: 없음
- Produces: 브라우저(`http://localhost:5173`)에서 POST 가능한 `/api/lineups/recommend`.

- [ ] **Step 1: CORS 미들웨어 추가**

`dugout-ai/app/main.py`의 import 블록에 추가:
```python
from fastapi.middleware.cors import CORSMiddleware
```
`app = FastAPI(...)` 정의 직후, exception handler 등록 줄들 위에 추가:
```python
# ponytail: dev origin만 허용. 운영 origin은 배포(D)에서 환경변수로 확장.
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:5173"],
    allow_methods=["POST"],
    allow_headers=["Content-Type"],
)
```

- [ ] **Step 2: 기존 테스트·타입 회귀 확인**

Run: `cd dugout-ai && source .venv/bin/activate && pytest -q && mypy app/`
Expected: 기존 테스트 전부 통과, mypy 에러 없음.

- [ ] **Step 3: CORS 프리플라이트 동작 확인**

Run (별도 터미널에서 `uvicorn app.main:app --port 8001` 띄운 뒤):
```bash
curl -s -i -X OPTIONS http://localhost:8001/api/lineups/recommend \
  -H "Origin: http://localhost:5173" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: Content-Type" | grep -i "access-control-allow-origin"
```
Expected: `access-control-allow-origin: http://localhost:5173` 헤더가 보임.

- [ ] **Step 4: Commit**

```bash
cd /Users/heetae/Documents/Source/Dugout
git add dugout-ai/app/main.py
git commit -m "feat(ai): 웹 데모용 CORS 미들웨어 (dev origin)"
```

---

### Task 4: API 클라이언트 + 타입 (api.ts)

**Files:**
- Create: `dugout-web/src/api.ts`

**Interfaces:**
- Consumes: 없음
- Produces:
  - 타입 `AttendeeProfile`, `LineupAssignment`, `LineupResponse`, `Mode`(`"BALANCED" | "COMPETITIVE"`).
  - `recommend(attendees: AttendeeProfile[], mode: Mode): Promise<LineupResponse>` — 실패 시 서버 `message` 또는 `요청 실패 (HTTP n)` 문구로 `Error` throw.

- [ ] **Step 1: api.ts 작성**

Create `dugout-web/src/api.ts`:
```ts
export interface AttendeeProfile {
  user_id: number;
  primary_position: string;
  sub_positions: string[];
  bench_ratio_recent: number;
  bats_left: boolean;
  singles: number;
  doubles: number;
  triples: number;
  home_runs: number;
  walks: number;
  hit_by_pitch: number;
  sacrifice_flies: number;
  strikeouts: number;
  in_play_outs: number;
  reached_on_errors: number;
}

export interface LineupAssignment {
  user_id: number;
  position: string;
  batting_order: number | null;
  is_bench: boolean;
  reason: string | null;
}

export interface LineupResponse {
  match_id: number;
  is_ai_generated: boolean;
  source: string;
  fairness_note: string | null;
  entries: LineupAssignment[];
}

export type Mode = "BALANCED" | "COMPETITIVE";

const AI_BASE = import.meta.env.VITE_AI_BASE ?? "http://localhost:8001";

export async function recommend(attendees: AttendeeProfile[], mode: Mode): Promise<LineupResponse> {
  const res = await fetch(`${AI_BASE}/api/lineups/recommend`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ match_id: 0, attendees, lineup_mode: mode }),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => null);
    throw new Error(err?.message ?? `요청 실패 (HTTP ${res.status})`);
  }
  return res.json();
}
```

- [ ] **Step 2: 타입 확인**

Run: `cd dugout-web && npx tsc --noEmit`
Expected: 에러 없음.

- [ ] **Step 3: Commit**

```bash
cd /Users/heetae/Documents/Source/Dugout
git add dugout-web/src/api.ts
git commit -m "feat(web): AI 라인업 추천 API 클라이언트 + 타입"
```

---

### Task 5: LineupView 컴포넌트 + 스모크 테스트 (TDD)

**Files:**
- Create: `dugout-web/src/LineupView.tsx`
- Test: `dugout-web/src/LineupView.test.tsx`

**Interfaces:**
- Consumes: `LineupResponse` (Task 4).
- Produces: `LineupView(props: { response: LineupResponse; labels: Record<string, string> }): JSX.Element` — `batting_order`가 있는 entry를 타순 오름차순 리스트로(이름·포지션·reason), 그리고 9포지션 다이아몬드를 렌더. `fairness_note`가 있으면 표시.

- [ ] **Step 1: 실패하는 스모크 테스트 작성**

Create `dugout-web/src/LineupView.test.tsx`:
```tsx
import { render, screen } from "@testing-library/react";
import { LineupView } from "./LineupView";
import type { LineupResponse } from "./api";

test("타순을 오름차순으로, reason·공정성 노트와 함께 렌더한다", () => {
  const response: LineupResponse = {
    match_id: 0,
    is_ai_generated: true,
    source: "AI",
    fairness_note: "공정성 노트",
    entries: [
      { user_id: 1, position: "C", batting_order: 2, is_bench: false, reason: "종합 최고타자" },
      { user_id: 2, position: "P", batting_order: 1, is_bench: false, reason: "순수 출루형" },
    ],
  };
  render(<LineupView response={response} labels={{ "1": "OVERALL#1", "2": "PURE_ONBASE#2" }} />);

  const items = screen.getAllByRole("listitem");
  expect(items[0]).toHaveTextContent("PURE_ONBASE#2"); // 1번이 먼저
  expect(items[0]).toHaveTextContent("순수 출루형");
  expect(items[1]).toHaveTextContent("OVERALL#1");
  expect(screen.getByText("공정성 노트")).toBeInTheDocument();
});
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `cd dugout-web && npm test`
Expected: FAIL — `LineupView` 모듈 없음(import 에러).

- [ ] **Step 3: LineupView 구현**

Create `dugout-web/src/LineupView.tsx`:
```tsx
import type { LineupResponse } from "./api";

const POSITIONS = ["P", "C", "1B", "2B", "3B", "SS", "LF", "CF", "RF"];

export function LineupView({
  response,
  labels,
}: {
  response: LineupResponse;
  labels: Record<string, string>;
}) {
  const name = (uid: number) => labels[String(uid)] ?? `#${uid}`;
  const batters = response.entries
    .filter((e) => e.batting_order != null)
    .sort((a, b) => (a.batting_order as number) - (b.batting_order as number));
  const posByUser = new Map(response.entries.map((e) => [e.user_id, e.position]));
  const userByPos = new Map(response.entries.map((e) => [e.position, e.user_id]));

  return (
    <div className="result">
      {response.fairness_note && <p className="fairness">{response.fairness_note}</p>}
      <ol className="lineup">
        {batters.map((e) => (
          <li key={e.user_id}>
            <span className="order">{e.batting_order}</span>
            <span className="name">{name(e.user_id)}</span>
            <span className="pos">{posByUser.get(e.user_id)}</span>
            <span className="reason">{e.reason}</span>
          </li>
        ))}
      </ol>
      <div className="diamond">
        {POSITIONS.map((pos) => {
          const uid = userByPos.get(pos);
          return (
            <div key={pos} className={`slot slot-${pos}`}>
              <strong>{pos}</strong>
              {uid != null && <span>{name(uid)}</span>}
            </div>
          );
        })}
      </div>
    </div>
  );
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `cd dugout-web && npm test`
Expected: PASS (1 test).

- [ ] **Step 5: Commit**

```bash
cd /Users/heetae/Documents/Source/Dugout
git add dugout-web/src/LineupView.tsx dugout-web/src/LineupView.test.tsx
git commit -m "feat(web): 라인업 결과 뷰(타순·다이아몬드) + 스모크 테스트"
```

---

### Task 6: App 조립 (토글 + fetch + 픽스처) + 다이아몬드 CSS

**Files:**
- Modify: `dugout-web/src/App.tsx`
- Create: `dugout-web/src/index.css` (생성물 있으면 교체)

**Interfaces:**
- Consumes: `recommend`, `Mode`, `LineupResponse` (Task 4); `LineupView` (Task 5); `pa200.json`/`pa5.json` (Task 2).
- Produces: 모드·표본 토글 → fetch → 결과 렌더하는 완성 데모 페이지.

- [ ] **Step 1: App.tsx 구현**

Replace `dugout-web/src/App.tsx` 전체:
```tsx
import { useEffect, useState } from "react";
import { recommend, type AttendeeProfile, type LineupResponse, type Mode } from "./api";
import { LineupView } from "./LineupView";
import pa200 from "./fixtures/pa200.json";
import pa5 from "./fixtures/pa5.json";

type Sample = "pa200" | "pa5";
type Fixture = { attendees: AttendeeProfile[]; labels: Record<string, string> };
const FIXTURES: Record<Sample, Fixture> = {
  pa200: pa200 as Fixture,
  pa5: pa5 as Fixture,
};

export default function App() {
  const [mode, setMode] = useState<Mode>("BALANCED");
  const [sample, setSample] = useState<Sample>("pa200");
  const [data, setData] = useState<LineupResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const fixture = FIXTURES[sample];

  useEffect(() => {
    let cancelled = false;
    setError(null);
    recommend(fixture.attendees, mode)
      .then((r) => !cancelled && setData(r))
      .catch((e: Error) => !cancelled && setError(e.message));
    return () => {
      cancelled = true;
    };
  }, [mode, sample, fixture.attendees]);

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
      {error && <p className="error">{error}</p>}
      {data && <LineupView response={data} labels={fixture.labels} />}
    </main>
  );
}
```

- [ ] **Step 2: 다이아몬드 CSS 작성**

Replace `dugout-web/src/index.css` 전체:
```css
body { font-family: system-ui, sans-serif; margin: 2rem; color: #1a1a1a; }
.controls { display: flex; gap: 1.5rem; margin: 1rem 0; }
.error { color: #c0392b; }
.fairness { color: #555; font-style: italic; }
.lineup { display: grid; gap: 0.25rem; padding-left: 0; list-style: none; }
.lineup li { display: grid; grid-template-columns: 1.5rem 8rem 2.5rem 1fr; gap: 0.5rem; align-items: baseline; }
.lineup .order { font-weight: 700; }
.lineup .pos { color: #2980b9; }
.lineup .reason { color: #555; font-size: 0.9rem; }

.diamond {
  position: relative;
  width: 320px;
  height: 320px;
  margin: 2rem 0;
  background: #2e7d32;
  border-radius: 8px;
}
.slot {
  position: absolute;
  transform: translate(-50%, -50%);
  text-align: center;
  color: #fff;
  font-size: 0.8rem;
  line-height: 1.1;
}
.slot strong { display: block; }
.slot-P  { left: 50%; top: 62%; }
.slot-C  { left: 50%; top: 92%; }
.slot-1B { left: 78%; top: 60%; }
.slot-2B { left: 64%; top: 40%; }
.slot-3B { left: 22%; top: 60%; }
.slot-SS { left: 36%; top: 40%; }
.slot-LF { left: 18%; top: 18%; }
.slot-CF { left: 50%; top: 8%; }
.slot-RF { left: 82%; top: 18%; }
```

- [ ] **Step 3: 타입·테스트 회귀 확인**

Run: `cd dugout-web && npx tsc --noEmit && npm test`
Expected: 타입 에러 없음, 스모크 테스트 통과.

- [ ] **Step 4: 엔드투엔드 수동 확인**

터미널 1: `cd dugout-ai && source .venv/bin/activate && uvicorn app.main:app --reload --port 8001`
터미널 2: `cd dugout-web && npm run dev`
브라우저에서 `http://localhost:5173` 열기. 확인 항목:
- PA=200에서 2번 슬롯이 OVERALL, 1번이 PURE_ONBASE, 4번이 POWER 근처로 배치되고 reason 문자열이 보인다.
- 표본을 PA=5로 토글하면 라인업이 평탄화된다(슬로팅 변화).
- 모드를 COMPETITIVE로 바꾸면 재요청된다.
- 다이아몬드에 9개 포지션 라벨과 이름이 표시된다.

- [ ] **Step 5: Commit**

```bash
cd /Users/heetae/Documents/Source/Dugout
git add dugout-web/src/App.tsx dugout-web/src/index.css
git commit -m "feat(web): 토글·fetch·다이아몬드로 라인업 데모 페이지 완성"
```

---

## Self-Review

**Spec coverage:**
- 위치/스택(Vite+React+TS, 단일 페이지) → Task 1. ✓
- 데이터(Python 생성기 덤프, PA=200/5 픽스처, 로직 중복 0) → Task 2. ✓
- AI 직결 + CORS 한 줄 → Task 3(CORS), Task 4(직결 fetch). ✓
- 토글 2개(모드·표본) → Task 6. ✓
- 타순 리스트 + reason + 공정성 노트 + CSS 다이아몬드 → Task 5, Task 6. ✓
- 에러 핸들링(fetch 경계, message 표시) → Task 4(throw), Task 6(렌더). ✓
- vitest 스모크 1개 → Task 5. ✓
- PRD/TDD 미갱신 → 어떤 task도 docs/PRD·TDD를 건드리지 않음. ✓

**Placeholder scan:** TBD/TODO/"적절히 처리" 없음. 모든 코드 step에 완전한 코드 포함.

**Type consistency:** `recommend(attendees, mode)`·`LineupResponse`·`Mode`·`AttendeeProfile`가 Task 4 정의와 Task 5·6 사용처에서 일치. `labels`는 전 구간 `Record<string,string>`(문자열 키)로 통일. 픽스처 `labels` 키도 `str(user_id)`로 생성(Task 2)되어 정합.

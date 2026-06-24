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
    setData(null);
    recommend(fixture.attendees, mode)
      .then((r) => !cancelled && setData(r))
      .catch((e: unknown) => {
        if (!cancelled) setError(e instanceof Error ? e.message : String(e));
      });
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

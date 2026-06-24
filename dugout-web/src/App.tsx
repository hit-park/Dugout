import { useState } from "react";
import { type AttendeeProfile, type LineupResponse } from "./api";
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
const RESPONSES = responses as Record<Sample, LineupResponse>;

export default function App() {
  const [sample, setSample] = useState<Sample>("pa200");
  const fixture = FIXTURES[sample];
  const response = RESPONSES[sample];

  return (
    <main>
      <h1>AI 라인업 추천 데모</h1>
      <div className="controls">
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

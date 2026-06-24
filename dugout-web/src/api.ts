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

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
  const userByPos = new Map(response.entries.map((e) => [e.position, e.user_id]));

  return (
    <div className="result">
      {response.fairness_note && <p className="fairness">{response.fairness_note}</p>}
      <ol className="lineup">
        {batters.map((e) => (
          <li key={e.user_id}>
            <span className="order">{e.batting_order}</span>
            <span className="name">{name(e.user_id)}</span>
            <span className="pos">{e.position}</span>
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

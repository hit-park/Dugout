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

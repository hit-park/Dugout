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

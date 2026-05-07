from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def _attendees(n: int = 10):
    positions = ["P", "C", "1B", "2B", "3B", "SS", "LF", "CF", "RF", "DH"]
    return [
        {
            "user_id": i + 1,
            "primary_position": positions[i % len(positions)],
            "sub_positions": [],
            "bench_ratio_recent": 0.0,
            "bats_left": i % 3 == 0,
        }
        for i in range(n)
    ]


def test_recommend_assigns_9_field_positions():
    payload = {"match_id": 100, "attendees": _attendees(10), "lineup_mode": "BALANCED"}
    res = client.post("/api/lineups/recommend", json=payload)
    assert res.status_code == 200
    body = res.json()
    starters = [e for e in body["entries"] if not e["is_bench"]]
    assert len(starters) == 9
    starter_positions = {e["position"] for e in starters}
    assert starter_positions == {"P", "C", "1B", "2B", "3B", "SS", "LF", "CF", "RF"}


def test_recommend_returns_422_on_too_few_attendees():
    payload = {"match_id": 100, "attendees": _attendees(5)}
    res = client.post("/api/lineups/recommend", json=payload)
    # Pydantic min_length 검증으로 422
    assert res.status_code == 422


def test_recommend_bench_for_extras():
    payload = {"match_id": 100, "attendees": _attendees(11)}
    res = client.post("/api/lineups/recommend", json=payload)
    assert res.status_code == 200
    body = res.json()
    benches = [e for e in body["entries"] if e["is_bench"]]
    assert len(benches) == 2  # 11 - 9 = 2


def test_recommend_batting_order_uses_1_to_9():
    payload = {"match_id": 100, "attendees": _attendees(9)}
    res = client.post("/api/lineups/recommend", json=payload)
    assert res.status_code == 200
    body = res.json()
    starters = [e for e in body["entries"] if not e["is_bench"]]
    orders = sorted(e["batting_order"] for e in starters)
    assert orders == list(range(1, 10))

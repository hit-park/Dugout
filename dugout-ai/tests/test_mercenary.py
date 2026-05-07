from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def _candidate(user_id: int, nickname: str, positions, regions, rating=4.0):
    return {
        "user_id": user_id,
        "nickname": nickname,
        "positions": positions,
        "regions": regions,
        "available_days": ["SAT", "SUN"],
        "rating": rating,
        "total_games": 10,
    }


def test_recommend_filters_by_position_and_region():
    payload = {
        "request_id": 1,
        "needed_positions": ["P", "C"],
        "needed_regions": ["서울 강남"],
        "available_days": [],
        "candidates": [
            _candidate(10, "에이스", ["P"], ["서울 강남"], rating=4.5),
            _candidate(11, "포수", ["C"], ["서울 강남"], rating=3.0),
            _candidate(12, "외야", ["RF"], ["서울 강남"], rating=5.0),  # 포지션 불일치
            _candidate(13, "지방러", ["P"], ["부산"], rating=4.0),       # 지역 불일치
        ],
    }
    res = client.post("/api/mercenary/recommend", json=payload)
    assert res.status_code == 200
    body = res.json()
    user_ids = [m["user_id"] for m in body["matches"]]
    assert user_ids == [10, 11]  # rating 순


def test_recommend_no_candidates_returns_empty():
    payload = {
        "request_id": 1,
        "needed_positions": ["P"],
        "needed_regions": [],
        "available_days": [],
        "candidates": [],
    }
    res = client.post("/api/mercenary/recommend", json=payload)
    assert res.status_code == 200
    assert res.json()["matches"] == []


def test_recommend_sorted_by_score_desc():
    payload = {
        "request_id": 1,
        "needed_positions": ["P"],
        "needed_regions": [],
        "available_days": [],
        "candidates": [
            _candidate(10, "낮은평점", ["P"], [], rating=2.0),
            _candidate(11, "높은평점", ["P"], [], rating=5.0),
        ],
    }
    res = client.post("/api/mercenary/recommend", json=payload)
    assert res.status_code == 200
    matches = res.json()["matches"]
    assert matches[0]["user_id"] == 11
    assert matches[1]["user_id"] == 10
    assert matches[0]["score"] > matches[1]["score"]

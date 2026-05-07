from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_score_perfect_match():
    payload = {
        "home_elo": 1200,
        "away_elo": 1200,
        "distance_km": 3.0,
        "time_overlap_ratio": 1.0,
        "away_manner_score": 5.0,
    }
    res = client.post("/api/matching/score", json=payload)
    assert res.status_code == 200
    body = res.json()
    assert body["total_score"] == 100.0
    assert body["breakdown"]["skill"] == 100.0
    assert body["breakdown"]["distance"] == 100.0
    assert body["breakdown"]["time"] == 100.0
    assert body["breakdown"]["manner"] == 100.0


def test_score_far_distance_drops():
    payload = {
        "home_elo": 1200,
        "away_elo": 1200,
        "distance_km": 50.0,
        "time_overlap_ratio": 1.0,
        "away_manner_score": 5.0,
    }
    res = client.post("/api/matching/score", json=payload)
    assert res.status_code == 200
    body = res.json()
    # 50km는 30km 이후 단계 — 기본 50 - 20 = 30
    assert body["breakdown"]["distance"] == 30.0


def test_score_large_elo_gap_drops_skill():
    payload = {
        "home_elo": 1600,
        "away_elo": 1100,
        "distance_km": 0.0,
        "time_overlap_ratio": 0.5,
        "away_manner_score": 3.0,
    }
    res = client.post("/api/matching/score", json=payload)
    assert res.status_code == 200
    body = res.json()
    # diff 500 → max(0, 40 - (500-200)*0.2) = max(0, 40-60) = 0
    assert body["breakdown"]["skill"] == 0.0


def test_score_validation_error_on_invalid_overlap():
    payload = {
        "home_elo": 1200,
        "away_elo": 1200,
        "distance_km": 3.0,
        "time_overlap_ratio": 1.5,  # 0~1 초과
        "away_manner_score": 5.0,
    }
    res = client.post("/api/matching/score", json=payload)
    assert res.status_code == 422

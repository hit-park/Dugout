from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_predict_attend_when_high_recent_rate():
    payload = {
        "user_id": 1,
        "match_id": 100,
        "day_of_week_rate": 0.9,
        "recent_5_attendance_rate": 0.9,
        "consecutive_absent_count": 0,
        "distance_km": 5.0,
        "has_bad_weather": False,
        "avg_response_lag_hours": 1.0,
    }
    res = client.post("/api/attendance/predict", json=payload)
    assert res.status_code == 200
    body = res.json()
    assert body["prediction"] == "ATTEND"
    assert body["probability"] >= 0.7
    assert body["confidence"] > 0.0


def test_predict_absent_when_consecutive_absent_and_far():
    payload = {
        "user_id": 1,
        "match_id": 100,
        "day_of_week_rate": 0.2,
        "recent_5_attendance_rate": 0.2,
        "consecutive_absent_count": 4,
        "distance_km": 80.0,
        "has_bad_weather": True,
    }
    res = client.post("/api/attendance/predict", json=payload)
    assert res.status_code == 200
    body = res.json()
    assert body["prediction"] == "ABSENT"
    assert body["probability"] < 0.3


def test_predict_validation_error_on_invalid_rate():
    payload = {
        "user_id": 1,
        "match_id": 100,
        "day_of_week_rate": 1.5,  # 0~1 초과
        "recent_5_attendance_rate": 0.5,
        "consecutive_absent_count": 0,
        "distance_km": 0.0,
    }
    res = client.post("/api/attendance/predict", json=payload)
    assert res.status_code == 422

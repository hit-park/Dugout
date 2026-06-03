from fastapi.testclient import TestClient

from app.main import app
from app.schemas.lineup import AttendeeProfile
from app.services import batting_order

_client = TestClient(app)


def _player(uid: int, **counts) -> AttendeeProfile:
    return AttendeeProfile(user_id=uid, primary_position="DH", **counts)


def test_components_basic():
    # 단타1 2루타1 볼넷1 삼진1 → PA4 AB3 H2 OBP .750 SLG 1.000
    p = _player(1, singles=1, doubles=1, walks=1, strikeouts=1)
    c = batting_order._components(p)
    assert c.pa == 4
    assert c.ab == 3
    assert c.hits == 2
    assert abs(c.on_base - 3) < 1e-9          # 2안타 + 1볼넷
    assert abs(c.obp_denom - 4) < 1e-9        # AB3 + BB1
    assert c.total_bases == 3                 # 1 + 2


def test_has_records_false_when_all_zero():
    assert batting_order.has_records([_player(1), _player(2)]) is False


def test_has_records_true_when_any_pa():
    assert batting_order.has_records([_player(1), _player(2, singles=1)]) is True


def test_shrinkage_pulls_small_sample_toward_team_mean():
    # 5타석 .800 OBP 선수 vs 팀평균 .300 → 보정 OBP가 팀평균 쪽으로 크게 수축
    small = _player(1, singles=4, in_play_outs=1)   # 5타석 4안타 → raw OBP .800
    team_obp, team_iso = 0.300, 0.100
    adj_obp, _ = batting_order._adjusted(small, team_obp, team_iso)
    assert adj_obp < 0.500                          # k=50이라 5타석은 평균 쪽으로 강하게 수축
    assert adj_obp > team_obp                        # 그래도 평균보다는 높음


def test_order_puts_best_overall_at_second_and_power_at_fourth():
    # 9명: 한 명은 종합 최고(고출루+고장타), 한 명은 순수 장타, 한 명은 순수 출루
    best = _player(1, singles=40, doubles=20, home_runs=20, walks=40)   # 고OBP+고ISO
    power = _player(2, home_runs=40, strikeouts=60)                     # 고ISO 저OBP
    onbase = _player(3, singles=30, walks=60, in_play_outs=30)          # 고OBP 저ISO
    fillers = [_player(i, singles=10, in_play_outs=40) for i in range(4, 10)]
    result = batting_order.order([best, power, onbase, *fillers])

    assert result is not None
    assert result[1] == 2          # 종합 최고타자 → 2번 (The Book 반전)
    assert result[2] == 4          # 순수 장타 → 4번
    assert result[3] == 1          # 순수 출루 → 1번


def test_order_returns_none_on_cold_start():
    cold = [_player(i) for i in range(1, 10)]   # 전원 기록 0
    assert batting_order.order(cold) is None


def test_reached_on_error_counts_as_out_not_on_base():
    # 실책출루는 AB에 포함(아웃 취급), on_base에는 미포함 — 이 기능의 핵심 도메인 결정
    p = _player(1, reached_on_errors=1)
    c = batting_order._components(p)
    assert c.pa == 1
    assert c.ab == 1            # ROE는 타수에 포함
    assert c.on_base == 0       # 출루로 치지 않음
    assert c.hits == 0


def test_order_assigns_exactly_nine_slots_1_to_9():
    starters = [_player(i, singles=10, in_play_outs=10 + i) for i in range(1, 10)]
    result = batting_order.order(starters)
    assert result is not None
    assert sorted(result.values()) == list(range(1, 10))


def test_recommend_uses_sabermetric_order_when_records_present():
    positions = ["P", "C", "1B", "2B", "3B", "SS", "LF", "CF", "RF"]
    attendees = [
        {
            "user_id": i + 1,
            "primary_position": positions[i],
            "sub_positions": [],
            "bench_ratio_recent": 0.0,
            "bats_left": False,
            "singles": 10,
            "in_play_outs": 40,
        }
        for i in range(9)
    ]
    # user_id 1을 종합 최고타자로 (고OBP + 고ISO)
    attendees[0].update({"singles": 40, "doubles": 20, "home_runs": 20, "walks": 40, "in_play_outs": 0})

    res = _client.post(
        "/api/lineups/recommend",
        json={"match_id": 1, "attendees": attendees, "lineup_mode": "COMPETITIVE"},
    )
    assert res.status_code == 200
    entries = {e["user_id"]: e["batting_order"] for e in res.json()["entries"] if not e["is_bench"]}
    assert entries[1] == 2  # 종합 최고타자 → 2번 (The Book 반전)


# ─── reason 문자열 테스트 ────────────────────────────────────────────────────


def test_reasons_returns_none_on_cold_start():
    cold = [_player(i) for i in range(1, 10)]
    assert batting_order.reasons(cold) is None


def test_reasons_slot2_contains_2번():
    best = _player(1, singles=40, doubles=20, home_runs=20, walks=40)
    power = _player(2, home_runs=40, strikeouts=60)
    onbase = _player(3, singles=30, walks=60, in_play_outs=30)
    fillers = [_player(i, singles=10, in_play_outs=40) for i in range(4, 10)]
    starters = [best, power, onbase, *fillers]

    result = batting_order.reasons(starters)

    assert result is not None
    # 종합 최고타자(uid=1)는 2번 배치 → reason에 "2번" 포함
    assert result[1] is not None
    assert "2번" in result[1]


def test_reasons_all_starters_have_reason_when_records_present():
    starters = [_player(i, singles=10, in_play_outs=10 + i) for i in range(1, 10)]
    result = batting_order.reasons(starters)
    assert result is not None
    assert len(result) == 9
    for _uid, reason in result.items():
        assert reason is not None and len(reason) > 0


def test_reasons_format_uses_baseball_notation():
    # 소수점 3자리, 정수부 0 제거 포맷 검증 (.xxx 형태)
    starters = [_player(i, singles=10, in_play_outs=10 + i) for i in range(1, 10)]
    result = batting_order.reasons(starters)
    assert result is not None
    for reason in result.values():
        # 출루율·ISO 포함 슬롯의 포맷 검증: .<3 digits> 패턴이 있거나 순수 텍스트 슬롯(3번, 5번)이어야 함
        if "출루율" in reason or "ISO" in reason or "출루 ." in reason:
            import re
            assert re.search(r"\.\d{3}", reason), f"baseball notation missing in: {reason}"


def test_reasons_via_recommend_endpoint_slot2_has_reason():
    """라우터 통합: 기록 있는 선수의 2번 슬롯 entry에 reason이 non-None이고 '2번' 포함."""
    positions = ["P", "C", "1B", "2B", "3B", "SS", "LF", "CF", "RF"]
    attendees = [
        {
            "user_id": i + 1,
            "primary_position": positions[i],
            "sub_positions": [],
            "bench_ratio_recent": 0.0,
            "bats_left": False,
            "singles": 10,
            "in_play_outs": 40,
        }
        for i in range(9)
    ]
    # user_id 1을 종합 최고타자로
    attendees[0].update({"singles": 40, "doubles": 20, "home_runs": 20, "walks": 40, "in_play_outs": 0})

    res = _client.post(
        "/api/lineups/recommend",
        json={"match_id": 2, "attendees": attendees, "lineup_mode": "BALANCED"},
    )
    assert res.status_code == 200
    entries = res.json()["entries"]
    slot2_entry = next(e for e in entries if e["batting_order"] == 2)
    assert slot2_entry["reason"] is not None
    assert "2번" in slot2_entry["reason"]


def test_cold_start_entries_have_no_reason():
    """기록 없는 선수 라인업은 모든 선발 entry의 reason이 None."""
    positions = ["P", "C", "1B", "2B", "3B", "SS", "LF", "CF", "RF"]
    attendees = [
        {
            "user_id": i + 1,
            "primary_position": positions[i],
            "sub_positions": [],
            "bench_ratio_recent": 0.0,
            "bats_left": i % 2 == 0,
            # 타석 기록 없음 → 콜드 스타트
        }
        for i in range(9)
    ]

    res = _client.post(
        "/api/lineups/recommend",
        json={"match_id": 3, "attendees": attendees, "lineup_mode": "BALANCED"},
    )
    assert res.status_code == 200
    starters = [e for e in res.json()["entries"] if not e["is_bench"]]
    for entry in starters:
        assert entry["reason"] is None

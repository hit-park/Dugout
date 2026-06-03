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

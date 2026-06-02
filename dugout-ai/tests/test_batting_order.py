from app.schemas.lineup import AttendeeProfile
from app.services import batting_order


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

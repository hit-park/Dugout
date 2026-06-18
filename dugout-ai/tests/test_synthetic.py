import pytest

from app.schemas.lineup import AttendeeProfile
from app.services import batting_order
from app.tooling import synthetic
from app.tooling.archetypes import OUTCOMES, Archetype, weights
from app.tooling.statline import PlayerLine, StatLine, to_attendee_profile


def test_statline_defaults_are_zero():
    line = StatLine()
    assert line.singles == 0
    assert line.reached_on_errors == 0


def test_to_attendee_profile_maps_all_counts():
    line = StatLine(
        singles=3, doubles=1, triples=2, home_runs=5,
        walks=7, hit_by_pitch=1, sacrifice_flies=4,
        strikeouts=9, in_play_outs=6, reached_on_errors=8,
    )
    profile = to_attendee_profile(line, user_id=7, primary_position="SS")
    assert isinstance(profile, AttendeeProfile)
    assert profile.user_id == 7
    assert profile.primary_position == "SS"
    assert profile.sub_positions == []
    assert profile.singles == 3
    assert profile.doubles == 1
    assert profile.triples == 2
    assert profile.home_runs == 5
    assert profile.walks == 7
    assert profile.hit_by_pitch == 1
    assert profile.sacrifice_flies == 4
    assert profile.strikeouts == 9
    assert profile.in_play_outs == 6
    assert profile.reached_on_errors == 8


def test_player_line_holds_label_and_statline():
    pl = PlayerLine(label="박**(61)", statline=StatLine(singles=2))
    assert pl.label == "박**(61)"
    assert pl.statline.singles == 2


def test_outcomes_match_statline_fields():
    line = StatLine()
    for outcome in OUTCOMES:
        assert hasattr(line, outcome), f"{outcome} is not a StatLine field"
    assert len(OUTCOMES) == 10


def test_every_archetype_has_weight_for_every_outcome():
    for archetype in Archetype:
        w = weights(archetype)
        assert set(w.keys()) == set(OUTCOMES)
        assert all(v >= 0 for v in w.values())
        assert sum(w.values()) > 0


def test_generate_is_deterministic_for_same_seed():
    a = synthetic.generate(Archetype.OVERALL, plate_appearances=100, seed=7)
    b = synthetic.generate(Archetype.OVERALL, plate_appearances=100, seed=7)
    assert a == b


def test_generate_total_outcomes_equals_pa():
    line = synthetic.generate(Archetype.POWER, plate_appearances=120, seed=3)
    total = (
        line.singles + line.doubles + line.triples + line.home_runs
        + line.walks + line.hit_by_pitch + line.sacrifice_flies
        + line.strikeouts + line.in_play_outs + line.reached_on_errors
    )
    assert total == 120


def test_generate_zero_pa_is_empty():
    assert synthetic.generate(Archetype.AVERAGE, plate_appearances=0, seed=1) == StatLine()


def test_generate_negative_pa_raises():
    with pytest.raises(ValueError):
        synthetic.generate(Archetype.AVERAGE, plate_appearances=-1, seed=1)


def test_archetypes_separate_in_expected_directions_at_large_sample():
    pa = 2000
    overall = to_attendee_profile(synthetic.generate(Archetype.OVERALL, plate_appearances=pa, seed=1), user_id=1)
    onbase = to_attendee_profile(synthetic.generate(Archetype.PURE_ONBASE, plate_appearances=pa, seed=2), user_id=2)
    power = to_attendee_profile(synthetic.generate(Archetype.POWER, plate_appearances=pa, seed=3), user_id=3)
    average = to_attendee_profile(synthetic.generate(Archetype.AVERAGE, plate_appearances=pa, seed=4), user_id=4)

    team = [overall, onbase, power, average]
    team_obp, team_iso = batting_order._team_averages(team)
    obp: dict[int, float] = {}
    iso: dict[int, float] = {}
    for p in team:
        o, i = batting_order._adjusted(p, team_obp, team_iso)
        obp[p.user_id] = o
        iso[p.user_id] = i

    assert obp[2] > obp[3]
    assert iso[3] > iso[2]
    overall_score = {uid: obp[uid] + 0.5 * iso[uid] for uid in obp}
    assert overall_score[1] == max(overall_score.values())

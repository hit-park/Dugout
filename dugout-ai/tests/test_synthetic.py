from app.schemas.lineup import AttendeeProfile
from app.tooling.archetypes import OUTCOMES, Archetype, weights
from app.tooling.statline import PlayerLine, StatLine, to_attendee_profile


def test_statline_defaults_are_zero():
    line = StatLine()
    assert line.singles == 0
    assert line.reached_on_errors == 0


def test_to_attendee_profile_maps_all_counts():
    line = StatLine(singles=3, doubles=1, walks=2, strikeouts=4, in_play_outs=5)
    profile = to_attendee_profile(line, user_id=7, primary_position="SS")
    assert isinstance(profile, AttendeeProfile)
    assert profile.user_id == 7
    assert profile.primary_position == "SS"
    assert profile.singles == 3
    assert profile.doubles == 1
    assert profile.walks == 2
    assert profile.strikeouts == 4
    assert profile.in_play_outs == 5
    assert profile.sub_positions == []


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

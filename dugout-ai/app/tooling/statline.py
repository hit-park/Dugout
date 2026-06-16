"""합성 생성기와 gameone 크롤러의 공통 자료형 + 엔진 입력 변환."""

from dataclasses import dataclass

from app.schemas.lineup import AttendeeProfile


@dataclass(frozen=True)
class StatLine:
    """타석 결과 집계 카운트. AttendeeProfile의 raw 카운트 필드와 1:1."""

    singles: int = 0
    doubles: int = 0
    triples: int = 0
    home_runs: int = 0
    walks: int = 0
    hit_by_pitch: int = 0
    sacrifice_flies: int = 0
    strikeouts: int = 0
    in_play_outs: int = 0
    reached_on_errors: int = 0


@dataclass(frozen=True)
class PlayerLine:
    """표시 라벨(마스킹된 이름 등) + StatLine. 데모/크롤러 출력 단위."""

    label: str
    statline: StatLine


def to_attendee_profile(
    line: StatLine,
    *,
    user_id: int,
    primary_position: str = "DH",
    sub_positions: list[str] | None = None,
    bats_left: bool = False,
    bench_ratio_recent: float = 0.0,
) -> AttendeeProfile:
    return AttendeeProfile(
        user_id=user_id,
        primary_position=primary_position,
        sub_positions=sub_positions or [],
        bats_left=bats_left,
        bench_ratio_recent=bench_ratio_recent,
        singles=line.singles,
        doubles=line.doubles,
        triples=line.triples,
        home_runs=line.home_runs,
        walks=line.walks,
        hit_by_pitch=line.hit_by_pitch,
        sacrifice_flies=line.sacrifice_flies,
        strikeouts=line.strikeouts,
        in_play_outs=line.in_play_outs,
        reached_on_errors=line.reached_on_errors,
    )

"""라인업 추천 데모 (엔진 직결, api 우회).

사용법:
  python scripts/demo_lineup.py                  # 합성 PA=200 vs PA=5 비교
  python scripts/demo_lineup.py --pa 300         # 합성 표본 크기 지정
  python scripts/demo_lineup.py --club-idx 40837 # gameone 실데이터(마스킹)
"""

import argparse

from app.services import batting_order
from app.tooling import gameone, synthetic
from app.tooling.archetypes import Archetype
from app.tooling.statline import PlayerLine, to_attendee_profile

POSITIONS = ["P", "C", "1B", "2B", "3B", "SS", "LF", "CF", "RF"]


def _synthetic_lines(pa: int) -> list[PlayerLine]:
    specs = [Archetype.OVERALL, Archetype.PURE_ONBASE, Archetype.POWER] + [Archetype.AVERAGE] * 6
    return [
        PlayerLine(label=f"{arch.value}#{idx + 1}", statline=synthetic.generate(arch, plate_appearances=pa, seed=100 + idx + 1))
        for idx, arch in enumerate(specs)
    ]


def _render(title: str, lines: list[PlayerLine]) -> str:
    usable = lines[:9]
    starters = [
        to_attendee_profile(pl.statline, user_id=i + 1, primary_position=POSITIONS[i % len(POSITIONS)])
        for i, pl in enumerate(usable)
    ]
    out = [f"## {title}"]
    slots = batting_order.order(starters)
    if slots is None:
        out.append("(콜드 스타트 — 기록 없음, 좌우 교차 폴백)")
        return "\n".join(out)

    reason_by_uid = batting_order.reasons(starters) or {}
    label_by_uid = {i + 1: usable[i].label for i in range(len(usable))}
    for uid in sorted(slots, key=lambda u: slots[u]):
        out.append(f"{slots[uid]}번  {label_by_uid[uid]:<16} — {reason_by_uid.get(uid, '')}")
    return "\n".join(out)


def main() -> None:
    parser = argparse.ArgumentParser(description="라인업 추천 데모 (엔진 직결)")
    parser.add_argument("--club-idx", type=int, default=None, help="gameone club_idx")
    parser.add_argument("--pa", type=int, default=200, help="합성 데이터 표본 타석 수")
    args = parser.parse_args()

    if args.club_idx is not None:
        lines = gameone.fetch_club_hitters(args.club_idx)
        print(_render(f"gameone club {args.club_idx} (실데이터·마스킹)", lines))
    else:
        print(_render(f"합성 PA={args.pa} (특성 또렷)", _synthetic_lines(args.pa)))
        print()
        print(_render("합성 PA=5 (작은 표본 — shrinkage로 평탄화)", _synthetic_lines(5)))


if __name__ == "__main__":
    main()

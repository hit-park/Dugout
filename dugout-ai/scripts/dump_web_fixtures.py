"""웹 데모용 합성 라인업 픽스처 덤프 (기존 생성기 재사용, 로직 중복 없음).

사용법: python scripts/dump_web_fixtures.py [출력디렉터리]
기본 출력: <repo>/dugout-web/src/fixtures/
"""

import json
import sys
from pathlib import Path

from app.tooling import synthetic
from app.tooling.archetypes import Archetype
from app.tooling.statline import PlayerLine, to_attendee_profile

POSITIONS = ["P", "C", "1B", "2B", "3B", "SS", "LF", "CF", "RF"]
SPECS = [Archetype.OVERALL, Archetype.PURE_ONBASE, Archetype.POWER] + [Archetype.AVERAGE] * 6


def _fixture(pa: int) -> dict:
    lines = [
        PlayerLine(
            label=f"{arch.value}#{i + 1}",
            statline=synthetic.generate(arch, plate_appearances=pa, seed=100 + i + 1),
        )
        for i, arch in enumerate(SPECS)
    ]
    attendees = [
        to_attendee_profile(pl.statline, user_id=i + 1, primary_position=POSITIONS[i % len(POSITIONS)])
        for i, pl in enumerate(lines)
    ]
    return {
        "attendees": [a.model_dump() for a in attendees],
        "labels": {str(i + 1): lines[i].label for i in range(len(lines))},
    }


def main() -> None:
    if len(sys.argv) > 1:
        out_dir = Path(sys.argv[1])
    else:
        out_dir = Path(__file__).resolve().parents[2] / "dugout-web" / "src" / "fixtures"
    out_dir.mkdir(parents=True, exist_ok=True)
    for name, pa in (("pa200", 200), ("pa5", 5)):
        path = out_dir / f"{name}.json"
        path.write_text(json.dumps(_fixture(pa), ensure_ascii=False, indent=2), encoding="utf-8")
        print(f"wrote {path}")


if __name__ == "__main__":
    main()

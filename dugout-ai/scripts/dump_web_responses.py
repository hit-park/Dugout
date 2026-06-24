"""웹 정적 배포용 라인업 응답 미리 계산 (엔진 재사용, 결정적).

사용법: python scripts/dump_web_responses.py [출력파일]
기본 출력: <repo>/dugout-web/src/responses.json
"""

import json
import sys
from pathlib import Path

from app.schemas.lineup import AttendeeProfile, LineupRecommendRequest
from app.services import hungarian

REPO = Path(__file__).resolve().parents[2]
FIXTURES_DIR = REPO / "dugout-web" / "src" / "fixtures"
SAMPLES = ("pa200", "pa5")
# 9명 풀로스터에선 BALANCED/COMPETITIVE 출력이 동일 → BALANCED 고정
MODE = "BALANCED"


def _response(sample: str) -> dict:
    fixture = json.loads((FIXTURES_DIR / f"{sample}.json").read_text(encoding="utf-8"))
    attendees = [AttendeeProfile(**a) for a in fixture["attendees"]]
    req = LineupRecommendRequest(match_id=0, attendees=attendees, lineup_mode=MODE)
    return hungarian.recommend(req).model_dump()


def main() -> None:
    if len(sys.argv) > 1:
        out = Path(sys.argv[1])
    else:
        out = REPO / "dugout-web" / "src" / "responses.json"
    payload = {s: _response(s) for s in SAMPLES}
    out.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"wrote {out}: {len(payload)} responses")


if __name__ == "__main__":
    main()

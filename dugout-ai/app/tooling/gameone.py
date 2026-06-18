"""gameone 사회인 야구 타자 랭킹 크롤러.

club_idx → 마스킹된 PlayerLine 목록. 실명(PII)은 적재 즉시 마스킹한다.
컬럼 인덱스는 라이브 DOM 기준이며, 사이트 개편 시 _COL_* 상수만 조정.
"""

import re

import httpx
from bs4 import BeautifulSoup  # type: ignore[import-untyped]

from app.core.errors import AIException
from app.tooling.statline import PlayerLine, StatLine

_BASE_URL = "https://www.gameone.kr/club/info/ranking/hitter"

# 0-based 컬럼 인덱스 (타자 랭킹 테이블)
_COL_NAME = 1
_COL_AB = 5
_COL_HITS = 7
_COL_SINGLES = 8
_COL_DOUBLES = 9
_COL_TRIPLES = 10
_COL_HOME_RUNS = 11
_COL_SAC_FLIES = 17
_COL_WALKS = 18
_COL_HBP = 20
_COL_STRIKEOUTS = 21
_COL_OBP = 24  # 최소 컬럼 수 가드 기준

_NAME_RE = re.compile(r"(.+?)\((\d+)\)\s*$")


def mask_name(raw: str) -> str:
    """실명을 첫 글자 + '**'로 마스킹. 등번호는 보존."""
    raw = raw.strip()
    m = _NAME_RE.match(raw)
    if m:
        name, jersey = m.group(1), m.group(2)
        return f"{name[0]}**({jersey})"
    return f"{raw[0]}**" if raw else "**"


def _to_int(text: str) -> int:
    try:
        return int(text.strip())
    except ValueError:
        return 0


def parse_hitter_rows(html: str) -> list[PlayerLine]:
    soup = BeautifulSoup(html, "html.parser")
    result: list[PlayerLine] = []
    for tr in soup.select("tr"):
        cells = [td.get_text(strip=True) for td in tr.find_all("td")]
        if len(cells) <= _COL_OBP:
            continue  # 헤더/요약/빈 행 스킵
        ab = _to_int(cells[_COL_AB])
        hits = _to_int(cells[_COL_HITS])
        strikeouts = _to_int(cells[_COL_STRIKEOUTS])
        statline = StatLine(
            singles=_to_int(cells[_COL_SINGLES]),
            doubles=_to_int(cells[_COL_DOUBLES]),
            triples=_to_int(cells[_COL_TRIPLES]),
            home_runs=_to_int(cells[_COL_HOME_RUNS]),
            walks=_to_int(cells[_COL_WALKS]),
            hit_by_pitch=_to_int(cells[_COL_HBP]),
            sacrifice_flies=_to_int(cells[_COL_SAC_FLIES]),
            strikeouts=strikeouts,
            in_play_outs=max(0, ab - hits - strikeouts),
            reached_on_errors=0,  # 페이지에 없음 → ROE는 아웃 취급과 일관
        )
        result.append(PlayerLine(label=mask_name(cells[_COL_NAME]), statline=statline))
    return result


def fetch_club_hitters(club_idx: int, *, timeout: float = 10.0) -> list[PlayerLine]:
    try:
        resp = httpx.get(_BASE_URL, params={"club_idx": club_idx}, timeout=timeout)
        resp.raise_for_status()
    except httpx.HTTPStatusError as e:
        raise AIException(
            code="GAMEONE_HTTP_ERROR",
            message=f"gameone 응답 오류: {e.response.status_code}",
            status_code=502,
        ) from e
    except httpx.RequestError as e:
        raise AIException(
            code="GAMEONE_UNREACHABLE",
            message="gameone 연결 실패",
            status_code=502,
        ) from e

    rows = parse_hitter_rows(resp.text)
    if not rows:
        raise AIException(
            code="GAMEONE_EMPTY",
            message="파싱된 타자 기록이 없습니다 (club_idx 확인 또는 DOM 변경)",
            status_code=404,
        )
    return rows

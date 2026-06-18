"""gameone 크롤러 — 파싱/마스킹은 오프라인 합성 HTML로, 실데이터는 스모크로."""

from app.tooling.gameone import mask_name, parse_hitter_rows

# 컬럼 순서(gameone 타자 랭킹): 순위0 이름1 타율2 게임수3 타석4 타수5 득점6
# 총안타7 1루타8 2루타9 3루타10 홈런11 루타12 타점13 도루14 도실15
# 희타16 희비17 볼넷18 고의4구19 사구20 삼진21 ... (24=출루율)
_HEADER = [
    "순위", "이름", "타율", "게임수", "타석", "타수", "득점", "총안타",
    "1루타", "2루타", "3루타", "홈런", "루타", "타점", "도루", "도실",
    "희타", "희비", "볼넷", "고의4구", "사구", "삼진", "병살", "장타율",
    "출루율", "도루성공률", "멀티히트", "OPS", "BB/K", "장타/안타",
]


def _row(values: dict[int, str]) -> str:
    cells = ["0"] * len(_HEADER)
    for idx, val in values.items():
        cells[idx] = val
    return "<tr>" + "".join(f"<td>{c}</td>" for c in cells) + "</tr>"


def _html(*rows: str) -> str:
    return f"<table><tbody>{''.join(rows)}</tbody></table>"


def test_mask_name_keeps_first_char_and_jersey():
    assert mask_name("홍길동(7)") == "홍**(7)"
    assert mask_name("김합성(61)") == "김**(61)"


def test_mask_name_without_jersey():
    assert mask_name("홍길동") == "홍**"


def test_parse_extracts_statline_and_masks_name():
    # 이름=홍길동(7) 타수=20 총안타=9 1루타5 2루타2 3루타1 홈런1 희비0 볼넷3 사구0 삼진4
    row = _row({1: "홍길동(7)", 5: "20", 7: "9", 8: "5", 9: "2", 10: "1", 11: "1", 18: "3", 21: "4"})
    rows = parse_hitter_rows(_html(row))
    assert len(rows) == 1
    pl = rows[0]
    assert pl.label == "홍**(7)"                  # 실명 마스킹
    assert pl.statline.singles == 5
    assert pl.statline.doubles == 2
    assert pl.statline.triples == 1
    assert pl.statline.home_runs == 1
    assert pl.statline.walks == 3
    assert pl.statline.strikeouts == 4
    # in_play_outs = 타수(20) - 안타(9) - 삼진(4) = 7
    assert pl.statline.in_play_outs == 7


def test_parse_skips_short_rows():
    short = "<tr><td>요약</td><td>3</td></tr>"
    good = _row({1: "김합성(11)", 5: "4", 7: "2", 8: "2", 21: "1"})
    rows = parse_hitter_rows(_html(short, good))
    assert len(rows) == 1
    assert rows[0].label == "김**(11)"


def test_parse_empty_table_returns_empty_list():
    assert parse_hitter_rows(_html()) == []

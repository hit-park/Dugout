"""결정적 합성 타석 데이터 생성기.

(아키타입, 표본크기, seed) → StatLine. 주입된 seed로만 난수를 쓰므로
동일 입력은 항상 동일 출력 — 검증 테스트가 100% 재현된다.
"""

import random

from app.tooling.archetypes import OUTCOMES, Archetype, weights
from app.tooling.statline import StatLine


def generate(archetype: Archetype, *, plate_appearances: int, seed: int) -> StatLine:
    if plate_appearances < 0:
        raise ValueError("plate_appearances must be >= 0")

    w = weights(archetype)
    population = list(OUTCOMES)
    probs = [w[o] for o in population]
    rng = random.Random(seed)
    draws = rng.choices(population, weights=probs, k=plate_appearances)

    counts = {outcome: 0 for outcome in OUTCOMES}
    for outcome in draws:
        counts[outcome] += 1

    return StatLine(**counts)

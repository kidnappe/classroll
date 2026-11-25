#!/usr/bin/env python
"""在真机标注上网格搜索可解释的版面门限与 UI 降权组合。

用法: python tools/grid_search.py artifacts/real-device/photo-dataset.json
"""
from __future__ import annotations

import itertools
import sys
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
from eval_rules import load


def main() -> None:
    path = Path(sys.argv[1] if len(sys.argv) > 1 else "artifacts/real-device/photo-dataset.json")
    rows = [r for r in load(path) if r["verdict"] == "Likely"]
    y = np.array([r["y"] for r in rows])

    def a(key):
        return np.array([r[key] for r in rows], dtype=float)

    chars, area, screen = a("chars"), a("area"), a("screen")
    boxes, avg, short = a("boxes"), a("avgchars"), a("short")
    top, topedge = a("top"), a("topedge")
    ui, clock, ss = a("ui"), a("clock"), a("screenshot")

    results = []
    for area_t, avg_t, short_t, box_min, box_max, top_t, topedge_t in itertools.product(
        (0.02, 0.03, 0.04), (8.0, 8.5, 9.0), (0.55, 0.60, 0.65),
        (3, 5), (40, 50, 60), (0.0, 0.01, 0.02), (0.2, 0.3, 0.4),
    ):
        layout = (
            (chars >= 30) & (area >= area_t) & (boxes >= box_min) & (boxes <= box_max)
            & (avg >= avg_t) & (short <= short_t) & (top >= top_t) & (topedge <= topedge_t)
        )
        for penalty_name, penalty in (
            ("none", np.zeros(len(rows), dtype=bool)),
            ("ss", ss >= 1),
            ("ss+clock2", (ss >= 1) | (clock >= 2)),
            ("ss+clock2+ui3", (ss >= 1) | (clock >= 2) | (ui >= 3)),
        ):
            for strong_name, strong in (
                ("nostrong", np.zeros(len(rows), dtype=bool)),
                ("avg>=14&short<=.3", (avg >= 14) & (short <= 0.3)),
                ("avg>=13&boxes<=30", (avg >= 13) & (boxes <= 30)),
                ("avg>=12&short<=.4", (avg >= 12) & (short <= 0.4)),
            ):
                mask = layout & (~penalty | strong)
                tp = int(((mask == 1) & (y == 1)).sum())
                fp = int(((mask == 1) & (y == 0)).sum())
                fn = int(((mask == 0) & (y == 1)).sum())
                p = tp / (tp + fp) if tp + fp else 0.0
                r = tp / (tp + fn) if tp + fn else 0.0
                f1 = 2 * p * r / (p + r) if p + r else 0.0
                results.append((p, r, f1, area_t, avg_t, short_t, box_min, box_max,
                                top_t, topedge_t, penalty_name, strong_name, tp, fp, fn))

    print("== F1 前 20 ==")
    for row in sorted(results, key=lambda v: -v[2])[:20]:
        p, r, f1, area_t, avg_t, short_t, box_min, box_max, top_t, topedge_t, pen, strong, tp, fp, fn = row
        print(f"P={p:.3f} R={r:.3f} F1={f1:.3f} TP={tp} FP={fp} FN={fn} | area>={area_t} avg>={avg_t} "
              f"short<={short_t} box {box_min}..{box_max} top>={top_t} te<={topedge_t} pen={pen} strong={strong}")

    print("\n== P>=0.85 中召回前 20 ==")
    filtered = [row for row in results if row[0] >= 0.85]
    for row in sorted(filtered, key=lambda v: -v[1])[:20]:
        p, r, f1, area_t, avg_t, short_t, box_min, box_max, top_t, topedge_t, pen, strong, tp, fp, fn = row
        print(f"P={p:.3f} R={r:.3f} F1={f1:.3f} TP={tp} FP={fp} FN={fn} | area>={area_t} avg>={avg_t} "
              f"short<={short_t} box {box_min}..{box_max} top>={top_t} te<={topedge_t} pen={pen} strong={strong}")

    print("\n== P>=0.88 中召回前 15 ==")
    filtered = [row for row in results if row[0] >= 0.88]
    for row in sorted(filtered, key=lambda v: -v[1])[:15]:
        p, r, f1, area_t, avg_t, short_t, box_min, box_max, top_t, topedge_t, pen, strong, tp, fp, fn = row
        print(f"P={p:.3f} R={r:.3f} F1={f1:.3f} TP={tp} FP={fp} FN={fn} | area>={area_t} avg>={avg_t} "
              f"short<={short_t} box {box_min}..{box_max} top>={top_t} te<={topedge_t} pen={pen} strong={strong}")


if __name__ == "__main__":
    main()

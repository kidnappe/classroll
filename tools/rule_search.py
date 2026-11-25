#!/usr/bin/env python
"""在真机数据上迭代候选规则，打印混淆矩阵与残余错误。

用法:
  python tools/rule_search.py artifacts/real-device/photo-dataset.json
"""
from __future__ import annotations

import sys
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
from eval_rules import load


def evaluate(rows, mask, label):
    y = np.array([r["y"] for r in rows])
    tp = int(((mask == 1) & (y == 1)).sum())
    fp = int(((mask == 1) & (y == 0)).sum())
    fn = int(((mask == 0) & (y == 1)).sum())
    tn = int(((mask == 0) & (y == 0)).sum())
    precision = tp / (tp + fp) if tp + fp else 0.0
    recall = tp / (tp + fn) if tp + fn else 0.0
    print(f"{label:64} P={precision:.3f} R={recall:.3f} TP={tp:3d} FP={fp:3d} FN={fn:3d} TN={tn:3d}")
    return precision, recall


def main() -> None:
    path = Path(sys.argv[1] if len(sys.argv) > 1 else "artifacts/real-device/photo-dataset.json")
    rows = [r for r in load(path) if r["verdict"] == "Likely"]

    def a(key):
        return np.array([r[key] for r in rows], dtype=float)

    chars, area, screen = a("chars"), a("area"), a("screen")
    boxes, avgchars, short, long = a("boxes"), a("avgchars"), a("short"), a("long")
    avgh, top, bottom, vspan = a("avgh"), a("top"), a("bottom"), a("vspan")
    topedge, bottomedge = a("topedge"), a("bottomedge")
    ui, clock, screenshot = a("ui"), a("clock"), a("screenshot")

    base = (chars >= 30) & (area >= 0.03) & (boxes >= 3)
    typography = (avgchars >= 9) & (short <= 0.55) & (boxes <= 45)
    margin = (top >= 0.02) & (topedge <= 0.3)
    layout = base & typography & margin
    ui_negative = screenshot + (clock >= 2) + (ui >= 3) >= 1
    layout_strong = (avgchars >= 13) & (boxes <= 25) & (short <= 0.45)

    tests = {
        "current": ((chars >= 20) & (area >= 0.006)) | ((chars >= 8) & (area >= 0.012)) | ((screen >= 0.55) & (chars >= 6)),
        "layout": layout,
        "layout & !ui_negative": layout & ~ui_negative,
        "layout & (!ui_negative | layout_strong)": layout & (~ui_negative | layout_strong),
        "layout & !screenshot": layout & (screenshot == 0),
        "layout & !screenshot(strong ok)": layout & ((screenshot == 0) | layout_strong),
        "layout & !clock>=2": layout & (clock < 2),
        "layout & short<=.45": layout & (short <= 0.45),
        "layout & short<=.45 & top>=.03": layout & (short <= 0.45) & (top >= 0.03),
        "layout & avg>=10": layout & (avgchars >= 10),
        "layout & avg>=11": layout & (avgchars >= 11),
        "layout & top>=.03": layout & (top >= 0.03),
        "layout & top>=.03 & avg>=10": layout & (top >= 0.03) & (avgchars >= 10),
        "layout & top>=.03 & avg>=10 & short<=.45": layout & (top >= 0.03) & (avgchars >= 10) & (short <= 0.45),
        "layout & vspan<=.95": layout & (vspan <= 0.95),
        "layout & !ui_negative & vspan<=.95": layout & ~ui_negative & (vspan <= 0.95),
    }
    for label, mask in tests.items():
        evaluate(rows, mask, label)

    best = layout & (~ui_negative | layout_strong)
    print("\n== 最佳候选残余误报 ==")
    for i, r in enumerate(rows):
        if best[i] and r["y"] == 0:
            print(f"- {r['name'][:50]:50} chars={r['chars']:4.0f} area={r['area']:.3f} lines={r['lines']:3.0f} "
                  f"avg={r['avgchars']:5.1f} short={r['short']:.2f} top={r['top']:.2f} ss={r['screenshot']:.0f} "
                  f"clock={r['clock']:.0f} ui={r['ui']:.0f}")
            print(f"  {r['text'][:140]!r}")

    print("\n== 最佳候选残余漏标 ==")
    for i, r in enumerate(rows):
        if best[i] == 0 and r["y"] == 1:
            print(f"- {r['name'][:50]:50} chars={r['chars']:4.0f} area={r['area']:.3f} lines={r['lines']:3.0f} "
                  f"avg={r['avgchars']:5.1f} short={r['short']:.2f} top={r['top']:.2f} ss={r['screenshot']:.0f} "
                  f"clock={r['clock']:.0f} ui={r['ui']:.0f}")
            print(f"  {r['text'][:140]!r}")


if __name__ == "__main__":
    main()

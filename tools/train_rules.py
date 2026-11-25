#!/usr/bin/env python
"""在真机标注数据上比较规则与小型模型，输出可解释的判定门限。

用法:
  python tools/train_rules.py artifacts/real-device/photo-dataset.json
"""
from __future__ import annotations

import sys
from pathlib import Path

import numpy as np
from sklearn.ensemble import GradientBoostingClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import roc_auc_score
from sklearn.model_selection import StratifiedKFold
from sklearn.tree import DecisionTreeClassifier, export_text

sys.path.insert(0, str(Path(__file__).resolve().parent))
from eval_rules import load


FEATURES = [
    "chars", "area", "screen", "boxes", "avgchars", "short", "long",
    "avgh", "top", "bottom", "vspan", "topedge", "bottomedge",
    "ui", "clock", "screenshot",
]


def build(rows: list[dict]) -> tuple[np.ndarray, np.ndarray]:
    x = np.array([[float(r[key]) for key in FEATURES] for r in rows])
    y = np.array([r["y"] for r in rows])
    return x, y


def main() -> None:
    path = Path(sys.argv[1] if len(sys.argv) > 1 else "artifacts/real-device/photo-dataset.json")
    rows = [r for r in load(path) if r["verdict"] == "Likely"]
    x, y = build(rows)
    print(f"samples={len(y)} pos={int(y.sum())} neg={int((1 - y).sum())}")

    cv = StratifiedKFold(n_splits=5, shuffle=True, random_state=7)
    models = {
        "logistic": lambda: LogisticRegression(max_iter=2000, C=1.0),
        "gboost": lambda: GradientBoostingClassifier(n_estimators=150, max_depth=2, random_state=7),
        "tree3": lambda: DecisionTreeClassifier(max_depth=3, min_samples_leaf=8, random_state=7),
        "tree4": lambda: DecisionTreeClassifier(max_depth=4, min_samples_leaf=6, random_state=7),
    }
    for name, factory in models.items():
        aucs, precs, recalls = [], [], []
        for train, test in cv.split(x, y):
            model = factory().fit(x[train], y[train])
            proba = model.predict_proba(x[test])[:, 1]
            aucs.append(roc_auc_score(y[test], proba))
            pred = proba >= 0.5
            tp = int((pred & (y[test] == 1)).sum())
            fp = int((pred & (y[test] == 0)).sum())
            fn = int((~pred & (y[test] == 1)).sum())
            precs.append(tp / max(1, tp + fp))
            recalls.append(tp / max(1, tp + fn))
        print(f"{name:9} AUC={np.mean(aucs):.3f} P={np.mean(precs):.3f} R={np.mean(recalls):.3f}")

    logit = LogisticRegression(max_iter=2000, C=1.0).fit(x, y)
    print("\n== logistic 系数（未标准化，看方向） ==")
    for name, coef in sorted(zip(FEATURES, logit.coef_[0]), key=lambda kv: -abs(kv[1])):
        print(f"{name:10} {coef:+.4f}")

    tree = DecisionTreeClassifier(max_depth=4, min_samples_leaf=6, random_state=7).fit(x, y)
    print("\n== 决策树（深度 4） ==")
    print(export_text(tree, feature_names=FEATURES, decimals=3))


if __name__ == "__main__":
    main()

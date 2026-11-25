#!/usr/bin/env python
"""把 HPROF 导出的照片数据集整理成可复用的“扫描记录”。

不修改 App、不实现 App 落盘；只生成测试用记录：
  - 全部 576 张候选
  - 人工勾选 / 未勾选标签
  - 当时 verdict、OCR 文本/框、粗筛特征
  - 后续规则回放所需的派生版式特征

用法:
  python tools/make_scan_record.py artifacts/real-device/photo-dataset.json artifacts/real-device/scan-record.json
"""
from __future__ import annotations

import csv
import json
import re
import sys
from collections import Counter
from datetime import datetime
from pathlib import Path

CLOCK_PATTERN = re.compile(r"\b\d{1,2}:\d{2}\b")
SCREENSHOT_NAME = re.compile(
    r"(screenshot|screencap|screen[-_ ]?shot|截屏|截图)",
    re.IGNORECASE,
)
HEADING_PATTERN = re.compile(
    r"^\s*(?:[一二三四五六七八九十]+[、.．]|第[一二三四五六七八九十0-9]+[章节讲部分]|"
    r"\d+[、.．)）]|[（(]\d+[）)]|[·•●▪◆◇→\-—])"
)


def layout(ocr: dict) -> dict:
    text = ocr.get("text", "")
    boxes = ocr.get("boxes", [])
    text_lines = [line.strip() for line in text.split("\n") if line.strip()]
    line_count = max(len(boxes), len(text_lines))
    char_count = int(ocr.get("characterCount", 0))
    average_chars = char_count / line_count if line_count else 0.0
    short_lines = (
        sum(1 for line in text_lines if len(re.sub(r"\s", "", line)) <= 6) / len(text_lines)
        if text_lines
        else 1.0
    )
    top_edge = (
        sum(1 for box in boxes if float(box.get("top", 0)) < 0.06) / len(boxes)
        if boxes
        else 0.0
    )
    heights = [float(box.get("bottom", 0)) - float(box.get("top", 0)) for box in boxes]
    return {
        "lineCount": line_count,
        "averageCharsPerLine": round(average_chars, 4),
        "shortLineRatio": round(short_lines, 4),
        "topEdgeRatio": round(top_edge, 4),
        "averageBoxHeight": round(sum(heights) / len(heights), 4) if heights else 0.0,
        "maxBoxHeight": round(max(heights), 4) if heights else 0.0,
        "clockHits": len(CLOCK_PATTERN.findall(text)),
        "screenshotName": bool(SCREENSHOT_NAME.search(ocr.get("_displayName", ""))),
        "headingCount": sum(1 for line in text_lines if HEADING_PATTERN.match(line)),
    }


def label_of(item: dict) -> tuple[str, bool, bool]:
    verdict = item.get("verdict", "")
    selected = bool(item.get("selected"))
    touched = bool(item.get("touched"))
    reviewed = verdict == "Likely" or touched
    if verdict == "Likely":
        return ("positive" if selected else "negative"), reviewed, True
    if verdict == "Boundary" and selected:
        return "positive_boundary", reviewed, False
    return "unlabeled", reviewed, False


def main() -> None:
    if len(sys.argv) != 3:
        raise SystemExit("usage: make_scan_record.py <photo-dataset.json> <scan-record.json>")
    source = Path(sys.argv[1])
    target = Path(sys.argv[2])
    data = json.loads(source.read_text(encoding="utf-8"))
    raw_items = data["items"]

    items = []
    for raw in raw_items:
        ocr = dict(raw.get("ocr", {}))
        ocr["_displayName"] = raw.get("displayName", "")
        features = layout(ocr)
        label, reviewed, tuning_eligible = label_of(raw)
        items.append({
            "id": raw.get("id", ""),
            "imageUri": raw.get("imageUri", ""),
            "displayName": raw.get("displayName", ""),
            "title": raw.get("title", ""),
            "groupId": raw.get("groupId", ""),
            "timeLabel": raw.get("timeLabel", ""),
            "verdictAtScan": raw.get("verdict", ""),
            "statusAtScan": raw.get("status", ""),
            "reason": raw.get("reason", ""),
            "error": raw.get("error"),
            "selected": bool(raw.get("selected")),
            "touched": bool(raw.get("touched")),
            "reviewed": reviewed,
            "label": label,
            "tuningEligible": tuning_eligible,
            "coarse": raw.get("coarse", {}),
            "ocr": {
                "characterCount": int(ocr.get("characterCount", 0)),
                "textAreaRatio": float(ocr.get("textAreaRatio", 0.0)),
                "text": ocr.get("text", ""),
                "boxes": ocr.get("boxes", []),
            },
            "layout": features,
        })

    verdict_counts = Counter(item["verdictAtScan"] for item in items)
    label_counts = Counter(item["label"] for item in items)
    selected_count = sum(1 for item in items if item["selected"])
    reviewed_count = sum(1 for item in items if item["reviewed"])
    likely = [item for item in items if item["verdictAtScan"] == "Likely"]
    positive = sum(1 for item in items if item["label"] == "positive")
    negative = sum(1 for item in items if item["label"] == "negative")
    boundary_positive = sum(1 for item in items if item["label"] == "positive_boundary")
    unlabeled = sum(1 for item in items if item["label"] == "unlabeled")

    assert len(items) > 0
    assert selected_count == sum(1 for item in items if item["selected"])
    assert positive + negative == len(likely)
    assert positive == sum(1 for item in likely if item["selected"])
    assert negative == len(likely) - positive
    assert boundary_positive == sum(1 for item in items if item["verdictAtScan"] == "Boundary" and item["selected"])
    assert reviewed_count == sum(1 for item in items if item["reviewed"])
    assert positive + negative + boundary_positive + unlabeled == len(items)

    record = {
        "schemaVersion": 1,
        "recordType": "classroll_scan_record",
        "generatedAt": datetime.now().astimezone().isoformat(timespec="seconds"),
        "source": {
            "hprof": data.get("sourceHprof", ""),
            "dataset": str(source).replace("\\", "/"),
        },
        "scan": {
            "candidateCount": len(items),
            "selectedCount": selected_count,
            "touchedCount": sum(1 for item in items if item["touched"]),
            "reviewedCount": reviewed_count,
            "verdictCounts": dict(verdict_counts),
            "labelCounts": dict(label_counts),
            "tuningSet": {
                "verdict": "Likely",
                "total": len(likely),
                "positive": positive,
                "negative": negative,
            },
            "boundaryPositive": boundary_positive,
            "unlabeled": unlabeled,
            "selectedSetSizes": data.get("selectedSets", []),
            "labelPolicy": [
                "Likely 档全部视为已审阅：selected=true -> positive；selected=false -> negative。",
                "Boundary 档只把 selected=true 的记为 positive_boundary，不把其余 Boundary 当成 negative。",
                "Doubtful / Unlike 未逐张审阅，label=unlabeled，不参与 precision/recall。",
                "positive_boundary 单独统计，不混入 Likely 的正负样本。",
                "touched=true 表示人工点过或批量改过；reviewed = Likely 或 touched。",
                "文件名来自截图（Screenshot/截屏/截图/screencap）的照片按规则跳过识别，verdict=Unlike。",
                "layout 由 ocr.text 与 ocr.boxes 计算，口径与 App 识别规则一致。",
            ],
        },
        "items": items,
    }
    target.write_text(json.dumps(record, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    csv_path = target.with_suffix(".csv")
    columns = [
        "id", "displayName", "verdictAtScan", "statusAtScan", "selected", "touched", "reviewed",
        "label", "tuningEligible", "characterCount", "textAreaRatio", "lineCount",
        "averageCharsPerLine", "shortLineRatio", "topEdgeRatio", "clockHits",
        "screenshotName", "screenScore", "faceCount", "reason",
    ]
    with csv_path.open("w", encoding="utf-8-sig", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=columns)
        writer.writeheader()
        for item in items:
            features = item["layout"]
            writer.writerow({
                "id": item["id"],
                "displayName": item["displayName"],
                "verdictAtScan": item["verdictAtScan"],
                "statusAtScan": item["statusAtScan"],
                "selected": item["selected"],
                "touched": item["touched"],
                "reviewed": item["reviewed"],
                "label": item["label"],
                "tuningEligible": item["tuningEligible"],
                "characterCount": item["ocr"]["characterCount"],
                "textAreaRatio": item["ocr"]["textAreaRatio"],
                "lineCount": features["lineCount"],
                "averageCharsPerLine": features["averageCharsPerLine"],
                "shortLineRatio": features["shortLineRatio"],
                "topEdgeRatio": features["topEdgeRatio"],
                "clockHits": features["clockHits"],
                "screenshotName": features["screenshotName"],
                "screenScore": item["coarse"].get("screenScore", 0.0),
                "faceCount": item["coarse"].get("faceCount", 0),
                "reason": item["reason"],
            })

    summary_path = target.with_name(target.stem + "-summary.txt")
    summary = [
        f"source hprof: {record['source']['hprof']}",
        f"generatedAt: {record['generatedAt']}",
        f"candidates: {len(items)}",
        f"selected: {selected_count}",
        f"touched: {record['scan']['touchedCount']}",
        f"reviewed: {reviewed_count}",
        f"verdicts: {dict(verdict_counts)}",
        f"labels: {dict(label_counts)}",
        f"tuning set: Likely {len(likely)} = {positive} positive + {negative} negative",
        f"boundary positive: {boundary_positive}",
        f"unlabeled: {unlabeled}",
        f"selected sets in hprof: {data.get('selectedSets', [])}",
        "",
        "label policy:",
        *[f"- {line}" for line in record["scan"]["labelPolicy"]],
    ]
    summary_path.write_text("\n".join(summary) + "\n", encoding="utf-8")

    print(f"written: {target}")
    print(f"written: {csv_path}")
    print(f"written: {summary_path}")
    print("\n".join(summary))


if __name__ == "__main__":
    main()

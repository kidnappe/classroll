#!/usr/bin/env python
"""用真机标注数据评估识别规则。

数据来自 tools/hprof_photo_dataset.py 导出的 photo-dataset.json。
只使用 RecognitionRules.afterOcr 能拿到的字段：OCR 文本/字数/面积/文本框 + 粗筛特征。

用法:
  python tools/eval_rules.py artifacts/real-device/photo-dataset.json
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

import numpy as np

# 屏幕 UI / 应用界面常见词。真实课件正文几乎不会整行出现这些词。
UI_KEYWORDS = [
    "微信", "朋友圈", "聊天", "发送", "撤回", "语音", "视频通话", "转账", "红包", "表情",
    "淘宝", "天猫", "京东", "拼多多", "购物车", "宝贝", "订单", "物流", "客服", "优惠券",
    "支付宝", "余额宝", "账单", "付款", "收款",
    "抖音", "快手", "微博", "小红书", "B站", "哔哩哔哩", "关注", "粉丝", "点赞", "评论", "转发",
    "高德地图", "百度地图", "导航", "路线", "驾车", "公交", "地铁", "打车",
    "天气", "空气质量", "风力", "降水", "摄氏度",
    "设置", "通用", "隐私", "通知", "蜂窝网络", "无线局域网", "蓝牙", "电池", "存储空间",
    "应用商店", "下载", "安装", "更新", "版本", "权限",
    "王者荣耀", "和平精英", "原神", "排位", "战绩", "对局", "英雄", "皮肤", "装备",
    "美团", "饿了么", "外卖", "餐厅", "评价", "人均",
    "携程", "去哪儿", "机票", "酒店", "车票",
    "邮箱", "收件箱", "已发送", "草稿",
    "浏览器", "搜索", "历史记录", "书签",
    "日历", "闹钟", "计时器", "备忘录",
    "WLAN", "Wi-Fi", "WiFi", "蓝牙", "飞行模式", "个人热点",
    "确定", "取消", "完成", "编辑", "分享", "更多", "返回",
    "元", "￥", "¥", "折", "包邮", "秒杀", "立即购买",
]
UI_PATTERN = re.compile("|".join(re.escape(word) for word in UI_KEYWORDS))
CLOCK_PATTERN = re.compile(r"\b\d{1,2}:\d{2}\b")
PERCENT_PATTERN = re.compile(r"\b\d{1,3}%")
HEADING_PATTERN = re.compile(
    r"^\s*(?:[一二三四五六七八九十]+[、.．]|第[一二三四五六七八九十0-9]+[章节讲部分]|"
    r"\d+[、.．)）]|[（(]\d+[）)]|[·•●▪◆◇→\-—])"
)
BATTERY_PATTERN = re.compile(r"(电量|电池|charging|Charging)")
SCREENSHOT_NAME = re.compile(
    r"(screenshot|screen[-_ ]?shot|截屏|截图|mmexport|wx_camera|wechat|weixin|"
    r"qqimage|qq_image|screencap|dcim/screenshots)",
    re.IGNORECASE,
)


def line_stats(text: str) -> tuple[int, float, float, float, int]:
    lines = [ln.strip() for ln in text.split("\n") if ln.strip()]
    if not lines:
        return 0, 0.0, 0.0, 0.0, 0
    counts = [len(re.sub(r"\s", "", ln)) for ln in lines]
    return (
        len(lines),
        float(np.mean(counts)),
        float(np.mean([n <= 6 for n in counts])),
        float(np.mean([n >= 20 for n in counts])),
        max(counts),
    )


def box_stats(boxes: list[dict]) -> dict[str, float]:
    if not boxes:
        return dict(avgh=0.0, maxh=0.0, top=1.0, bottom=0.0, left=0.0, right=0.0,
                    hspan=0.0, vspan=0.0, topedge=0.0, bottomedge=0.0,
                    leftedge=0.0, rightedge=0.0)
    heights = [b["bottom"] - b["top"] for b in boxes]
    left = min(b["left"] for b in boxes)
    right = max(b["right"] for b in boxes)
    top = min(b["top"] for b in boxes)
    bottom = max(b["bottom"] for b in boxes)
    n = len(boxes)
    return dict(
        avgh=float(np.mean(heights)),
        maxh=float(max(heights)),
        top=top,
        bottom=bottom,
        left=left,
        right=right,
        hspan=right - left,
        vspan=bottom - top,
        topedge=sum(1 for b in boxes if b["top"] < 0.06) / n,
        bottomedge=sum(1 for b in boxes if b["bottom"] > 0.94) / n,
        leftedge=sum(1 for b in boxes if b["left"] < 0.04) / n,
        rightedge=sum(1 for b in boxes if b["right"] > 0.96) / n,
    )


def load(path: Path) -> list[dict]:
    data = json.loads(path.read_text(encoding="utf-8"))
    rows = []
    for item in data["items"]:
        ocr = item["ocr"]
        coarse = item["coarse"]
        lines, avgchars, short, long, maxline = line_stats(ocr["text"])
        stats = box_stats(ocr["boxes"])
        text = ocr["text"]
        ui_hits = len(UI_PATTERN.findall(text))
        headings = sum(1 for ln in text.split("\n") if HEADING_PATTERN.match(ln))
        rows.append(dict(
            id=item["id"],
            name=item["displayName"] or "",
            verdict=item["verdict"],
            y=1 if item["selected"] else 0,
            chars=ocr["characterCount"],
            area=ocr["textAreaRatio"],
            screen=coarse["screenScore"],
            faces=coarse["faceCount"],
            maxface=coarse["maxFaceAreaRatio"],
            bright=coarse["brightness"],
            outlier=coarse["outlierDistance"],
            boxes=len(ocr["boxes"]),
            lines=lines,
            avgchars=avgchars,
            short=short,
            long=long,
            maxline=maxline,
            ui=ui_hits,
            headings=headings,
            clock=len(CLOCK_PATTERN.findall(text)),
            percent=len(PERCENT_PATTERN.findall(text)),
            screenshot=1 if SCREENSHOT_NAME.search(item["displayName"] or "") else 0,
            text=text,
            **stats,
        ))
    return rows


def evaluate(rows: list[dict], mask: np.ndarray, label: str) -> dict:
    y = np.array([r["y"] for r in rows])
    tp = int(((mask == 1) & (y == 1)).sum())
    fp = int(((mask == 1) & (y == 0)).sum())
    fn = int(((mask == 0) & (y == 1)).sum())
    tn = int(((mask == 0) & (y == 0)).sum())
    precision = tp / (tp + fp) if tp + fp else 0.0
    recall = tp / (tp + fn) if tp + fn else 0.0
    print(f"{label:58} P={precision:.3f} R={recall:.3f} TP={tp:3d} FP={fp:3d} FN={fn:3d} TN={tn:3d}")
    return dict(label=label, P=precision, R=recall, tp=tp, fp=fp, fn=fn, tn=tn)


def main() -> None:
    path = Path(sys.argv[1] if len(sys.argv) > 1 else "artifacts/real-device/photo-dataset.json")
    rows = load(path)
    likely = [r for r in rows if r["verdict"] == "Likely"]
    y = np.array([r["y"] for r in likely])
    print(f"total={len(rows)} likely={len(likely)} pos={int((y == 1).sum())} neg={int((y == 0).sum())}")

    def col(key):
        return np.array([r[key] for r in likely], dtype=float)

    chars, area, screen, faces = col("chars"), col("area"), col("screen"), col("faces")
    maxface, bright, outlier = col("maxface"), col("bright"), np.nan_to_num(col("outlier"), nan=-1.0)
    boxes, lines, avgchars = col("boxes"), col("lines"), col("avgchars")
    short, long, maxline = col("short"), col("long"), col("maxline")
    avgh, maxh = col("avgh"), col("maxh")
    top, bottom, hspan, vspan = col("top"), col("bottom"), col("hspan"), col("vspan")
    topedge, bottomedge = col("topedge"), col("bottomedge")
    ui, clock, percent, screenshot = col("ui"), col("clock"), col("percent"), col("screenshot")

    print("\n== 均值对比 ==")
    keys = ["chars", "area", "screen", "faces", "boxes", "lines", "avgchars", "short", "long",
            "maxline", "avgh", "maxh", "top", "bottom", "hspan", "vspan", "topedge",
            "bottomedge", "ui", "clock", "percent", "screenshot"]
    for key in keys:
        values = np.array([r[key] for r in likely], dtype=float)
        print(f"{key:10} pos={values[y == 1].mean():8.3f} neg={values[y == 0].mean():8.3f}")

    print("\n== 当前规则复现 ==")
    current = (
        ((chars >= 20) & (area >= 0.006))
        | ((chars >= 8) & (area >= 0.012))
        | ((screen >= 0.55) & (chars >= 6))
    )
    evaluate(likely, current, "current afterOcr")

    print("\n== 候选规则 ==")
    base_text = (chars >= 30) & (area >= 0.03)
    candidates = {
        "chars>=30 & area>=.03": base_text,
        "base & avgchars>=9": base_text & (avgchars >= 9),
        "base & boxes>=4": base_text & (boxes >= 4),
        "base & boxes>=4 & avgchars>=9": base_text & (boxes >= 4) & (avgchars >= 9),
        "base & boxes 4..40 & avgchars>=9": base_text & (boxes >= 4) & (boxes <= 40) & (avgchars >= 9),
        "base & short<=.55": base_text & (short <= 0.55),
        "base & short<=.45": base_text & (short <= 0.45),
        "base & boxes>=4 & short<=.55": base_text & (boxes >= 4) & (short <= 0.55),
        "base & boxes>=4 & avgchars>=9 & short<=.5": base_text & (boxes >= 4) & (avgchars >= 9) & (short <= 0.5),
        "base & boxes>=4 & avgchars>=9 & top>=.03": base_text & (boxes >= 4) & (avgchars >= 9) & (top >= 0.03),
        "base & boxes>=4 & avgchars>=9 & ui==0": base_text & (boxes >= 4) & (avgchars >= 9) & (ui == 0),
        "base & boxes>=4 & avgchars>=9 & screenshot==0": base_text & (boxes >= 4) & (avgchars >= 9) & (screenshot == 0),
        "base & boxes>=4 & avgchars>=9 & topedge<=.2": base_text & (boxes >= 4) & (avgchars >= 9) & (topedge <= 0.2),
    }
    for label, mask in candidates.items():
        evaluate(likely, mask, label)

    # 简单阈值网格：area / avgchars / boxes
    print("\n== 网格搜索 (area, avgchars, boxes) ==")
    best = []
    for area_t in (0.02, 0.03, 0.04, 0.05, 0.06, 0.08, 0.10):
        for avg_t in (7, 8, 9, 10, 11, 12):
            for box_t in (3, 4, 5, 6):
                mask = (chars >= 30) & (area >= area_t) & (avgchars >= avg_t) & (boxes >= box_t)
                tp = int(((mask == 1) & (y == 1)).sum())
                fp = int(((mask == 1) & (y == 0)).sum())
                fn = int(((mask == 0) & (y == 1)).sum())
                p = tp / (tp + fp) if tp + fp else 0.0
                r = tp / (tp + fn)
                best.append((p, r, area_t, avg_t, box_t, tp, fp, fn))
    best.sort(key=lambda row: (row[0] * row[1]) ** 0.5, reverse=True)
    for p, r, area_t, avg_t, box_t, tp, fp, fn in best[:12]:
        print(f"area>={area_t:<5} avg>={avg_t:<3} boxes>={box_t}  P={p:.3f} R={r:.3f} TP={tp} FP={fp} FN={fn}")

    print("\n== 当前规则误报样例 (前 25) ==")
    for i, r in enumerate(likely):
        if current[i] == 1 and y[i] == 0:
            print(f"- {r['name'][:48]:48} chars={r['chars']:4.0f} area={r['area']:.3f} "
                  f"lines={r['lines']:3.0f} avg={r['avgchars']:5.1f} short={r['short']:.2f} "
                  f"ui={r['ui']:.0f} clock={r['clock']:.0f} pct={r['percent']:.0f} ss={r['screenshot']:.0f}")
            print(f"  {r['text'][:150]!r}")

    print("\n== 当前规则漏标样例 (前 15) ==")
    for i, r in enumerate(likely):
        if current[i] == 0 and y[i] == 1:
            print(f"- {r['name'][:48]:48} chars={r['chars']:4.0f} area={r['area']:.3f} "
                  f"lines={r['lines']:3.0f} avg={r['avgchars']:5.1f} short={r['short']:.2f}")
            print(f"  {r['text'][:150]!r}")


if __name__ == "__main__":
    main()

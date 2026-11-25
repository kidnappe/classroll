#!/usr/bin/env python
"""从真机 HPROF 中导出每张照片的识别特征与人工勾选标签。

用法：
  python tools/hprof_photo_dataset.py artifacts/real-device/classroll-selection-conv.hprof artifacts/real-device/photo-dataset.json
"""
from __future__ import annotations

import json
import re
import struct
import sys
from collections import Counter
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from hprof_selection_extract import PHOTO_ID, Hprof, hash_set_keys

TYPES = {4: ("boolean", 1), 5: ("char", 2), 6: ("float", 4), 7: ("double", 8),
         8: ("byte", 1), 9: ("short", 2), 10: ("int", 4), 11: ("long", 8)}


def read_fields(h: Hprof, object_id: int) -> dict[str, tuple[int, object]]:
    class_id = h.instance_classes.get(object_id)
    body = h.instances.get(object_id)
    if not class_id or body is None:
        return {}
    result: dict[str, tuple[int, object]] = {}
    offset = 0
    for _, fields in h.layout(class_id):
        for name_id, value_type in fields:
            name = h.strings.get(name_id, "")
            if value_type == 2:
                if offset + h.ids > len(body):
                    return result
                result[name] = (2, int.from_bytes(body[offset:offset + h.ids], "big"))
                offset += h.ids
            else:
                _, size = TYPES.get(value_type, ("?", 0))
                if offset + size > len(body):
                    return result
                raw = body[offset:offset + size]
                value: object
                if value_type == 4:
                    value = raw[0] != 0
                elif value_type == 5:
                    value = int.from_bytes(raw, "big")
                elif value_type == 6:
                    value = struct.unpack(">f", raw)[0]
                elif value_type == 7:
                    value = struct.unpack(">d", raw)[0]
                elif value_type == 8:
                    value = int.from_bytes(raw, "big", signed=True)
                elif value_type == 9:
                    value = int.from_bytes(raw, "big", signed=True)
                elif value_type == 10:
                    value = int.from_bytes(raw, "big", signed=True)
                elif value_type == 11:
                    value = int.from_bytes(raw, "big", signed=True)
                else:
                    value = None
                result[name] = (value_type, value)
                offset += size
    return result


def enum_name(h: Hprof, object_id: int) -> str:
    if not object_id:
        return ""
    values = read_fields(h, object_id)
    name = values.get("name")
    if not name:
        return ""
    return h.text(name[1])


def boxed_int(h: Hprof, object_id: int) -> int | None:
    if not object_id:
        return None
    values = read_fields(h, object_id)
    value = values.get("value")
    return int(value[1]) if value else None


def string_value(h: Hprof, object_id: int) -> str:
    return h.text(object_id) if object_id else ""


def text_boxes(h: Hprof, list_id: int) -> list[dict[str, float]]:
    if not list_id:
        return []
    values = read_fields(h, list_id)
    element_data = values.get("elementData")
    size = values.get("size")
    if not element_data or not size:
        return []
    array = h.object_arrays.get(element_data[1])
    if not array:
        return []
    result = []
    for box_id in array[: int(size[1])]:
        if not box_id:
            continue
        box = read_fields(h, box_id)
        if not {"left", "top", "right", "bottom"}.issubset(box):
            continue
        result.append({
            "left": float(box["left"][1]),
            "top": float(box["top"][1]),
            "right": float(box["right"][1]),
            "bottom": float(box["bottom"][1]),
        })
    return result


def main() -> None:
    if len(sys.argv) != 3:
        raise SystemExit("usage: hprof_photo_dataset.py <input.hprof> <output.json>")
    h = Hprof(Path(sys.argv[1]))

    photos: dict[str, dict] = {}
    for object_id in h.instances:
        if h.name_of(object_id) != "com.classroll.photo.model.PhotoItem":
            continue
        values = read_fields(h, object_id)
        photo_id = string_value(h, values.get("id", (2, 0))[1])
        if not PHOTO_ID.match(photo_id):
            continue
        coarse = read_fields(h, values.get("coarse", (2, 0))[1])
        ocr = read_fields(h, values.get("ocr", (2, 0))[1])
        boxes = text_boxes(h, ocr.get("boxes", (2, 0))[1])
        photos[photo_id] = {
            "id": photo_id,
            "imageUri": string_value(h, values.get("imageUri", (2, 0))[1]),
            "displayName": string_value(h, values.get("displayName", (2, 0))[1]),
            "title": string_value(h, values.get("title", (2, 0))[1]),
            "groupId": string_value(h, values.get("groupId", (2, 0))[1]),
            "timeLabel": string_value(h, values.get("timeLabel", (2, 0))[1]),
            "reason": string_value(h, values.get("reason", (2, 0))[1]),
            "error": string_value(h, values.get("error", (2, 0))[1]) or None,
            "status": enum_name(h, values.get("status", (2, 0))[1]),
            "verdict": enum_name(h, values.get("verdict", (2, 0))[1]),
            "timeSource": enum_name(h, values.get("timeSource", (2, 0))[1]),
            "coarse": {
                "brightness": float(coarse.get("brightness", (6, 0.0))[1]),
                "faceCount": int(coarse.get("faceCount", (10, 0))[1]),
                "maxFaceAreaRatio": float(coarse.get("maxFaceAreaRatio", (6, 0.0))[1]),
                "nearDuplicateCount": int(coarse.get("nearDuplicateCount", (10, 0))[1]),
                "outlierDistance": boxed_int(h, coarse.get("outlierDistance", (2, 0))[1]),
                "perceptualHash": int(coarse.get("perceptualHash", (11, 0))[1]),
                "screenScore": float(coarse.get("screenScore", (6, 0.0))[1]),
            },
            "ocr": {
                "characterCount": int(ocr.get("characterCount", (10, 0))[1]),
                "textAreaRatio": float(ocr.get("textAreaRatio", (6, 0.0))[1]),
                "text": string_value(h, ocr.get("text", (2, 0))[1]),
                "boxes": boxes,
            },
        }

    photo_ids = set(photos)
    sets = []
    for object_id in h.instances:
        if h.name_of(object_id) not in ("java.util.LinkedHashSet", "java.util.HashSet"):
            continue
        keys = [key for key in hash_set_keys(h, object_id) if key in photo_ids]
        if keys:
            sets.append((object_id, keys))
    if not sets:
        raise SystemExit("没有在 HPROF 里找到照片 ID 集合")
    sets.sort(key=lambda item: len(item[1]), reverse=True)

    # 新版本 ScanUiState 同时有 selectedIds 和 touchedIds；优先按字段名精确读取，
    # 读不到时退回旧口径：取最大的集合当 selectedIds。
    selected_ids = set(sets[0][1])
    touched_ids = set()
    for object_id in h.instances:
        if h.name_of(object_id) != "com.classroll.photo.scan.ScanUiState":
            continue
        fields = read_fields(h, object_id)
        selected_field = fields.get("selectedIds")
        touched_field = fields.get("touchedIds")
        if not selected_field or selected_field[0] != 2:
            continue
        candidate_selected = {key for key in hash_set_keys(h, selected_field[1]) if key in photo_ids}
        if len(candidate_selected) < len(selected_ids):
            continue
        selected_ids = candidate_selected
        if touched_field and touched_field[0] == 2:
            touched_ids = {key for key in hash_set_keys(h, touched_field[1]) if key in photo_ids}

    for photo_id, item in photos.items():
        item["selected"] = photo_id in selected_ids
        item["touched"] = photo_id in touched_ids

    output = sorted(photos.values(), key=lambda item: item["timeLabel"])
    Path(sys.argv[2]).write_text(json.dumps({
        "sourceHprof": str(Path(sys.argv[1])).replace("\\", "/"),
        "selectedCount": len(selected_ids),
        "touchedCount": len(touched_ids),
        "candidateCount": len(output),
        "selectedSets": [{"objectId": object_id, "size": len(keys)} for object_id, keys in sets[:10]],
        "items": output,
    }, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    print(f"photos={len(output)} selected={len(selected_ids)} touched={len(touched_ids)}")
    print(f"sets={[len(keys) for _, keys in sets[:10]]}")
    print("verdicts", Counter(item["verdict"] for item in output))
    print("selected verdicts", Counter(item["verdict"] for item in output if item["selected"]))
    print("touched verdicts", Counter(item["verdict"] for item in output if item["touched"]))


if __name__ == "__main__":
    main()

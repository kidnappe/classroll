#!/usr/bin/env python
"""从 Android HPROF 中提取课片当前勾选的照片，不依赖 Android Studio。"""
from __future__ import annotations

import argparse
import json
import re
import struct
from collections import defaultdict
from pathlib import Path

PHOTO_ID = re.compile(r"^(?:media-\d+|picker--?\d+-\d+)$")


class Hprof:
    def __init__(self, path: Path):
        self.data = path.read_bytes()
        self.pos = 0
        self.ids = 4
        self.strings = {}
        self.class_names = {}
        self.class_supers = {}
        self.class_fields = {}
        self.instances = {}
        self.instance_classes = {}
        self.object_arrays = {}
        self.primitive_arrays = {}
        self.array_classes = {}
        self._read_header()
        self._read_records()

    def _u(self, size: int) -> int:
        value = int.from_bytes(self.data[self.pos:self.pos + size], "big")
        self.pos += size
        return value

    def _u1(self) -> int:
        return self._u(1)

    def _u2(self) -> int:
        return self._u(2)

    def _u4(self) -> int:
        return self._u(4)

    def _id(self) -> int:
        return self._u(self.ids)

    def _skip(self, size: int) -> None:
        self.pos += size

    def _read_header(self) -> None:
        end = self.data.index(b"\x00", 0, 64)
        self.pos = end + 1
        self.ids = self._u4()
        self._skip(8)

    def _read_records(self) -> None:
        while self.pos + 9 <= len(self.data):
            tag = self._u1()
            self._skip(4)
            length = self._u4()
            end = self.pos + length
            if end > len(self.data):
                break
            if tag == 0x01:
                sid = self._id()
                self.strings[sid] = self.data[self.pos:end].decode("utf-8", "replace")
            elif tag == 0x02:
                self._u4()
                class_id = self._id()
                self._u4()
                name_id = self._id()
                self.class_names[class_id] = name_id
            elif tag in (0x0C, 0x1C):
                self._read_heap(self.pos, end)
            self.pos = end

    def _read_heap(self, pos: int, end: int) -> None:
        self.pos = pos
        trace = []
        while self.pos < end:
            start = self.pos
            tag = self._u1()
            trace.append((hex(tag), start))
            if len(trace) > 16:
                trace.pop(0)
            if tag == 0xFF:
                self._id()
            elif tag == 0x01:
                self._id()
                self._id()
            elif tag in (0x02, 0x03):
                self._id()
                self._u4()
                self._u4()
            elif tag == 0x04:
                self._id()
                self._u4()
            elif tag in (0x05, 0x06, 0x07):
                self._id()
                if tag == 0x06:
                    self._u4()
            elif tag == 0x08:
                self._id()
                self._u4()
                self._u4()
            elif tag in (0x89, 0x8A, 0x8B, 0x8C, 0x8D):
                self._id()
            elif tag == 0x8E:
                self._id()
                self._u4()
                self._u4()
            elif tag == 0xFE:
                self._u4()
                self._id()
            elif tag == 0x20:
                self._read_class_dump()
            elif tag == 0x21:
                self._read_instance()
            elif tag == 0x22:
                self._read_object_array()
            elif tag == 0x23:
                self._read_primitive_array()
            else:
                raise ValueError(f"unknown heap tag 0x{tag:02x} at {self.pos - 1}; recent={trace}")

    def _value_size(self, value_type: int) -> int:
        if value_type == 2:
            return self.ids
        return {4: 1, 5: 2, 6: 4, 7: 8, 8: 1, 9: 2, 10: 4, 11: 8}.get(value_type, 0)

    def _read_class_dump(self) -> None:
        class_id = self._id()
        self._u4()
        super_id = self._id()
        self.class_supers[class_id] = super_id
        self._id()
        self._id()
        self._id()
        self._u4()
        self._u4()
        self._u4()
        constants = self._u2()
        for _ in range(constants):
            self._u2()
            value_type = self._u1()
            self._skip(self._value_size(value_type))
        statics = self._u2()
        for _ in range(statics):
            self._id()
            value_type = self._u1()
            self._skip(self._value_size(value_type))
        fields_count = self._u2()
        fields = []
        for _ in range(fields_count):
            name_id = self._id()
            value_type = self._u1()
            fields.append((name_id, value_type))
        self.class_fields[class_id] = fields

    def _read_instance(self) -> None:
        object_id = self._id()
        self._u4()
        class_id = self._id()
        size = self._u4()
        body = self.data[self.pos:self.pos + size]
        self.pos += size
        self.instances[object_id] = body
        self.instance_classes[object_id] = class_id

    def _read_object_array(self) -> None:
        array_id = self._id()
        self._u4()
        count = self._u4()
        class_id = self._id()
        values = [self._id() for _ in range(count)]
        self.object_arrays[array_id] = values
        self.array_classes[array_id] = class_id

    def _read_primitive_array(self) -> None:
        array_id = self._id()
        self._u4()
        count = self._u4()
        element_type = self._u1()
        size = count * {4: 1, 5: 2, 6: 4, 7: 8, 8: 1, 9: 2, 10: 4, 11: 8}.get(element_type, 1)
        self.primitive_arrays[array_id] = (element_type, self.data[self.pos:self.pos + size])
        self.pos += size
        self.array_classes[array_id] = element_type

    def class_name(self, class_id: int) -> str:
        name_id = self.class_names.get(class_id, 0)
        return self.strings.get(name_id, "")

    def layout(self, class_id: int) -> list[tuple[str, int]]:
        result = []
        current = class_id
        while current:
            name = self.class_name(current)
            fields = self.class_fields.get(current, [])
            result.append((name, fields))
            current = self.class_supers.get(current, 0)
        return result

    def fields(self, object_id: int) -> dict[str, int]:
        class_id = self.instance_classes.get(object_id)
        body = self.instances.get(object_id)
        if not class_id or body is None:
            return {}
        result = {}
        offset = 0
        for _, fields in self.layout(class_id):
            for name_id, value_type in fields:
                name = self.strings.get(name_id, "")
                if value_type == 2:
                    value = int.from_bytes(body[offset:offset + self.ids], "big")
                    result[name] = value
                    offset += self.ids
                else:
                    offset += {4: 1, 5: 2, 6: 4, 7: 8, 8: 1, 9: 2, 10: 4, 11: 8}.get(value_type, 1)
        return result

    def name_of(self, object_id: int) -> str:
        return self.class_name(self.instance_classes.get(object_id, 0))

    def string(self, string_id: int) -> str:
        return self.strings.get(string_id, "")

    def text(self, object_id: int) -> str:
        if self.name_of(object_id) != "java.lang.String":
            return self.strings.get(object_id, "")
        fields = self.fields(object_id)
        value_id = fields.get("value", 0)
        array = self.primitive_arrays.get(value_id)
        if not array:
            return ""
        element_type, data = array
        if element_type == 5:
            return data.decode("utf-16-be", "replace").rstrip("\x00")
        if element_type == 8:
            coder = fields.get("coder", 0)
            return data.decode("utf-16-be" if coder else "latin-1", "replace")
        return ""


def hash_set_keys(h: Hprof, set_id: int) -> list[str]:
    values = h.fields(set_id)
    hash_map_id = values.get("map", 0)
    if not hash_map_id:
        return []
    map_values = h.fields(hash_map_id)
    table_id = map_values.get("table", 0)
    if table_id in h.object_arrays:
        buckets = h.object_arrays[table_id]
    elif table_id in h.instances:
        buckets = []
    else:
        buckets = []
    result = []
    seen_nodes = set()
    for node_id in buckets:
        while node_id and node_id not in seen_nodes:
            seen_nodes.add(node_id)
            node = h.fields(node_id)
            key_id = node.get("key", 0)
            text = h.text(key_id)
            if text:
                result.append(text)
            node_id = node.get("next", 0)
    return result


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("hprof", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    h = Hprof(args.hprof)

    photos = []
    for object_id in h.instances:
        if h.name_of(object_id) != "com.classroll.photo.model.PhotoItem":
            continue
        fields = h.fields(object_id)
        photo_id = h.text(fields.get("id", 0))
        if not PHOTO_ID.match(photo_id):
            continue
        photos.append({
            "id": photo_id,
            "imageUri": h.text(fields.get("imageUri", 0)),
            "displayName": h.text(fields.get("displayName", 0)),
            "title": h.text(fields.get("title", 0)),
            "groupId": h.text(fields.get("groupId", 0)),
            "timeLabel": h.text(fields.get("timeLabel", 0)),
            "reason": h.text(fields.get("reason", 0)),
        })
    unique = {item["id"]: item for item in photos}
    print(f"photoItems={len(unique)} sample={list(unique)[:3]}")
    photo_ids = set(unique)
    sets = []
    set_count = 0
    for object_id in h.instances:
        if h.name_of(object_id) not in ("java.util.LinkedHashSet", "java.util.HashSet"):
            continue
        set_count += 1
        keys = hash_set_keys(h, object_id)
        photo_keys = [key for key in keys if key in photo_ids]
        if photo_keys:
            sets.append((object_id, photo_keys))

    print(f"photoIds={len(photo_ids)} hashSets={set_count} matchedSets={len(sets)}")
    for object_id, keys in sorted(sets, key=lambda item: len(item[1]), reverse=True)[:10]:
        print(f"  set {object_id:08x} keys={len(keys)} sample={keys[:3]}")
    selected = sorted((s for s in sets if len(s[1]) >= 10), key=lambda item: len(item[1]), reverse=True)
    if not selected:
        raise SystemExit("没有找到照片 ID 集合")
    selected_ids = set(selected[0][1])

    for item in unique.values():
        item["selected"] = item["id"] in selected_ids
    output = {
        "candidateCount": len(unique),
        "selectedCount": len(selected_ids),
        "setCandidates": [{"objectId": obj, "size": len(keys)} for obj, keys in selected[:10]],
        "selected": [item for item in unique.values() if item["selected"]],
        "allPhotos": sorted(unique.values(), key=lambda item: item["timeLabel"]),
    }
    args.output.write_text(json.dumps(output, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"sets={[len(keys) for _, keys in selected[:10]]}")
    print(f"photos={len(unique)} selected={len(selected_ids)}")
    print(f"written={args.output}")


if __name__ == "__main__":
    main()

#!/usr/bin/env python
"""把学校课表导出转换成课片可导入的 JSON。"""
from __future__ import annotations

import argparse
import json
from datetime import date
from pathlib import Path


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--semester-start", default="2026-09-07")
    parser.add_argument("--before", type=int, default=10)
    parser.add_argument("--after", type=int, default=20)
    return parser.parse_args()


def convert(source: dict, semester_start: date, before: int, after: int) -> dict:
    if semester_start.weekday() != 0:
        raise ValueError("第一周周一必须是周一")
    school_schedules = source.get("schoolSchedules") or {}
    profiles = []
    for season in ("summer", "winter"):
        item = school_schedules.get(season)
        if not item:
            raise ValueError(f"缺少 {season} 作息")
        sections = []
        for section in item.get("sections", []):
            start = section.get("start", "")
            end = section.get("end", "")
            if not start or not end or end <= start:
                raise ValueError(f"{season} 第 {section.get('index')} 节时间不合法")
            sections.append({"index": int(section["index"]), "start": start, "end": end})
        if not sections:
            raise ValueError(f"{season} 作息没有节次")
        effective = item.get("effective2026")
        if not effective:
            raise ValueError(f"{season} 作息缺少生效日期")
        profiles.append({
            "effectiveFrom": effective,
            "name": item.get("name") or season,
            "sections": sections,
        })
    profiles.sort(key=lambda profile: profile["effectiveFrom"])

    courses = []
    for index, course in enumerate(source.get("courses", []), start=1):
        name = (course.get("name") or "").strip()
        day = int(course.get("day", 0))
        if not name or not 1 <= day <= 7:
            raise ValueError(f"第 {index} 条课程名称或星期不合法")
        section_numbers = [int(value) for value in str(course.get("sections", "")).split(",") if value.strip()]
        if not section_numbers:
            raise ValueError(f"{name} 没有上课节次")
        if section_numbers != list(range(section_numbers[0], section_numbers[-1] + 1)):
            raise ValueError(f"{name} 的上课节次不连续：{section_numbers}")
        for profile in profiles:
            available = {int(item["index"]) for item in profile["sections"]}
            if not set(section_numbers).issubset(available):
                raise ValueError(f"{name} 的节次在 {profile['name']} 中不存在")
        weeks = [int(value) for value in str(course.get("weeks", "")).split(",") if value.strip()]
        if not weeks or any(week < 1 or week > 52 for week in weeks):
            raise ValueError(f"{name} 的周次不合法")
        converted = {
            "name": name,
            "weekday": day,
            "sectionStart": section_numbers[0],
            "sectionEnd": section_numbers[-1],
            "weeks": ",".join(str(week) for week in sorted(set(weeks))),
            "room": (course.get("position") or "").strip(),
        }
        teacher = (course.get("teacher") or "").strip()
        if teacher:
            converted["teacher"] = teacher
        courses.append(converted)

    return {
        "semesterStart": semester_start.isoformat(),
        "bufferBeforeMin": before,
        "bufferAfterMin": after,
        "timeProfiles": profiles,
        "courses": courses,
    }


def main() -> None:
    args = parse_args()
    source = json.loads(args.source.read_text(encoding="utf-8"))
    converted = convert(
        source,
        date.fromisoformat(args.semester_start),
        args.before,
        args.after,
    )
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(converted, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    unique_names = len({course["name"] for course in converted["courses"]})
    print(f"written: {args.output}")
    print(f"courses: {len(converted['courses'])} arrangements / {unique_names} unique names")
    print(f"profiles: {[item['effectiveFrom'] + ' ' + item['name'] for item in converted['timeProfiles']]}")


if __name__ == "__main__":
    main()

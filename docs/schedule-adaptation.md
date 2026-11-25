# 他校课程表适配指南

课页目前只对**河南师范大学**教务系统导出的 `.xls` 周课表做了直接适配。其他学校的课表要先转换成课页认识的格式，或者给解析器补一个分支。

本文面向两类人：

- 想在自己学校用起来、但不想改代码的同学 → 看[路线 A：转成 JSON](#路线-a不改代码转成-json)。
- 愿意改代码并提交 PR 的开发者 → 看[路线 B：为你的学校加-xls-适配](#路线-b为你的学校加-xls-适配)。

---

## 1. 先搞清楚课页能吃什么

| 来源 | 是否直接支持 | 说明 |
| --- | --- | --- |
| 河南师大教务导出的 `.xls` 周课表 | 是 | 按本文第 2 节的结构写的解析器 |
| 其他学校的 `.xls` | 否 | 结构不同，需要转换或改解析器 |
| JSON 课表 v1 / v2 | 是 | 任何学校都可以先转成 JSON |

导入入口：**设置 → 课程与扫描 → 课程表**（没有课表时首页也有「导入课程表」）。导入后会进预览页，确认后保存。

---

## 2. 当前支持的 Excel 长什么样

打开河南师大导出的文件，结构是这样的：

```
┌ 第 1 行：标题        示例学生2026-2027-1课表
├ 第 2 行：周次        （合并）第1周 | 第2周 | 第3周
├ 第 3 行：日期        09-07 09-08 ... 09-13 | 09-14 ...
├ 第 4 行：星期        一 二 三 四 五 六 日 | 一 二 ...
├ 第 5 行：第一大节
├ 第 6 行：第二大节
├ 第 7 行：第三大节
├ 第 8 行：第四大节
├ 第 9 行：第五大节
├ 第 10-11 行：空行
└ 然后重复：下一块再放 3 周
```

- **列**：第 1–7 列是周一至周日，第 8–14 列是下一周，第 15–21 列再下一周；循环。
- **每 11 行一个周块**，一屏放 3 周，整份文件大约 7 个块（19–21 周）。
- **单元格格式**：每门课四行

  ```
  课程名[类型]
  教师
  周次-节次
  教室
  ```

  例如 `马克思主义政治经济学[理论] / 米庭乐 / 1-0708 / 新554_203`，其中 `1-0708` 表示第 1 周、第 7–8 节。

- **一个格子里可以叠两门课**，比如第 6 周周一第 7–8 节同时有「马克思主义哲学」和「马克思主义政治经济学」。解析器会按格子里的「周次-节次」数量切出多门课。
- **教室可能为空**（体育课常见），此时只有三行。
- **节次来自单元格里的 `周次-节次`**，不是按它在第几行反推；同一门课出现在哪几周，就把周次并起来。

---

## 3. 解析器的假设清单（改代码前必读）

实现位于 [`XlsScheduleParser.kt`](../app/src/main/java/com/classroll/photo/data/XlsScheduleParser.kt)。

| # | 假设 | 不满足时会怎样 |
| --- | --- | --- |
| 1 | 读第一个有内容的 sheet | 多 sheet 且课表不在第一个 → 解析到空 |
| 2 | 第 0 列是节次/行标签，课程从第 1 列开始 | 课程列含第 0 列时整体右移 |
| 3 | 周次-节次 token 必须匹配 `^周次-四位数$`，如 `1-0708` | 写成 `第1周 第7-8节` 之类 → 识别不到 |
| 4 | 每门课在格子里固定占「名称/教师/token/教室」四行（教室可为空） | 行序不同 → 名称、教师错位 |
| 5 | 学期第一周周一：取第一列第一个 `MM-DD`，年份从标题的 `YYYY-YYYY-N` 推 | 没有日期行 → 退回当年 9 月 7 日 |
| 6 | 作息时间不来自 Excel | 导入后要用默认作息，需在设置里核对 |
| 7 | 周次直接取 token 前缀 | 若 token 写的是列位置以外的周次，会照 token 走 |

只要你的学校导出文件不满足其中一条，就需要路线 B。

---

## 路线 A：不改代码，转成 JSON

这是最快的办法。课页的 JSON 导入不关心你原来是什么格式。

### A.1 JSON v2 模板

```json
{
  "schemaVersion": 2,
  "term": { "name": "2026-2027-1", "start": "2026-09-07", "totalWeeks": 19 },
  "bufferBeforeMin": 10,
  "bufferAfterMin": 20,
  "skipDates": ["2026-10-01"],
  "skipRanges": [{ "start": "2026-11-10", "end": "2026-11-14" }],
  "timeProfiles": [
    {
      "effectiveFrom": "2026-09-07",
      "name": "秋季作息",
      "groups": { "morning": 4, "afternoon": 4, "evening": 2 },
      "sections": [
        { "index": 1, "start": "08:00", "end": "08:45" },
        { "index": 2, "start": "08:55", "end": "09:40" },
        { "index": 3, "start": "10:10", "end": "10:55" },
        { "index": 4, "start": "11:05", "end": "11:50" },
        { "index": 5, "start": "14:30", "end": "15:15" },
        { "index": 6, "start": "15:25", "end": "16:10" },
        { "index": 7, "start": "16:40", "end": "17:25" },
        { "index": 8, "start": "17:35", "end": "18:20" },
        { "index": 9, "start": "19:30", "end": "20:15" },
        { "index": 10, "start": "20:25", "end": "21:10" }
      ]
    }
  ],
  "courses": [
    {
      "id": "math-mon-0102",
      "name": "高等数学",
      "weekday": 1,
      "sections": [1, 2],
      "weeks": "1-16",
      "room": "教三-201",
      "teacher": "示例教师"
    },
    {
      "name": "大学体育",
      "weekday": 4,
      "sections": [5, 6],
      "weeks": "1-16单",
      "room": "",
      "teacher": "示例教师"
    }
  ]
}
```

### A.2 必填字段

| 字段 | 说明 |
| --- | --- |
| `term.start` | **必须是周一**，第一教学周的周一；也可用旧字段 `semesterStart` |
| `courses` | 至少一门；`name`、`weekday`（1=周一 … 7=周日）必填 |
| `courses[].weeks` | 周次，支持 `1-16`、`1,3,5`、`1-8,10-16`、`单`、`双`、`1-16单`、`第1-9周` |
| `courses[].sections` | 节次数组，如 `[3,4]`；也兼容 `sectionStart/sectionEnd`，或直接给 `start`/`end`（`"08:00"`） |
| `timeProfiles` | **扫描时要用**：至少要有一套覆盖你所有节次的作息，否则这些课没有时间窗 |

`term.name`（学期名）会用作归档目录名；`totalWeeks` 用于限制扫描范围。`bufferBeforeMin` / `bufferAfterMin` 是课前课后缓冲，默认 10 / 20 分钟。

### A.3 用脚本把校内课表转 JSON

用 Python 读原表，按行/列拼出上面的结构即可，示意：

```python
import json, xlrd

sh = xlrd.open_workbook("课表.xls").sheet_by_index(0)
courses = []
# 按你校的布局遍历：找到每一格 -> 解析出 name/weekday/sections/weeks/room
# courses.append({...})
schedule = {
    "schemaVersion": 2,
    "term": {"name": "2026-2027-1", "start": "2026-09-07", "totalWeeks": 19},
    "timeProfiles": [ /* 见上面模板，填你校作息 */ ],
    "courses": courses,
}
json.dump(schedule, open("我的课表.classroll.json", "w", encoding="utf-8"), ensure_ascii=False, indent=2)
```

然后把生成的 JSON 传到手机，在课页里导入即可。

---

## 路线 B：为你的学校加 xls 适配

### B.1 先看清结构

把学校导出的文件拖到电脑上，用下面的脚本 dump 出来（需要 `pip install xlrd`）：

```python
import xlrd
sh = xlrd.open_workbook("你的课表.xls", formatting_info=True).sheet_by_index(0)
print("尺寸", sh.nrows, "x", sh.ncols)
print("合并", sh.merged_cells)
for r in range(min(sh.nrows, 30)):
    cells = [f"{c}:{sh.cell_value(r, c)!r}" for c in range(sh.ncols) if str(sh.cell_value(r, c)).strip()]
    if cells:
        print(r, " | ".join(cells))
```

重点确认：

1. 表头在第几行，周次/日期在哪一行；
2. 一周是几列，从第几列开始；
3. 每个格子里课程信息的行序（课程名、教师、周次、教室）；
4. 节次用什么写法（`1-2`、`0102`、`第1-2节`、`1,2`）；
5. 同一格是否可能有多门课；
6. 周次是写在格子里，还是靠所在列推。

### B.2 加一个解析分支

建议**不要直接改河南师大的逻辑**，而是：

1. 新建 `XlsScheduleParserXx.kt`，输出同样的 `CourseSchedule`；
2. 在调用处按结构探测选择解析器：

   ```kotlin
   // ScanViewModel 里原来只有 XlsScheduleParser.parse(bytes, base)
   val parsed = when {
       XlsScheduleParserXx.looksLike(bytes) -> XlsScheduleParserXx.parse(bytes, base)
       else -> XlsScheduleParser.parse(bytes, base)
   }
   ```

3. 两个解析器做同一种输出：`Course(...)`，其中 `weeks` 是 `Set<Int>`，`sections` 是节次列表。

如果你只是想改河南师大分支里的正则或行偏移，也请把改动限定在 `parseCourseCells` 内部，并补充测试。

### B.3 加测试（必须）

1. 准备一份**脱敏**的课表：标题里的姓名改掉，教师名改成「示例教师A/B/C」；
2. 放到 `app/src/test/resources/`；
3. 在 `XlsScheduleParserTest.kt` 旁边加你的用例，至少断言：
   - 一门每周都上的课，周次覆盖完整；
   - 一门只有单双周的课，周次正确；
   - 一个叠了两门课的格子，两门都在、教室没有错位；
   - 总课程条数与人工数的一致。

参考现有用例：

```kotlin
@Test
fun `周一7-8马克思主义政治经济学覆盖全部周次`() {
    val courses = schedule.courses.filter {
        it.name == "马克思主义政治经济学" && it.weekday == 1 && it.sections == listOf(7, 8)
    }
    assertEquals(1, courses.size)
    assertEquals((1..19).toSet(), courses[0].weeks)
}
```

跑测试：

```bash
./gradlew :app:testDebugUnitTest
```

---

## 5. 作息、节次与时间匹配

- 时间匹配靠「节次 → 具体时间」，时间来自**作息**（`timeProfiles`），不来自 Excel。
- 导入 `.xls` 时会沿用当前已保存的作息；没有就生成一套默认作息（8:00 起，10 节）。
- 一套作息覆盖不了的节次，那门课不会产生扫描时间窗。导入后请到**设置 → 作息时间**核对每一节的起止。
- 作息支持多套，按 `effectiveFrom` 生效，适合「冬令时/夏令时」切换。
- 「上午 4 节 / 下午 4 节 / 晚上 2 节」只用于设置页分组展示，不参与匹配。

## 6. 常见差异怎么处理

| 你校的情况 | 处理方式 |
| --- | --- |
| 课表按「第 1 节、第 2 节」纵向排，不是按周横向排 | 走路线 A（脚本转 JSON），比改解析器省事 |
| 一周 5 天而不是 7 天 | 若走 xls 路线，把 `(col - 1) % 7 + 1` 的 7 改成实际天数，并重算列起点 |
| 节次写成 `第1-2节` 或 `0102-0304` | 改 `tokenPattern` 与节次切分 |
| 周次写成 `1~16` 或 `1-16周` | 在 JSON 里用支持的语法，或扩展解析器的周次正则 |
| 没有日期行 | `semesterStart` 会退回当年 9 月 7 日，导入后在设置里改「学期与周次」 |
| 一个格子里三门课 | 解析器按 token 逐门切分，理论上支持，但请补测试 |
| 教室写在教师前面 | 调整 `parseCourseCells` 里名称/教师/教室的偏移 |

## 7. 单双周、停课、调课

- 单双周：`weeks` 支持 `1-16单`、`1-16双`，也支持 `单`、`双`。
- 停课：在设置 → 停课日期里维护单日或区间，扫描时这些日期不生成时间窗。
- 调课：不做自动识别，靠确认页把那次课的照片手动剔除或换组。

## 8. 提交 Issue / PR 之前

请务必脱敏：

- 删除或替换学生姓名、学号、教师姓名（示例里用「示例教师A」）；
- 课表截图打码；
- 只提供**结构**和**样式**，不需要整份真实课表。

提 Issue 时附上：学校名称、教务导出按钮位置、脱敏后的结构片段（行列分布 + 一两个单元格原文）、以及你希望课页怎么识别。这样适配会快很多。

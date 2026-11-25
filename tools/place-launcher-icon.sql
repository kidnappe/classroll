WITH RECURSIVE counter(i) AS (
    SELECT 0
    UNION ALL
    SELECT i + 1 FROM counter WHERE i < 19
),
grid AS (
    SELECT i % 4 AS x, i / 4 AS y FROM counter
)
INSERT INTO favorites (
    title, intent, container, screen, cellX, cellY, spanX, spanY,
    itemType, appWidgetId, modified, restored, profileId, rank, options, appWidgetSource
)
SELECT
    '课片',
    '#Intent;action=android.intent.action.MAIN;category=android.intent.category.LAUNCHER;launchFlags=0x10200000;component=com.classroll.photo/.MainActivity;end',
    -100, 0, grid.x, grid.y, 1, 1,
    0, -1, 0, 0, 0, 0, 0, -1
FROM grid
WHERE NOT EXISTS (
    SELECT 1 FROM favorites WHERE intent LIKE '%com.classroll.photo%'
)
AND NOT EXISTS (
    SELECT 1 FROM favorites f
    WHERE f.container = -100
      AND f.screen = 0
      AND grid.x < f.cellX + f.spanX
      AND grid.x + 1 > f.cellX
      AND grid.y < f.cellY + f.spanY
      AND grid.y + 1 > f.cellY
)
ORDER BY grid.y, grid.x
LIMIT 1;

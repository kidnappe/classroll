#!/usr/bin/env bash
# 把课片图标放到模拟器桌面（Pixel Launcher）。每次 install 后调用：
#   tools/place-launcher-icon.sh [serial]
set -euo pipefail

ADB="${ADB:-E:/code/code tools/android-sdk/platform-tools/adb.exe}"
SERIAL="${1:-emulator-5554}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
export MSYS_NO_PATHCONV=1

"$ADB" -s "$SERIAL" root >/dev/null 2>&1 || true
sleep 2

DB=$("$ADB" -s "$SERIAL" shell "ls /data/data/com.google.android.apps.nexuslauncher/databases/launcher_*.db 2>/dev/null | head -1" | tr -d '\r')
if [ -z "$DB" ]; then
  echo "找不到 Pixel Launcher 数据库"
  exit 1
fi

if "$ADB" -s "$SERIAL" shell "sqlite3 $DB \"SELECT COUNT(*) FROM favorites WHERE intent LIKE '%com.classroll.photo%';\"" | tr -d '\r' | grep -q '^[1-9]'; then
  echo "课片图标已经在桌面"
  exit 0
fi

read -r OWNER MODE <<EOF
$("$ADB" -s "$SERIAL" shell "stat -c '%u:%g %a' $DB" | tr -d '\r')
EOF

"$ADB" -s "$SERIAL" shell "am force-stop com.google.android.apps.nexuslauncher"
"$ADB" -s "$SERIAL" push "$SCRIPT_DIR/place-launcher-icon.sql" /data/local/tmp/place-launcher-icon.sql >/dev/null
"$ADB" -s "$SERIAL" shell "sqlite3 $DB < /data/local/tmp/place-launcher-icon.sql"
"$ADB" -s "$SERIAL" shell "chown $OWNER $DB; chmod $MODE $DB"
"$ADB" -s "$SERIAL" shell "am start -a android.intent.action.MAIN -c android.intent.category.HOME" >/dev/null
echo "已把课片图标放到桌面"

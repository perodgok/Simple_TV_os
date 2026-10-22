#!/usr/bin/env bash
# Read-only diagnostic for Xiaomi branded remote buttons.
# Run: bash scripts/audit-android-remote-events.sh [ADB_SERIAL]
set -u
TV="${1:-192.168.0.108:5555}"
STAMP="$(date +%Y%m%d-%H%M%S)"
OUT="$HOME/simple-tv-os/logs/android-remote-audit-$STAMP"
mkdir -p "$OUT"

echo "========== НАЧАЛО ОТЧЁТА — ANDROID REMOTE AUDIT =========="
echo "TV=$TV"
echo "Local logs: $OUT"
adb -s "$TV" get-state || exit 1
echo "[1/3] HOME and Android accessibility status"
{
  adb -s "$TV" shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
  adb -s "$TV" shell settings get secure enabled_accessibility_services
  adb -s "$TV" shell settings get secure accessibility_enabled
} | tee "$OUT/environment.txt"

echo "[2/3] Logcat per button; don't press any other buttons during capture"
for key in WINK KION RUTUBE KINOPOISK OKKO IVI; do
  printf '\nPrepare %s. Press ENTER, then press the button ONCE within 5 seconds...\n' "$key"
  read -r _
  # Start fresh logcat reader; logs stay unchanged on the TV.
  adb -s "$TV" logcat -v threadtime -T 1 >"$OUT/$key-full.log" 2>&1 &
  reader=$!
  sleep 5
  kill "$reader" 2>/dev/null || true
  wait "$reader" 2>/dev/null || true
  grep -iE 'keyevent|keycode|inputdispatcher|inputreader|activitytaskmanager|activitymanager|start.*activity|launch|am_start|vcr|pvr|controlpanel|coffee|logoff|database' "$OUT/$key-full.log" | tail -n 45 >"$OUT/$key-filtered.log" || true
  echo "--- $key: filtered events ---"
  cat "$OUT/$key-filtered.log"
done
echo "[3/3] Finished. Full logs contain device/app information; do not upload publicly."
echo "========== КОНЕЦ ОТЧЁТА — ANDROID REMOTE AUDIT =========="
echo "Send filtered logs from: $OUT"

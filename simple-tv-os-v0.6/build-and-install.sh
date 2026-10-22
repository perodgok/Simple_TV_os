#!/bin/bash
set -Eeuo pipefail
export PATH="/opt/homebrew/bin:/opt/homebrew/sbin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin"
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export PATH="$JAVA_HOME/bin:$PATH"
SDK="$HOME/Library/Android/sdk"
BT="$SDK/build-tools/35.0.0"
TV="192.168.0.108:5555"
ROOT="$(cd "$(dirname "$0")" && pwd)"
OUT="$ROOT/build"
mkdir -p "$OUT/classes" "$OUT/dex" "$OUT/res" "$HOME/simple-tv-os/logs"
LOG="$HOME/simple-tv-os/logs/simple-tv-os-v0.6-build.log"
exec > >(tee "$LOG") 2>&1
finish(){ code=$?; if [ "$code" -eq 0 ]; then echo '✅ УСПЕШНО — APK собран и отправлен на ТВ'; else echo "❌ СБОЙ — код $code"; fi; echo '========== КОНЕЦ ОТЧЁТА — SIMPLE TV OS BUILD =========='; }
trap finish EXIT
echo '========== НАЧАЛО ОТЧЁТА — SIMPLE TV OS BUILD =========='
echo '[1/8] Проверяем инструменты...'
for file in "$SDK/platforms/android-35/android.jar" "$BT/aapt2" "$BT/d8" "$BT/zipalign" "$BT/apksigner"; do test -e "$file" || { echo "Отсутствует: $file"; exit 1; }; done
command -v javac; command -v keytool
echo '[2/8] Компилируем ресурсы...'
"$BT/aapt2" compile "$ROOT/res/values/strings.xml" -o "$OUT/res"
echo '[3/8] Компилируем Android manifest и ресурсы...'
"$BT/aapt2" link -o "$OUT/base-unsigned.apk" -I "$SDK/platforms/android-35/android.jar" --manifest "$ROOT/AndroidManifest.xml" --min-sdk-version 23 --target-sdk-version 30 "$OUT/res/values_strings.arsc.flat"
echo '[4/8] Компилируем Java...'
javac -encoding UTF-8 -source 8 -target 8 -classpath "$SDK/platforms/android-35/android.jar" -d "$OUT/classes" "$ROOT/src/com/simpletvos/home/MainActivity.java"
echo '[5/8] Создаём classes.dex...'
"$BT/d8" --min-api 23 --lib "$SDK/platforms/android-35/android.jar" --output "$OUT/dex" $(find "$OUT/classes" -name "*.class")
cp "$OUT/base-unsigned.apk" "$OUT/with-dex.apk"
(cd "$OUT/dex" && zip -q -u "$OUT/with-dex.apk" classes.dex)
echo '[6/8] Выравниваем и подписываем APK...'
"$BT/zipalign" -f 4 "$OUT/with-dex.apk" "$OUT/aligned.apk"
KEY="$HOME/simple-tv-os/keys/simple-tv-os.jks"
mkdir -p "$(dirname "$KEY")"
if [ ! -f "$KEY" ]; then
  keytool -genkeypair -noprompt -keystore "$KEY" -storepass changeit -keypass changeit -alias simpletvos -keyalg RSA -keysize 2048 -validity 3650 -dname 'CN=Simple TV OS, OU=Personal, O=Simple TV OS, C=DE'
fi
"$BT/apksigner" sign --ks "$KEY" --ks-key-alias simpletvos --ks-pass pass:changeit --key-pass pass:changeit --out "$OUT/SimpleTVOS-v0.6.apk" "$OUT/aligned.apk"
"$BT/apksigner" verify "$OUT/SimpleTVOS-v0.6.apk"
echo '[7/8] Проверяем телевизор...'
if [ "$(adb -s "$TV" get-state 2>/dev/null)" != device ]; then adb disconnect "$TV" >/dev/null 2>&1 || true; adb connect "$TV"; fi
[ "$(adb -s "$TV" get-state 2>/dev/null)" = device ] || { echo 'ТВ не авторизован в ADB'; exit 1; }
echo '[8/8] Устанавливаем и запускаем Simple TV OS...'
adb -s "$TV" install --no-incremental -r "$OUT/SimpleTVOS-v0.6.apk"
adb -s "$TV" shell am start -n com.simpletvos.home/.MainActivity
echo 'Обновление поверх v0.1: HOME и системные пакеты НЕ менялись.'
echo "APK: $OUT/SimpleTVOS-v0.6.apk"
echo "Лог: $LOG"

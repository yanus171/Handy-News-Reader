#!/data/data/com.termux/files/usr/bin/bash
# Сборка flavor fullDebug проекта Flym (debug-подпись, пароль не нужен) и установка на устройство.

set -euo pipefail

cd "$(dirname "$0")"

GRADLE_BIN="$(ls -d ~/.gradle/wrapper/dists/gradle-*-bin/*/gradle-*/bin/gradle | head -1)"
if [ -z "$GRADLE_BIN" ]; then
    echo "Gradle не найден в ~/.gradle/wrapper/dists" >&2
    exit 1
fi

ADB=/data/data/com.termux/files/usr/bin/adb
DEVICE="emulator-5554"

# --- сборка ---
"$GRADLE_BIN" :FlymFork:assembleFullDebug \
    -Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2 \
    --console=plain

APK="$(ls "$PWD"/FlymFork/build/outputs/apk/full/debug/*.apk 2>/dev/null | head -1)"
if [ -z "$APK" ]; then
    echo "Ошибка: APK не найден в $PWD/FlymFork/build/outputs/apk/full/debug/" >&2
    exit 1
fi
echo "APK: $APK"

# --- установка ---
"$ADB" -s "$DEVICE" install -r "$APK"
"$ADB" -s "$DEVICE" shell am start -n ru.yanus171.feedexfork/ru.yanus171.feedexfork.activity.HomeActivity

echo "Установлено на $DEVICE, HomeActivity запущена"
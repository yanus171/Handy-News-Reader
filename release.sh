#!/data/data/com.termux/files/usr/bin/bash
# Сборка APK flavor fullRelease проекта Flym с подписью release-ключом.
# Запрашивает пароли keystore и ключа (ввод видимый).

set -euo pipefail

cd "$(dirname "$0")"

GRADLE_BIN="$(ls -d ~/.gradle/wrapper/dists/gradle-*-bin/*/gradle-*/bin/gradle | head -1)"
if [ -z "$GRADLE_BIN" ]; then
    echo "Gradle не найден в ~/.gradle/wrapper/dists" >&2
    exit 1
fi

# --- данные подписи (алиас из HandyClock, ключ скопирован из ../HandyClock_AS/KeyStore) ---
KEY_ALIAS="yanus171key"
KEYSTORE_PATH="$PWD/KeyStore/keyStore"
if [ ! -f "$KEYSTORE_PATH" ]; then
    echo "Ошибка: keystore не найден по пути \"$KEYSTORE_PATH\"." >&2
    echo "Положите файл keyStore в подпапку KeyStore текущей директории." >&2
    exit 1
fi

read -r -p "Пароль keystore: " KEYSTORE_PASSWORD
echo
while ! keytool -list -keystore "$KEYSTORE_PATH" -storepass "$KEYSTORE_PASSWORD" -alias "$KEY_ALIAS" >/dev/null 2>&1; do
    echo "Неверный пароль keystore, или алиас $KEY_ALIAS не найден." >&2
    read -r -p "Пароль keystore: " KEYSTORE_PASSWORD
    echo
done

read -r -p "Пароль ключа: " KEY_PASSWORD
echo

# --- увеличение версии перед сборкой ---
# Версии хранятся в FlymFork/build.gradle: versionName ('1.1.6') и versionCode (341).
# Каждая сборка увеличивает числовую версию на 1 и инкрементирует младший компонент текстовой.
GRADLE_VER_FILE="$PWD/FlymFork/build.gradle"
if [ ! -f "$GRADLE_VER_FILE" ]; then
    echo "Ошибка: не найден $GRADLE_VER_FILE" >&2
    exit 1
fi

OLD_VERSION_NAME="$(sed -n "s/.*versionName '\([^']*\)'.*/\1/p" "$GRADLE_VER_FILE" | head -1)"
OLD_VERSION_CODE="$(sed -n "s/.*versionCode \([0-9]\+\).*/\1/p" "$GRADLE_VER_FILE" | head -1)"
if [ -z "$OLD_VERSION_NAME" ] || [ -z "$OLD_VERSION_CODE" ]; then
    echo "Ошибка: не удалось прочитать версии из $GRADLE_VER_FILE" >&2
    exit 1
fi

NEW_VERSION_CODE=$((OLD_VERSION_CODE + 1))
NEW_VERSION_NAME="$(printf '%s\n' "$OLD_VERSION_NAME" | awk -F. '{ $(NF) += 1; print }' OFS='.')"

# Файл в CRLF: awk сохраняет \r в строке как есть и матчит его через \r?$.
awk -v oldVC="$OLD_VERSION_CODE" -v newVC="$NEW_VERSION_CODE" -v oldVN="$OLD_VERSION_NAME" -v newVN="$NEW_VERSION_NAME" \
    '$0 ~ "^ *versionCode " oldVC "\r?$" { sub(oldVC, newVC) }
     $0 ~ "^ *versionName ." oldVN ".\r?$" { sub(oldVN, newVN) }
     { print }' "$GRADLE_VER_FILE" > "$GRADLE_VER_FILE.tmp" && mv "$GRADLE_VER_FILE.tmp" "$GRADLE_VER_FILE"

NEW_VERSION_CODE_AFTER="$(sed -n "s/.*versionCode \([0-9]\+\).*/\1/p" "$GRADLE_VER_FILE" | head -1)"
NEW_VERSION_NAME_AFTER="$(sed -n "s/.*versionName '\([^']*\)'.*/\1/p" "$GRADLE_VER_FILE" | head -1)"
if [ "$NEW_VERSION_CODE_AFTER" != "$NEW_VERSION_CODE" ] || [ "$NEW_VERSION_NAME_AFTER" != "$NEW_VERSION_NAME" ]; then
    echo "Ошибка: не удалось увеличить версию в $GRADLE_VER_FILE" >&2
    exit 1
fi

echo
# --- фиксация увеличения версии в git ---
# Коммитим только FlymFork/build.gradle (по пути, чтобы не захватить чужие правки рабочей копии).
if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    git add "$GRADLE_VER_FILE"
    git commit -m "v$NEW_VERSION_NAME" -- "$GRADLE_VER_FILE"
    echo "Коммит создан: v$NEW_VERSION_NAME"
else
    echo "Внимание: не git-репозиторий, коммит пропущен." >&2
fi

echo "Версия увеличена: v$OLD_VERSION_NAME ($OLD_VERSION_CODE) -> v$NEW_VERSION_NAME ($NEW_VERSION_CODE)"

# --- сборка ---
"$GRADLE_BIN" :FlymFork:assembleFullRelease \
    -Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2 \
    -PKEYSTORE_PATH="$KEYSTORE_PATH" \
    -PKEY_ALIAS="$KEY_ALIAS" \
    -PKEYSTORE_PASSWORD="$KEYSTORE_PASSWORD" \
    -PKEY_PASSWORD="$KEY_PASSWORD" \
    --console=plain

echo
APK="$(ls "$PWD"/FlymFork/build/outputs/apk/full/release/*.apk 2>/dev/null | head -1)"
if [ -z "$APK" ]; then
    echo "Ошибка: APK не найден в $PWD/FlymFork/build/outputs/apk/full/release/" >&2
    exit 1
fi

echo
echo "Копирование..."

RELEASE_DIR="$HOME/storage/downloads/release"
if [ ! -d "$RELEASE_DIR" ]; then
    echo "Ошибка: директория \"$RELEASE_DIR\" не существует." >&2
    exit 1
fi
cp "$APK" "$RELEASE_DIR/"
echo "$RELEASE_DIR/$(basename "$APK")"

RCLONE_REMOTE="yandex"
YANDEX_DIR="apk/full/release"
YANDEX_PATH="$RCLONE_REMOTE:$YANDEX_DIR"

rclone copy "$APK" "$YANDEX_PATH"
echo
echo "На Диске: $YANDEX_PATH/$(basename "$APK")"

# --- создание публичной ссылки ---
echo
echo "Создание публичной ссылки..."
PUBLIC_LINK="$(rclone link "$YANDEX_PATH/$(basename "$APK")" | tail -1)"
if [ -n "$PUBLIC_LINK" ]; then
    echo "Публичная ссылка:"
    echo "$PUBLIC_LINK"
else
    echo "Не удалось получить ссылку." >&2
    exit 1
fi
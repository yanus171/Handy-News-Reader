#!/bin/bash

# ===== НАСТРОЙКИ =====
REMOTE="yandex"
REMOTE_PATH="Projects/Flym"
LOCAL_PATH="$HOME/Yandex.Disk/Projects/Flym"
LOG_FILE="$HOME/.rclone_sync.log"
LOCK_FILE="/tmp/rclone_sync.lock"
FILTER_FILE="./.rclone_filters.txt"
BARE="$HOME/Yandex.Disk/Projects/Flym.bare.git"
REMOTE_BARE="$REMOTE:Projects/Flym.bare.git"

# ===== ФУНКЦИИ =====
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

# ===== ПРОВЕРКА БЛОКИРОВКИ =====
if [ -f "$LOCK_FILE" ]; then
    log "⚠️ Синхронизация уже выполняется (PID $(cat $LOCK_FILE))"
    exit 1
fi
echo $$ > "$LOCK_FILE"

# ===== СИНХРОНИЗАЦИЯ .git через bare (СНАЧАЛА) =====
log "🔄 Синхронизация .git (bare)"

# Скачать свежий bare с облака (если есть)
timeout 120 rclone copy "$REMOTE_BARE/" "$BARE/" --exclude ".DS_Store" >/dev/null 2>&1 || true

if ! git pull --rebase --autostash "$BARE" master; then
    log "❌ git pull --rebase не удался. Разрешите конфликты и запустите sync снова"
    rm -f "$LOCK_FILE"
    exit 1
fi

if ! git push "$BARE" master; then
    log "❌ git push в bare не удался. Проверьте состояние bare"
    rm -f "$LOCK_FILE"
    exit 1
fi

# Залить обновлённый bare обратно на облако
timeout 300 rclone copy "$BARE/" "$REMOTE_BARE/" --exclude ".DS_Store" >/dev/null 2>&1
if [ $? -ne 0 ]; then
    log "⚠️ Не удалось залить bare на облако (код $?)"
fi

# ===== ЗАПУСК BISYNC (ПОСЛЕ bare) =====
log "🔄 Запуск bisync"

rclone bisync "$LOCAL_PATH" "$REMOTE:$REMOTE_PATH" \
    --checkers=32 \
    --verbose \
    --progress \
    --filter-from "$FILTER_FILE" \
    --exclude ".git/" \
    --exclude "**/.git/" \
    --log-file="$LOG_FILE"

if [ $? -eq 0 ]; then
    log "✅ Синхронизация рабочей папки успешно завершена"
else
    log "❌ Ошибка синхронизации рабочей папки (код $?)"
    rm -f "$LOCK_FILE"
    exit 1
fi

rm -f "$LOCK_FILE"
log "🏁 Завершено"
exit 0

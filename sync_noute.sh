#!/bin/bash

# ===== НАСТРОЙКИ =====
REMOTE="yandex"
REMOTE_PATH="Projects/Flym"
LOCAL_PATH="$HOME/Yandex.Disk/Projects/Flym"
LOG_FILE="$HOME/.rclone_sync.log"
LOCK_FILE="/tmp/rclone_sync.lock"
FILTER_FILE="./.rclone_filters.txt"

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

# ===== ЗАПУСК BISYNC =====
log "🔄 Запуск bisync"

rclone bisync "$LOCAL_PATH" "$REMOTE:$REMOTE_PATH" \
    --checkers=32 \
    --verbose \
    --progress \
    --filter-from "$FILTER_FILE" \
    --log-file="$LOG_FILE"

if [ $? -eq 0 ]; then
    log "✅ Синхронизация успешно завершена"
else
    log "❌ Ошибка синхронизации (код $?)"
fi

rm -f "$LOCK_FILE"
log "🏁 Завершено"
exit 0

# Handy Reader — Быстрая сборка в Termux

## Рабочая команда (проверено)

Wrapper-скрипты в Termux не работают — вызывать кешированный Gradle напрямую:

```bash
cd /data/data/com.termux/files/home/Yandex.Disk/Projects/Flym
GRADLE_BIN="$(ls -d ~/.gradle/wrapper/dists/gradle-*-bin/*/gradle-*/bin/gradle | head -1)"
"$GRADLE_BIN" assembleFpdaQa -Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2 --console=plain
```

- `-Pandroid.aapt2FromMavenOverride=...` обязателен: в `gradle.properties` строка закомментирована (мешает сборке в Android Studio на десктопе), передавать только через `-P`.

- Инкрементальная сборка — секунды; первый запуск daemon'а за сессию — 1–2 мин.
- Для холодной сборки задавать timeout ≥ 900 с.
- Сеть доступна (проверено: `curl` к services.gradle.org — HTTP 200), несмотря на метку «network blocked» в постуре сессии. Зависимости и дистрибутив Gradle уже закешированы — обычная сборка идёт офлайн. Скачивать новое (зависимости, Gradle) — только при реальной необходимости: в постуре Ask каждая команда требует одобрения пользователя.

## Что НЕ работает (не тратить попытки)

- `./gradlew` → `/usr/bin/env: bad interpreter` (в Termux нет `/usr/bin/env`)
- `sh gradlew` → `Syntax error: "(" unexpected` (скрипт не POSIX-совместим)
- `./gradlew_new` → проверяет не тот путь дистрибутива (мимо стандартной раскладки `gradle-8.11.1-bin/<hash>/`) и начинает перекачивать Gradle, хотя он уже закеширован
- Нюанс: если сессия работает в постуре Ask, любой командой (включая сетевые) нужно одобрение — но это не блокировка сети

## Окружение (уже настроено, ничего менять не надо)

- Java 17: `/data/data/com.termux/files/usr/lib/jvm/java-17-openjdk` (уже в PATH)
- Android SDK: `~/android-sdk`, путь задан в `local.properties`
- aapt2: `android.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2` в `gradle.properties`; warning «experimental» при конфигурации — норма
- Gradle 8.11.1 закеширован: `~/.gradle/wrapper/dists/gradle-8.11.1-bin/`

## Флейв и артефакт

- Собирать только Fpda QA: `assembleFpdaQa`
- Результат: `app/build/outputs/apk/fpda/qa/ru.yanus171.feedexfork_fpdaQa_1.16.2_428.apk` (имя зависит от versionName/versionCode)
- Если задачи все UP-TO-DATE, а правки в исходниках были — проверять, что APK свежий по времени файла (`ls -la`)

## Проверка результата (по KODA.md — после компиляции)

**Сборка+установка+запуск — действие по умолчанию:** после ЛЮБОЙ правки кода/ресурсов сразу (не дожидаясь просьбы, не спрашивая) выполнять: `assembleFpdaQa` → install → `am start`. Только если пользователь явно сказал не собирать — пропустить.

Регресс-тесты (UI-проверки, клики по экрану) после установки НЕ выполнять — их делает пользователь вручную. Автоматически — сборка, установка и запуск (`am start`). На этом остановиться: pidof/dumpsys, logcat и любые проверки не выполнять — пользователь проверяет сам.

adb брать из Termux (пакет android-tools), НЕ из `~/android-sdk` — тот сломан («Exec format error»), подробности в разделе «Установка APK на устройство» ниже:

```bash
ADB=/data/data/com.termux/files/usr/bin/adb   # $PREFIX в агентских сессиях бывает пуст
$ADB devices                                  # emulator-5554 (может висеть и 127.0.0.1:5555 — всегда указывать -s)
$ADB -s emulator-5554 install -r app/build/outputs/apk/fpda/qa/*_fpdaQa_*.apk
$ADB -s emulator-5554 shell am start -n ru.yanus171.feedexfork/ru.yanus171.feedexfork.MainActivity
```

- Установка на устройство — только если пользователь её одобрил (может отменить запрос).

## Установка APK на устройство (проверено 2026-09-01)

### Единый путь: adb (другие способы не использовать)
- Цель: **emulator-5554** — эмулятор на этом же телефоне, adb-сервер Termux его видит.
- adb: **Termux-пакет android-tools** (`/data/data/com.termux/files/usr/bin/adb`, 35.0.2, ARM) — уже установлен. adb из `~/android-sdk/platform-tools` не работает («Exec format error», x86-бинарник) — не тратить попытки.
- Установка и запуск — только через adb. Альтернативы не использовать: `pm install` из Termux (SELinux-блокировки), `cp <apk> /sdcard/Download/` + тап в Files, `termux-open`.
- Нюанс агентских сессий: `$PREFIX` бывает пуст — звать adb по абсолютному пути.

```bash
ADB=/data/data/com.termux/files/usr/bin/adb
$ADB -s emulator-5554 install -r app/build/outputs/apk/fpda/qa/*_fpdaQa_*.apk
$ADB -s emulator-5554 shell pm list packages | grep feedfork   # проверка установки
$ADB -s emulator-5554 shell am start -n ru.yanus171.feedexfork/ru.yanus171.feedexfork.MainActivity

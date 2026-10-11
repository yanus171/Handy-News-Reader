#!/bin/sh
# Build the Flym project with a choice of flavor (fdroid/full) and build type (debug/release).
# On first run a signing key with the trivial password 123456 is created (printed to
# the screen); on subsequent runs the key is reused.
# Existing build methods (Android Studio, install.sh, release.sh) are not affected:
# the key lives in KeyStore/buildKey/ and is only used when the build is launched by this script.

set -eu
cd "$(dirname "$0")"

GRADLE_BIN=""
# --- gradle: first the Termux cache, then ./gradlew (Linux desktop) ---
for cand in "$HOME"/.gradle/wrapper/dists/gradle-*-bin/*/gradle-*/bin/gradle; do
    [ -x "$cand" ] && GRADLE_BIN="$cand" && break
done 2>/dev/null || true
if [ -z "$GRADLE_BIN" ] && [ -x ./gradlew ]; then
    GRADLE_BIN=./gradlew
fi
if [ -z "$GRADLE_BIN" ]; then
    echo "Error: gradle not found in ~/.gradle/wrapper/dists or ./gradlew" >&2
    exit 1
fi

# --- aapt2: Termux needs an override, Linux picks it up from the Android SDK automatically ---
AAPT2_ARGS=""
TERMUX_AAPT2=/data/data/com.termux/files/usr/bin/aapt2
if [ -x "$TERMUX_AAPT2" ]; then
    AAPT2_ARGS="-Pandroid.aapt2FromMavenOverride=$TERMUX_AAPT2"
fi

# --- flavor choice: argument or interactive prompt ---
FLAVOR="${1:-}"
case "$FLAVOR" in
    fdroid) ;;
    full) ;;
    *)
        printf 'Choose flavor: 1) full  2) fdroid\n'
        printf 'Flavor [full]: '
        read -r FLAVOR_IN
        case "$FLAVOR_IN" in
            2|fdroid) FLAVOR=fdroid ;;
            *)        FLAVOR=full ;;
        esac
        ;;
esac

# --- build type choice: argument or interactive prompt ---
BUILD_TYPE="${2:-}"
case "$BUILD_TYPE" in
    debug) ;;
    release) ;;
    *)
        printf 'Build type: 1) debug  2) release\n'
        printf 'Build type [debug]: '
        read -r BT_IN
        case "$BT_IN" in
            2|release) BUILD_TYPE=release ;;
            *)         BUILD_TYPE=debug ;;
        esac
        ;;
esac

# --- signing key: created once, then reused ---
KEY_DIR="KeyStore/buildKey"
KEYSTORE="$PWD/$KEY_DIR/keyStore"
KEY_ALIAS="flymbuildkey"
KEY_PASSWORD="123456"

if [ -f "$KEYSTORE" ]; then
    if ! keytool -list -keystore "$KEYSTORE" -storepass "$KEY_PASSWORD" -alias "$KEY_ALIAS" >/dev/null 2>&1; then
        echo "Error: $KEYSTORE exists, but alias $KEY_ALIAS was not found." >&2
        echo "Delete the file to create a new key." >&2
        exit 1
    fi
    echo "Key: reusing $KEYSTORE (password $KEY_PASSWORD, alias $KEY_ALIAS)"
else
    mkdir -p "$KEY_DIR"
    keytool -genkeypair -v \
        -keystore "$KEYSTORE" \
        -alias "$KEY_ALIAS" \
        -keyalg RSA -keysize 2048 -validity 10000 \
        -storepass "$KEY_PASSWORD" -keypass "$KEY_PASSWORD" \
        -dname "CN=Flym Build, O=Yanus171, C=RU" >/dev/null
    echo "New signing key created:"
    echo "  file:    $KEYSTORE"
    echo "  password: $KEY_PASSWORD"
    echo "  alias:   $KEY_ALIAS"
    echo "It will be reused on subsequent builds."
fi

# --- task name: :FlymFork:assemble<Flavor><BuildType> ---
case "$FLAVOR" in
    fdroid) TASK_FLAVOR=Fdroid ;;
    full)   TASK_FLAVOR=Full ;;
esac
case "$BUILD_TYPE" in
    debug)   TASK_TYPE=Debug ;;
    release) TASK_TYPE=Release ;;
esac
TASK=":FlymFork:assemble${TASK_FLAVOR}${TASK_TYPE}"

# --- build ---
echo
echo "Building: $TASK (signature: $KEY_ALIAS)"
# shellcheck disable=SC2086
"$GRADLE_BIN" "$TASK" \
    -PKEYSTORE_PATH="$KEYSTORE" \
    -PKEY_ALIAS="$KEY_ALIAS" \
    -PKEYSTORE_PASSWORD="$KEY_PASSWORD" \
    -PKEY_PASSWORD="$KEY_PASSWORD" \
    $AAPT2_ARGS \
    --console=plain

# --- result ---
APK=""
for cand in "$PWD"/FlymFork/build/outputs/apk/"$FLAVOR"/"$BUILD_TYPE"/*.apk; do
    [ -f "$cand" ] && APK="$cand" && break
done 2>/dev/null || true
if [ -z "$APK" ]; then
    echo "Error: APK not found in FlymFork/build/outputs/apk/$FLAVOR/$BUILD_TYPE/" >&2
    exit 1
fi
echo
echo "Done: $APK"
echo "Verify signature: keytool -printcert -jarfile \"$APK\""